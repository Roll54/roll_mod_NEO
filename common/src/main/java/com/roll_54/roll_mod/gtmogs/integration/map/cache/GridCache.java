package com.roll_54.roll_mod.gtmogs.integration.map.cache;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Holds the generated veins for a single grid cell.
 *
 * <p>
 * Veins are added from the worldgen worker threads (via {@code OreGenerator}) while the server thread reads them
 * for prospecting, saving and datapack reloads. Every method therefore synchronizes on the instance, and anything
 * handed out is a snapshot rather than a live view.
 */
public class GridCache {

    private final List<GeneratedVeinMetadata> veins = new ArrayList<>();

    /**
     * Membership mirror of {@link #veins}, so adding a vein does not have to scan the list -- that scan made
     * filling in a region quadratic in the number of veins discovered there.
     *
     * <p>
     * Kept in lockstep with the list. Note that {@link GeneratedVeinMetadata#hashCode()} folds in the mutable
     * definition holder, so this has to be rebuilt whenever definitions are rewritten; see
     * {@link #refreshDefinitions}.
     */
    private final Set<GeneratedVeinMetadata> present = new HashSet<>();

    public synchronized boolean addVein(GeneratedVeinMetadata vein) {
        if (!present.add(vein)) return false;
        veins.add(vein);
        return true;
    }

    /**
     * @return An immutable snapshot of this cell's veins, safe to iterate while worldgen threads keep adding.
     */
    public synchronized List<GeneratedVeinMetadata> getVeins() {
        return List.copyOf(veins);
    }

    public synchronized ListTag toNBT(HolderLookup.Provider registries) {
        ListTag result = new ListTag();
        for (GeneratedVeinMetadata pos : veins) {
            result.add(GeneratedVeinMetadata.CODEC
                    .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), pos)
                    .getOrThrow());
        }
        return result;
    }

    public synchronized void fromNBT(ListTag tag, HolderLookup.Provider provider) {
        for (Tag veinTag : tag) {
            // Skipped, not thrown: a vein whose definition was removed or merged into another file (as the
            // mining world copies were) must not take the whole cache down with it.
            GeneratedVeinMetadata.CODEC
                    .parse(provider.createSerializationContext(NbtOps.INSTANCE), veinTag)
                    .resultOrPartial(error -> GTMOGS.LOGGER.warn("Dropping cached ore vein: {}", error))
                    .ifPresent(this::addVein);
        }
    }

    /**
     * Re-resolves every vein's definition against the supplied registry, dropping veins whose definition no longer
     * exists.
     *
     * <p>
     * This lives here rather than in the caller because rewriting a definition changes the vein's hash code, so
     * {@link #present} must be rebuilt within the same critical section.
     */
    public synchronized void refreshDefinitions(Registry<OreVeinDefinition> registry) {
        veins.removeIf(vein -> {
            Optional<Holder.Reference<OreVeinDefinition>> def = registry.getHolder(vein.definition().getKey());
            def.ifPresent(vein::definition);
            return def.isEmpty();
        });

        present.clear();
        present.addAll(veins);
    }

    public synchronized List<GeneratedVeinMetadata> getVeinsMatching(Predicate<GeneratedVeinMetadata> predicate) {
        return veins.stream().filter(predicate).collect(Collectors.toList());
    }

    public synchronized void removeVeinsMatching(Predicate<GeneratedVeinMetadata> predicate) {
        if (veins.removeIf(predicate)) {
            present.clear();
            present.addAll(veins);
        }
    }
}
