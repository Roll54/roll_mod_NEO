package com.roll_54.roll_mod.minestar.hub.warp;

import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Every warp on the server, persisted on the overworld's {@code DimensionDataStorage} as
 * {@code world/data/roll_mod_warps.dat}. Mirrors {@code AuctionData}.
 */
public class WarpData extends SavedData {

    private static final String NAME = "roll_mod_warps";

    private final List<Warp> warps = new ArrayList<>();

    public static WarpData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(WarpData::new, WarpData::load, null), NAME);
    }

    /** Every warp, newest last. Callers must not mutate the list. */
    public List<Warp> all() {
        return warps;
    }

    /**
     * The order the hub shows warps in: official ones first — an admin warp points at an event or a
     * shop, so it belongs above someone's private base — and then the busiest.
     *
     * <p>Ranking by today's visitors rather than by age is the point: insertion order meant a dead
     * warp made on day one outranked the shop everyone actually uses. Total visits break a tie so a
     * long-popular warp still beats one that had a single busy morning, and creation time breaks the
     * remaining tie so two unvisited warps do not swap places between syncs.
     *
     * <p>This is the only place warps are ordered. The client renders the list as it arrives.
     */
    public List<Warp> sorted() {
        List<Warp> ordered = new ArrayList<>(warps);
        ordered.sort(java.util.Comparator.comparing((Warp w) -> !w.isAdmin())
                .thenComparing(java.util.Comparator.comparingInt(Warp::visitorsToday).reversed())
                .thenComparing(java.util.Comparator.comparingLong(Warp::visitsTotal).reversed())
                .thenComparingLong(Warp::created));
        return ordered;
    }

    /**
     * Warps matching a name, case-insensitively. Names are not unique, so callers that act on one
     * have to handle the ambiguous case rather than taking the first.
     */
    public List<Warp> byName(String name) {
        List<Warp> matches = new ArrayList<>();
        for (Warp warp : warps) {
            // Compared without the colour codes, so a name is typed the way it reads.
            if (LegacyText.plain(warp.name()).equalsIgnoreCase(LegacyText.plain(name))) {
                matches.add(warp);
            }
        }
        return matches;
    }

    /** Swaps a warp for an updated copy of itself, keyed by id. */
    public boolean replace(Warp warp) {
        for (int i = 0; i < warps.size(); i++) {
            if (warps.get(i).id().equals(warp.id())) {
                warps.set(i, warp);
                setDirty();
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Warp byId(UUID id) {
        if (id == null) return null;
        for (Warp warp : warps) {
            if (warp.id().equals(id)) return warp;
        }
        return null;
    }

    public int countFor(UUID owner) {
        int n = 0;
        for (Warp warp : warps) {
            if (warp.owner().equals(owner)) n++;
        }
        return n;
    }

    public void add(Warp warp) {
        warps.add(warp);
        setDirty();
    }

    public boolean remove(UUID id) {
        boolean removed = warps.removeIf(warp -> warp.id().equals(id));
        if (removed) setDirty();
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Warp warp : warps) {
            list.add(warp.save());
        }
        tag.put("warps", list);
        return tag;
    }

    private static WarpData load(CompoundTag tag, HolderLookup.Provider registries) {
        WarpData data = new WarpData();
        ListTag list = tag.getList("warps", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.warps.add(Warp.load(list.getCompound(i), registries));
        }
        return data;
    }
}
