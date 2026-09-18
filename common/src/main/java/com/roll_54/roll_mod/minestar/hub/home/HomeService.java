package com.roll_54.roll_mod.minestar.hub.home;

import com.roll_54.roll_mod.compat.ftb.HomesFacade;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.util.LegacyText;
import com.roll_54.roll_mod.util.PlayerLookup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every rule about homes, so the tab and the commands can ask the same questions and get the same
 * answers. Mirrors {@code WarpService}, without the moderation, the pricing or the warm-up: a home
 * teleport is instant and free, so unlike warps there is no pending map, no tick loop and nothing to
 * cancel on damage.
 *
 * <p>Home names are unique per owner. Warp names are not, which is why resolving one by name needs a
 * whole "which of these did you mean" path; enforcing uniqueness here at creation means {@code /home
 * <name>} never has to ask. Do not relax this without restoring that path.
 */
public final class HomeService {

    /** Without LuckPerms, or without the meta key. */
    public static final int DEFAULT_LIMIT = 3;

    /** With the prime permission but no explicit meta. */
    public static final int PRIME_LIMIT = 8;

    /**
     * When each player last caused a name lookup that could leave the machine. The invite field is
     * free text, so without this a player holding down enter turns the server into a Mojang-API
     * proxy and gets its address rate-limited.
     */
    private static final Map<UUID, Long> LAST_LOOKUP = new ConcurrentHashMap<>();

    private static final long LOOKUP_COOLDOWN_MS = 2_000L;

    private HomeService() {}

    /** How many homes this player may own. Meta first, then the prime/default pair. */
    public static int limitFor(ServerPlayer player) {
        Integer meta = LuckPermsCompat.homeLimit(player);
        if (meta != null) return Math.max(0, meta);
        return LuckPermsCompat.isPrime(player) ? PRIME_LIMIT : DEFAULT_LIMIT;
    }

    /* --------------------------------------------- owning --------------------------------------------- */

    /**
     * Creates a home at the player's feet, or — when they already have one by that name — moves that
     * one here. Re-using the name to move is what makes a separate "move" verb unnecessary.
     */
    public static void create(ServerPlayer player, String rawName) {
        String name = rawName == null ? "" : rawName.strip();
        if (name.isEmpty()) {
            refuse(player, "msg.roll_mod.homes.needName");
            return;
        }

        HomeData data = HomeData.get(player.server);
        PlayerHome existing = data.byOwnerAndName(player.getUUID(), name);
        if (existing != null) {
            data.replace(existing.movedTo(player));
            player.sendSystemMessage(
                    Component.translatable("msg.roll_mod.homes.moved",
                            LegacyText.display(existing.name())));
            HomeViewers.resync(player.server);
            return;
        }

        int limit = limitFor(player);
        if (data.countFor(player.getUUID()) >= limit) {
            refuse(player, "msg.roll_mod.homes.limit", limit);
            return;
        }

        PlayerHome home = PlayerHome.at(player, name);
        data.add(home);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.homes.created",
                LegacyText.display(home.name())));
        HomeViewers.resync(player.server);
    }

    /**
     * Copies one FTB Essentials home into this mod's list and drops FTB's copy.
     *
     * <p>Ordered that way deliberately: the copy is added first and FTB's is only forgotten once it
     * is safely stored, so a failure anywhere leaves the player with the home twice rather than not
     * at all. The name is FTB's, which may already be taken here — a home moved rather than added
     * would silently discard the FTB position, so a clash is refused and the player renames one
     * side first.
     */
    public static void migrate(ServerPlayer player, String rawName) {
        String name = rawName == null ? "" : rawName.strip();
        if (name.isEmpty()) return;

        // Matched on the clamped form as well as the raw one: FTB puts no length cap on a home
        // name, but HomeActionPacket clamps its text field to MAX_NAME, so a long FTB name arrives
        // here already truncated and would never match itself. FTB's own name is what gets
        // forgotten below, so the full string still has to be the one we keep hold of.
        HomesFacade.Legacy legacy = null;
        for (HomesFacade.Legacy candidate : HomesFacade.get().homesOf(player)) {
            if (candidate.name().equals(name) || clampName(candidate.name()).equals(name)) {
                legacy = candidate;
                break;
            }
        }
        // Gone already — most likely migrated from a second window before this click landed.
        if (legacy == null) return;

        HomeData data = HomeData.get(player.server);
        String target = clampName(legacy.name());
        if (data.byOwnerAndName(player.getUUID(), target) != null) {
            refuse(player, "msg.roll_mod.homes.migrateClash", target);
            return;
        }

        int limit = limitFor(player);
        if (data.countFor(player.getUUID()) >= limit) {
            refuse(player, "msg.roll_mod.homes.limit", limit);
            return;
        }

        PlayerHome home = PlayerHome.imported(player, legacy.name(), legacy.dimension(),
                legacy.x(), legacy.y(), legacy.z(), legacy.yaw(), legacy.pitch());
        data.add(home);
        HomesFacade.get().forget(player, legacy.name());
        player.sendSystemMessage(Component.translatable("msg.roll_mod.homes.migrated",
                LegacyText.display(home.name())));
        HomeViewers.resync(player.server);
    }

    /** The name a home will actually be stored under: what {@code PlayerHome.trim} would leave. */
    private static String clampName(String raw) {
        String clean = raw == null ? "" : raw.strip();
        return clean.length() <= PlayerHome.MAX_NAME ? clean
                : clean.substring(0, PlayerHome.MAX_NAME);
    }

    public static void delete(ServerPlayer player, UUID homeId) {
        HomeData data = HomeData.get(player.server);
        PlayerHome home = data.byId(homeId);
        if (home == null) return;
        // No operator bypass, deliberately, unlike warps: a home is private, and an op deleting
        // someone's base by mistake is a worse outcome than an op having to ask.
        if (!home.isOwner(player.getUUID())) {
            refuse(player, "msg.roll_mod.homes.notOwner");
            return;
        }

        // Tell the guests before it goes: access disappearing with no explanation reads as a bug.
        for (HomeShare share : home.shares()) {
            if (!share.isAccepted()) continue;
            ServerPlayer guest = player.server.getPlayerList().getPlayer(share.player());
            if (guest != null) {
                guest.sendSystemMessage(Component.translatable("msg.roll_mod.homes.revoked",
                        home.ownerName(), LegacyText.display(home.name()))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        data.remove(homeId);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.homes.deleted",
                LegacyText.display(home.name())));
        HomeViewers.resync(player.server);
    }

    /* -------------------------------------------- travelling ------------------------------------------- */

    /** Instant and free — no warm-up, so nothing here can be interrupted or refunded. */
    public static void teleport(ServerPlayer player, UUID homeId) {
        PlayerHome home = HomeData.get(player.server).byId(homeId);
        if (home == null) {
            // Gone since the client last synced — most likely the owner deleted it. There is no
            // name left to put in "no home called X", and the effect is the same either way.
            refuse(player, "msg.roll_mod.homes.noAccess");
            return;
        }
        if (!home.canTeleport(player.getUUID())) {
            refuse(player, "msg.roll_mod.homes.noAccess");
            return;
        }

        ServerLevel level = level(player.server, home);
        if (level == null) {
            refuse(player, "msg.roll_mod.homes.noDimension");
            return;
        }

        // Out of the hub first, so the player lands looking at the world rather than at the screen
        // they left from. Only once every refusal above has passed.
        player.closeContainer();
        player.teleportTo(level, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        player.sendSystemMessage(Component.translatable("msg.roll_mod.homes.arrived",
                LegacyText.display(home.name())));
    }

    @Nullable
    private static ServerLevel level(MinecraftServer server, PlayerHome home) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, home.dimension());
        return server.getLevel(key);
    }

    /* --------------------------------------------- sharing -------------------------------------------- */

    /**
     * Invites a player by name. The name is resolved asynchronously when it is neither online nor
     * cached, so everything after the lookup runs in the callback — which {@link PlayerLookup}
     * guarantees lands back on the server thread.
     */
    public static void invite(ServerPlayer owner, UUID homeId, String rawName) {
        String name = rawName == null ? "" : rawName.strip();
        HomeData data = HomeData.get(owner.server);
        PlayerHome home = data.byId(homeId);
        if (home == null) return;
        if (!home.isOwner(owner.getUUID())) {
            refuse(owner, "msg.roll_mod.homes.notOwner");
            return;
        }
        if (!PlayerLookup.isPlausibleName(name)) {
            refuse(owner, "msg.roll_mod.homes.invitePlayerNotFound", name);
            return;
        }
        // Before the lookup, not after: the whole point of the cap is that a spammer cannot make the
        // server resolve name after name on their behalf, which is the same thing LOOKUP_COOLDOWN_MS
        // guards. Checked again in the callback, where the roster is re-read.
        if (home.pendingCount() >= PlayerHome.MAX_PENDING_INVITES) {
            refuse(owner, "msg.roll_mod.homes.inviteLimit", PlayerHome.MAX_PENDING_INVITES);
            return;
        }

        // Only throttle lookups that would actually hit the network; an online player or a cached
        // one costs nothing and should stay instant.
        boolean local = PlayerLookup.online(owner.server, name) != null
                || PlayerLookup.cached(owner.server, name) != null;
        if (!local && !allowLookup(owner.getUUID())) {
            refuse(owner, "msg.roll_mod.homes.inviteTooFast");
            return;
        }

        PlayerLookup.resolve(owner.server, name, profile -> {
            if (profile == null) {
                refuse(owner, "msg.roll_mod.homes.invitePlayerNotFound", name);
                return;
            }
            // Re-read: the callback may be a tick or more later, and the home could be gone.
            PlayerHome current = HomeData.get(owner.server).byId(homeId);
            if (current == null || !current.isOwner(owner.getUUID())) return;

            if (profile.id().equals(owner.getUUID())) {
                refuse(owner, "msg.roll_mod.homes.inviteSelf");
                return;
            }
            if (current.shareFor(profile.id()) != null) {
                refuse(owner, "msg.roll_mod.homes.inviteDuplicate", profile.name());
                return;
            }
            // The check above ran against the roster as it was a tick or more ago; this one is the
            // one that actually enforces the cap, since a second invite could have landed since.
            if (current.pendingCount() >= PlayerHome.MAX_PENDING_INVITES) {
                refuse(owner, "msg.roll_mod.homes.inviteLimit", PlayerHome.MAX_PENDING_INVITES);
                return;
            }

            List<HomeShare> shares = new ArrayList<>(current.shares());
            shares.add(HomeShare.pending(profile.id(), profile.name()));
            HomeData.get(owner.server).replace(current.withShares(shares));

            owner.sendSystemMessage(Component.translatable("msg.roll_mod.homes.inviteSent",
                    profile.name(), LegacyText.display(current.name())));
            ServerPlayer guest = owner.server.getPlayerList().getPlayer(profile.id());
            if (guest != null) {
                guest.sendSystemMessage(Component.translatable("msg.roll_mod.homes.inviteReceived",
                        current.ownerName(), LegacyText.display(current.name()))
                        .withStyle(ChatFormatting.GOLD));
            }
            HomeViewers.resync(owner.server);
        });
    }

    public static void accept(ServerPlayer guest, UUID homeId) {
        answer(guest, homeId, true);
    }

    public static void decline(ServerPlayer guest, UUID homeId) {
        answer(guest, homeId, false);
    }

    private static void answer(ServerPlayer guest, UUID homeId, boolean accepted) {
        HomeData data = HomeData.get(guest.server);
        PlayerHome home = data.byId(homeId);
        if (home == null) return;
        HomeShare share = home.shareFor(guest.getUUID());
        // Not pending: either already answered or never invited. A stale client is not an error.
        if (share == null || !share.isPending()) return;

        List<HomeShare> shares = new ArrayList<>(home.shares());
        shares.removeIf(s -> s.player().equals(guest.getUUID()));
        if (accepted) shares.add(share.accepted());
        data.replace(home.withShares(shares));

        guest.sendSystemMessage(Component.translatable(
                accepted ? "msg.roll_mod.homes.accepted" : "msg.roll_mod.homes.declined",
                LegacyText.display(home.name())));
        ServerPlayer owner = guest.server.getPlayerList().getPlayer(home.owner());
        if (owner != null) {
            owner.sendSystemMessage(Component.translatable(
                    accepted ? "msg.roll_mod.homes.acceptedByGuest"
                            : "msg.roll_mod.homes.declinedByGuest",
                    guest.getGameProfile().getName(), LegacyText.display(home.name())));
        }
        HomeViewers.resync(guest.server);
    }

    /** A guest giving up their own access. The owner cannot use this to evict; they delete instead. */
    public static void leave(ServerPlayer guest, UUID homeId) {
        HomeData data = HomeData.get(guest.server);
        PlayerHome home = data.byId(homeId);
        if (home == null) return;
        if (home.isOwner(guest.getUUID())) {
            refuse(guest, "msg.roll_mod.homes.leaveOwner");
            return;
        }
        HomeShare share = home.shareFor(guest.getUUID());
        if (share == null || !share.isAccepted()) return;

        List<HomeShare> shares = new ArrayList<>(home.shares());
        shares.removeIf(s -> s.player().equals(guest.getUUID()));
        data.replace(home.withShares(shares));

        guest.sendSystemMessage(Component.translatable("msg.roll_mod.homes.left",
                LegacyText.display(home.name())));
        ServerPlayer owner = guest.server.getPlayerList().getPlayer(home.owner());
        if (owner != null) {
            owner.sendSystemMessage(Component.translatable("msg.roll_mod.homes.leftByGuest",
                    guest.getGameProfile().getName(), LegacyText.display(home.name())));
        }
        HomeViewers.resync(guest.server);
    }

    /* --------------------------------------------- helpers -------------------------------------------- */

    private static boolean allowLookup(UUID player) {
        long now = System.currentTimeMillis();
        Long last = LAST_LOOKUP.get(player);
        if (last != null && now - last < LOOKUP_COOLDOWN_MS) return false;
        LAST_LOOKUP.put(player, now);
        return true;
    }

    private static void refuse(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(
                Component.translatable(key, args).withStyle(ChatFormatting.RED));
    }
}
