package com.roll_54.roll_mod.minestar.hub.home;

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
 * Every home on the server, persisted on the overworld's {@code DimensionDataStorage} as {@code
 * world/data/roll_mod_homes.dat}. Mirrors {@code WarpData}.
 *
 * <p>The file name is effectively permanent — renaming it once this has shipped would orphan every
 * home on the server, the way {@code AuctionData} documents for its own.
 */
public class HomeData extends SavedData {

    private static final String NAME = "roll_mod_homes";

    private final List<PlayerHome> homes = new ArrayList<>();

    public static HomeData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new Factory<>(HomeData::new, HomeData::load, null), NAME);
    }

    /** Callers must not mutate the list. */
    public List<PlayerHome> all() {
        return homes;
    }

    public List<PlayerHome> ownedBy(UUID owner) {
        List<PlayerHome> mine = new ArrayList<>();
        for (PlayerHome home : homes) {
            if (home.isOwner(owner)) mine.add(home);
        }
        return mine;
    }

    /**
     * How many homes count against this player's allowance. Only the ones they own: this single
     * choice is the whole implementation of "a home shared with you costs you nothing".
     */
    public int countFor(UUID owner) {
        int n = 0;
        for (PlayerHome home : homes) {
            if (home.isOwner(owner)) n++;
        }
        return n;
    }

    /** Everything this player should be able to see: their own, plus any share, accepted or not. */
    public List<PlayerHome> visibleTo(UUID player) {
        List<PlayerHome> visible = new ArrayList<>();
        for (PlayerHome home : homes) {
            if (home.isOwner(player) || home.shareFor(player) != null) visible.add(home);
        }
        return visible;
    }

    @Nullable
    public PlayerHome byId(UUID id) {
        for (PlayerHome home : homes) {
            if (home.id().equals(id)) return home;
        }
        return null;
    }

    /** Names are unique per owner, so this can return one home rather than a list. */
    @Nullable
    public PlayerHome byOwnerAndName(UUID owner, String name) {
        for (PlayerHome home : homes) {
            // Compared without the colour codes: a home called "&abase" answers to "base",
            // and the uniqueness check that runs through here treats the two as the same name.
            if (home.isOwner(owner) && sameName(home.name(), name)) return home;
        }
        return null;
    }

    /** Homes this player has accepted an invite to, matched by name. */
    public List<PlayerHome> sharedWithByName(UUID player, String name) {
        List<PlayerHome> matches = new ArrayList<>();
        for (PlayerHome home : homes) {
            if (home.isOwner(player)) continue;
            HomeShare share = home.shareFor(player);
            if (share != null && share.isAccepted() && sameName(home.name(), name)) {
                matches.add(home);
            }
        }
        return matches;
    }

    /** Names match on what they read as, not on the codes used to colour them. */
    private static boolean sameName(String a, String b) {
        return LegacyText.plain(a).equalsIgnoreCase(LegacyText.plain(b));
    }

    public void add(PlayerHome home) {
        homes.add(home);
        setDirty();
    }

    /**
     * Swaps a home for an updated copy, dropping any lapsed invites on the way through.
     *
     * <p>This is the only write path, which makes it the one place expiry has to be swept from disk:
     * reads already ignore a lapsed invite ({@code PlayerHome#shareFor}), so the stored row is merely
     * stale rather than harmful, and cleaning it up whenever the home is touched anyway costs nothing
     * and needs no scheduler.
     */
    public boolean replace(PlayerHome home) {
        PlayerHome pruned = home.withoutExpiredShares();
        for (int i = 0; i < homes.size(); i++) {
            if (homes.get(i).id().equals(pruned.id())) {
                homes.set(i, pruned);
                setDirty();
                return true;
            }
        }
        return false;
    }

    public boolean remove(UUID id) {
        boolean removed = homes.removeIf(home -> home.id().equals(id));
        if (removed) setDirty();
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (PlayerHome home : homes) {
            list.add(home.save());
        }
        tag.put("homes", list);
        return tag;
    }

    private static HomeData load(CompoundTag tag, HolderLookup.Provider registries) {
        HomeData data = new HomeData();
        ListTag list = tag.getList("homes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            data.homes.add(PlayerHome.load(list.getCompound(i), registries));
        }
        return data;
    }
}
