package com.roll_54.roll_mod_client.compat.xaero;

import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.prospecting.ClientProspects;
import com.roll_54.roll_mod.prospecting.Discovery;
import com.roll_54.roll_mod.prospecting.OreKind;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Which ore veins a prospected chunk holds, by its ores. A chunk may hold several (one vein's edge reaching into
 * another's), so its ores are split: of the veins that generate in the player's world, the one sharing the most of
 * the chunk's ores (the smaller vein on a tie, as the closer fit) takes those ores, then the next one takes from what
 * is left, and so on. An ore no vein of this world holds stands as a vein of its own. Veins are named as the vein
 * recipes in TEI/JEI name them ({@code ore_vein.<namespace>.<path>}).
 *
 * <p>A vein's key is its id, or {@code ore:<ore key>} for an ore no vein holds; the vein filter stores these.
 */
@OnlyIn(Dist.CLIENT)
final class ProspectVeins {

    /** The prefix of the key of an ore that no vein of this world holds. */
    static final String ORE_PREFIX = "ore:";

    private ProspectVeins() {}

    /** One vein's share of a chunk. */
    record Piece(String key, List<Discovery> ores) {}

    /** The veins of this world with their ores and rock; read again when the dimension or the level changes. */
    private record Known(ResourceLocation id, Set<String> ores, Category category) {}

    /**
     * The rock a vein grows in, as the vein filter groups them: its gtmogs layer here. Stone takes stone, andesite,
     * diorite and granite; deepslate takes deepslate and tuff (vanilla's ore-replaceable tags); each planet its own.
     */
    enum Category {
        STONE, DEEPSLATE, NETHER, END, MOON, MARS, MERCURY, VENUS, GLACIO, OTHER;

        Component title() {
            return Component.translatable("gui.roll_mod.prospect_filter.category." + name().toLowerCase(java.util.Locale.ROOT));
        }

        static Category ofLayer(String layer) {
            if (layer.endsWith("deepslate")) return DEEPSLATE;
            return switch (layer) {
                case "stone", "mining_overworld_stone" -> STONE;
                case "netherrack" -> NETHER;
                case "endstone" -> END;
                case "moon_stone" -> MOON;
                case "mars_stone" -> MARS;
                case "mercury_stone" -> MERCURY;
                case "venus_stone" -> VENUS;
                case "glacio_stone" -> GLACIO;
                default -> OTHER;
            };
        }
    }

    /** The category of a vein key; {@link Category#OTHER} for an ore no vein holds. */
    static Category category(String key) {
        if (key.startsWith(ORE_PREFIX)) return Category.OTHER;
        for (Known k : known()) if (k.id().toString().equals(key)) return k.category();
        return Category.OTHER;
    }

    private static Object cachedFor;
    private static List<Known> known = List.of();

    /** The chunk's ores, split into veins; the biggest share first. */
    static List<Piece> split(List<Discovery> ores) {
        List<Piece> out = new ArrayList<>();
        List<Discovery> left = new ArrayList<>(ores);
        List<Known> veins = known();
        while (!left.isEmpty()) {
            Known best = null;
            int bestShared = 0, bestSize = Integer.MAX_VALUE;
            for (Known vein : veins) {
                int shared = 0;
                for (Discovery d : left) if (vein.ores().contains(d.oreKey)) shared++;
                if (shared == 0) continue;
                if (shared > bestShared || (shared == bestShared && vein.ores().size() < bestSize)) {
                    best = vein;
                    bestShared = shared;
                    bestSize = vein.ores().size();
                }
            }
            if (best == null) break;
            List<Discovery> taken = new ArrayList<>();
            for (var it = left.iterator(); it.hasNext(); ) {
                Discovery d = it.next();
                if (best.ores().contains(d.oreKey)) {
                    taken.add(d);
                    it.remove();
                }
            }
            out.add(new Piece(best.id().toString(), List.copyOf(taken)));
        }
        for (Discovery d : left) out.add(new Piece(ORE_PREFIX + d.oreKey, List.of(d)));
        return out;
    }

    /** A vein key's name: the vein's, or the ore's for an ore no vein holds. */
    static Component name(String key, List<Discovery> ores) {
        if (key.startsWith(ORE_PREFIX)) {
            return ores.isEmpty() ? Component.literal(key.substring(ORE_PREFIX.length())) : ClientProspects.name(ores.getFirst());
        }
        ResourceLocation id = ResourceLocation.tryParse(key);
        return id == null ? Component.literal(key) : Component.translatable(id.toLanguageKey("ore_vein"));
    }

    /** The vein's name, or null when no vein of this world holds any of the chunk's ores. */
    static Component name(List<Discovery> ores) {
        ResourceLocation id = id(ores);
        return id == null ? null : Component.translatable(id.toLanguageKey("ore_vein"));
    }

    /** The id of the vein the chunk's ores fit best, or null when no vein of this world holds any of them. */
    static ResourceLocation id(List<Discovery> ores) {
        List<Piece> pieces = split(ores);
        if (pieces.isEmpty() || pieces.getFirst().key().startsWith(ORE_PREFIX)) return null;
        return ResourceLocation.tryParse(pieces.getFirst().key());
    }

    private static List<Known> known() {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        if (cachedFor == level) return known;
        var registry = level.registryAccess().registry(GTRegistries.ORE_VEIN_REGISTRY).orElse(null);
        List<Known> out = new ArrayList<>();
        if (registry != null) {
            for (Holder.Reference<OreVeinDefinition> vein : registry.holders().toList()) {
                OreVeinDefinition def = vein.value();
                var dims = def.dimensionFilter();
                if (dims != null && !dims.isEmpty() && !dims.contains(level.dimension())) continue;
                Set<String> veinOres = new HashSet<>();
                def.veinGenerator().getAllBlocks().forEach(state -> veinOres.add(OreKind.of(state.getBlock()).key()));
                Category category = Category.ofLayer(def.layer(level.dimension()).getSerializedName());
                if (!veinOres.isEmpty()) out.add(new Known(vein.key().location(), Set.copyOf(veinOres), category));
            }
        }
        out.sort((a, b) -> a.id().toString().compareTo(b.id().toString()));
        known = List.copyOf(out);
        cachedFor = level;
        return known;
    }

    /** Every vein key found in the player's dimension, for the vein filter. */
    static List<String> foundKeys() {
        Set<String> keys = new LinkedHashSet<>();
        ProspectChunk.all().forEachRemaining(c -> split(c.ores()).forEach(p -> keys.add(p.key())));
        return List.copyOf(keys);
    }
}
