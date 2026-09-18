package com.roll_54.roll_mod.minestar.hub.home;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One player's home: a private destination, optionally shared with named guests.
 *
 * <p>Modelled on {@code Warp}, with the moderation and pricing dropped — a home is nobody's business
 * but its owner's and the people they let in. Named {@code PlayerHome} rather than {@code Home}
 * because the hub already has a {@code HomeTab}, which is its dashboard and has nothing to do with
 * this.
 *
 * <p>Unlike warps, home names are unique per owner (enforced in {@code HomeService}). Warp names are
 * not, which is why resolving one by name has a whole ambiguity path; making these unique means
 * {@code /home <name>} never has to ask which one.
 *
 * @param shares everyone invited to this home, accepted or not — see {@link HomeShare}
 */
public record PlayerHome(UUID id, UUID owner, String ownerName, String name,
                         ResourceLocation dimension, double x, double y, double z,
                         float yaw, float pitch, long created, List<HomeShare> shares) {

    /** The longest name the create form will accept, and the server enforces. */
    public static final int MAX_NAME = 24;

    /**
     * How many invites one home may have out and unanswered at once.
     *
     * <p>A guard against abuse rather than a gameplay limit: the invite field takes a free-typed
     * name, so without a cap one owner can paper every player on the server with notifications, and
     * each unknown name is a lookup the server may have to take off-machine. Only <em>pending</em>
     * invites count — someone who has accepted is a guest, not an outstanding request, and capping
     * those would limit how many friends a home can have, which is not the problem being solved.
     *
     * <p>Three, matching {@code HomeService.DEFAULT_LIMIT}: you would rarely have more than a couple
     * of invites in the air, and the cap clears itself as they are answered.
     */
    public static final int MAX_PENDING_INVITES = 3;

    /** A home at the player's feet, shared with nobody. */
    public static PlayerHome at(ServerPlayer player, String name) {
        return new PlayerHome(UUID.randomUUID(), player.getUUID(),
                player.getGameProfile().getName(), trim(name, MAX_NAME),
                player.level().dimension().location(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                System.currentTimeMillis(), List.of());
    }

    /**
     * A home at a position the player is not standing on, for bringing one across from another mod.
     * Fresh id and empty guest list, exactly like {@link #at}: what is carried over is the place and
     * the name, not any identity the other mod gave it.
     */
    public static PlayerHome imported(ServerPlayer player, String name, ResourceLocation dimension,
                                      double x, double y, double z, float yaw, float pitch) {
        return new PlayerHome(UUID.randomUUID(), player.getUUID(),
                player.getGameProfile().getName(), trim(name, MAX_NAME),
                dimension, x, y, z, yaw, pitch, System.currentTimeMillis(), List.of());
    }

    /** The same home, re-pinned to where the owner is standing now. Keeps its id and its guests. */
    public PlayerHome movedTo(ServerPlayer player) {
        return new PlayerHome(id, owner, ownerName, name,
                player.level().dimension().location(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), created, shares);
    }

    public PlayerHome withShares(List<HomeShare> shares) {
        return new PlayerHome(id, owner, ownerName, name, dimension, x, y, z, yaw, pitch, created,
                List.copyOf(shares));
    }

    public boolean isOwner(UUID player) {
        return owner.equals(player);
    }

    /** The owner, and anyone who has actually accepted. A pending invite is not access. */
    public boolean canTeleport(UUID player) {
        if (isOwner(player)) return true;
        HomeShare share = shareFor(player);
        return share != null && share.isAccepted();
    }

    /**
     * Invites sent and not yet answered, which is what {@link #MAX_PENDING_INVITES} caps. A lapsed
     * invite does not count, or one ignored guest would hold a slot on this home forever.
     */
    public int pendingCount() {
        long now = System.currentTimeMillis();
        int pending = 0;
        for (HomeShare share : shares) {
            if (!share.isAccepted() && !share.isExpired(now)) pending++;
        }
        return pending;
    }

    /**
     * This player's standing, or {@code null} if they have none — <em>including</em> when their
     * invite has lapsed, which is what makes a stale invite unusable everywhere at once.
     *
     * <p>Expiry is judged on read rather than swept on a timer, the same way
     * {@code Warp#visitorsToday()} handles its daily reset: a server that was down over the boundary
     * still answers correctly the first time it is asked, with nothing needing to have run. The row
     * itself is dropped from disk by {@link #withoutExpiredShares()} on the next write to this home.
     */
    @Nullable
    public HomeShare shareFor(UUID player) {
        long now = System.currentTimeMillis();
        for (HomeShare share : shares) {
            if (share.player().equals(player)) {
                return share.isExpired(now) ? null : share;
            }
        }
        return null;
    }

    /** Every share that still counts: accepted memberships, plus invites still inside their window. */
    public List<HomeShare> activeShares() {
        long now = System.currentTimeMillis();
        List<HomeShare> active = new ArrayList<>(shares.size());
        for (HomeShare share : shares) {
            if (!share.isExpired(now)) active.add(share);
        }
        return active;
    }

    /**
     * This home with lapsed invites dropped, or {@code this} when there are none.
     *
     * <p>Returning the same instance when nothing changed is what lets callers prune unconditionally
     * before a write without turning every save into a rewrite.
     */
    public PlayerHome withoutExpiredShares() {
        List<HomeShare> active = activeShares();
        return active.size() == shares.size() ? this : withShares(active);
    }

    private static String trim(String text, int max) {
        String clean = text == null ? "" : text.strip();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putUUID("owner", owner);
        tag.putString("ownerName", ownerName);
        tag.putString("name", name);
        tag.putString("dimension", dimension.toString());
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        tag.putLong("created", created);

        ListTag shared = new ListTag();
        for (HomeShare share : shares) {
            shared.add(share.save());
        }
        tag.put("shares", shared);
        return tag;
    }

    /** Every field added after the first release defaults, so older saved homes still load. */
    public static PlayerHome load(CompoundTag tag, HolderLookup.Provider registries) {
        List<HomeShare> shares = new ArrayList<>();
        ListTag shared = tag.getList("shares", Tag.TAG_COMPOUND);
        for (int i = 0; i < shared.size(); i++) {
            try {
                shares.add(HomeShare.load(shared.getCompound(i)));
            } catch (RuntimeException ignored) {
                // A malformed share is not worth losing the whole home over.
            }
        }

        return new PlayerHome(
                tag.getUUID("id"), tag.getUUID("owner"), tag.getString("ownerName"),
                tag.getString("name"),
                ResourceLocation.parse(tag.getString("dimension")),
                tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                tag.getFloat("yaw"), tag.getFloat("pitch"),
                tag.getLong("created"), List.copyOf(shares));
    }
}
