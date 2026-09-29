package com.roll_54.roll_mod.minestar.tpa;

import com.roll_54.roll_mod.minestar.teleport.TeleportService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Player-to-player teleport requests.
 *
 * <p>In memory only, deliberately: a request that outlived a restart would be a promise nobody
 * remembers making, and it is the one thing in this feature set that has no business in a file an
 * operator reads.
 *
 * <p>Requests expire on their own after {@link #EXPIRY_MILLIS}; the sweep runs in the same server
 * tick that drives the teleport countdowns.
 */
public final class TpaService {

    /** How long a request stands before it lapses. Long enough to notice, short enough to forget. */
    public static final long EXPIRY_MILLIS = 120_000L;

    private static final Map<UUID, TpaRequest> BY_ID = new ConcurrentHashMap<>();

    private TpaService() {}

    /* -------------------------------------------- reading ------------------------------------------- */

    public static List<TpaRequest> incoming(UUID player) {
        List<TpaRequest> list = new ArrayList<>();
        for (TpaRequest request : BY_ID.values()) {
            if (request.to().equals(player)) list.add(request);
        }
        list.sort((a, b) -> Long.compare(a.expiresAt(), b.expiresAt()));
        return list;
    }

    public static List<TpaRequest> outgoing(UUID player) {
        List<TpaRequest> list = new ArrayList<>();
        for (TpaRequest request : BY_ID.values()) {
            if (request.from().equals(player)) list.add(request);
        }
        list.sort((a, b) -> Long.compare(a.expiresAt(), b.expiresAt()));
        return list;
    }

    /* -------------------------------------------- request ------------------------------------------- */

    /**
     * Asks {@code target} for a teleport.
     *
     * <p>A second request to the same player refreshes the first rather than stacking, so a player
     * who clicks twice does not fill someone else's list.
     */
    public static boolean request(ServerPlayer from, ServerPlayer target, TpaRequest.Kind kind) {
        if (from.getUUID().equals(target.getUUID())) {
            refuse(from, "msg.roll_mod.tpa.self");
            return false;
        }

        // The target's own setting. Only the sender is told: a request the target chose never to
        // see should not arrive as a notification that somebody tried.
        if (!TpaSettings.allows(target, from)) {
            from.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.refused",
                    target.getGameProfile().getName()).withStyle(ChatFormatting.RED));
            return false;
        }

        TpaRequest existing = find(from.getUUID(), target.getUUID());
        if (existing != null) BY_ID.remove(existing.id());

        TpaRequest request = new TpaRequest(UUID.randomUUID(), from.getUUID(),
                from.getGameProfile().getName(), target.getUUID(), target.getGameProfile().getName(),
                kind, System.currentTimeMillis() + EXPIRY_MILLIS);
        BY_ID.put(request.id(), request);

        from.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.sent", request.toName()));
        target.sendSystemMessage(invitation(request));
        TpaViewers.resync(from.server);
        return true;
    }

    /** The line the target sees, with the two answers on it — chat is where most of this happens. */
    private static Component invitation(TpaRequest request) {
        String key = request.kind() == TpaRequest.Kind.TO
                ? "msg.roll_mod.tpa.asked"
                : "msg.roll_mod.tpa.askedHere";
        return Component.translatable(key, request.fromName())
                .append(" ")
                .append(Component.translatable("msg.roll_mod.tpa.accept")
                        .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/tpaccept " + request.fromName()))))
                .append(" ")
                .append(Component.translatable("msg.roll_mod.tpa.deny")
                        .withStyle(style -> style.withColor(ChatFormatting.RED)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/tpdeny " + request.fromName()))));
    }

    /* --------------------------------------- accept / deny ------------------------------------------ */

    /** Accepts the named player's request, or the oldest one when {@code fromName} is null. */
    public static boolean accept(ServerPlayer target, @Nullable String fromName) {
        TpaRequest request = pick(incoming(target.getUUID()), fromName);
        if (request == null) {
            refuse(target, "msg.roll_mod.tpa.nothing");
            return false;
        }
        BY_ID.remove(request.id());

        MinecraftServer server = target.server;
        ServerPlayer mover = server.getPlayerList().getPlayer(request.mover());
        ServerPlayer anchor = server.getPlayerList().getPlayer(request.anchor());
        if (mover == null || anchor == null) {
            refuse(target, "msg.roll_mod.tpa.gone");
            TpaViewers.resync(server);
            return false;
        }

        mover.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.accepted",
                target.getGameProfile().getName()));
        target.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.accepting",
                request.fromName()));

        mover.closeContainer();
        UUID anchorId = anchor.getUUID();
        Consumer<ServerPlayer> arrive = arriving -> {
            // Looked up again on arrival: the anchor has had the whole wait to walk off or log out,
            // and sending someone to where a player used to be is worse than not going.
            ServerPlayer destination = arriving.server.getPlayerList().getPlayer(anchorId);
            if (destination == null) {
                refuse(arriving, "msg.roll_mod.tpa.gone");
                return;
            }
            TeleportService.teleport(arriving, destination.serverLevel(), destination.getX(),
                    destination.getY(), destination.getZ(), destination.getYRot(),
                    destination.getXRot());
        };

        // A /tpa moves the player who asked for it, and who has been waiting on the answer ever
        // since: they go at once. A /tpahere moves the player who was standing somewhere doing
        // something else when the request came in, so that one keeps its warm-up.
        if (request.kind() == TpaRequest.Kind.TO) {
            arrive.accept(mover);
        } else {
            TeleportService.schedule(mover, arrive);
        }

        TpaViewers.resync(server);
        return true;
    }

    public static boolean deny(ServerPlayer target, @Nullable String fromName) {
        TpaRequest request = pick(incoming(target.getUUID()), fromName);
        if (request == null) {
            refuse(target, "msg.roll_mod.tpa.nothing");
            return false;
        }
        BY_ID.remove(request.id());

        target.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.denied", request.fromName()));
        ServerPlayer sender = target.server.getPlayerList().getPlayer(request.from());
        if (sender != null) {
            sender.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.wasDenied",
                    request.toName()).withStyle(ChatFormatting.RED));
        }
        TpaViewers.resync(target.server);
        return true;
    }

    /** Takes back one of your own requests. */
    public static boolean cancel(ServerPlayer from, @Nullable String toName) {
        TpaRequest request = pick(outgoing(from.getUUID()), toName);
        if (request == null) {
            refuse(from, "msg.roll_mod.tpa.nothing");
            return false;
        }
        BY_ID.remove(request.id());
        from.sendSystemMessage(Component.translatable("msg.roll_mod.tpa.cancelled", request.toName()));
        TpaViewers.resync(from.server);
        return true;
    }

    /** Drops one request by id — what the hub's buttons send. */
    public static boolean resolve(ServerPlayer player, UUID requestId, boolean accept) {
        TpaRequest request = BY_ID.get(requestId);
        if (request == null) {
            refuse(player, "msg.roll_mod.tpa.nothing");
            return false;
        }
        if (request.to().equals(player.getUUID())) {
            return accept ? accept(player, request.fromName()) : deny(player, request.fromName());
        }
        if (request.from().equals(player.getUUID()) && !accept) {
            return cancel(player, request.toName());
        }
        // Neither side of it: someone else's request, so nothing happens.
        return false;
    }

    /* -------------------------------------------- debug --------------------------------------------- */

    /**
     * Files a request as-is, with no players, checks or messages: for {@code /rollmod debug fill},
     * which needs requests from players who are not online. It lapses on {@link #tick} like any other.
     */
    public static void debugPut(TpaRequest request) {
        BY_ID.put(request.id(), request);
    }

    /** Withdraws a request filed by {@link #debugPut}, silently. */
    public static boolean debugRemove(UUID id) {
        return BY_ID.remove(id) != null;
    }

    /* ------------------------------------------- upkeep --------------------------------------------- */

    /** Forgets everything to or from this player, for logout. */
    public static void forget(UUID player) {
        BY_ID.values().removeIf(request ->
                request.from().equals(player) || request.to().equals(player));
    }

    /** Sweeps lapsed requests. Called once a tick, and only talks to anyone when something lapsed. */
    public static void tick(MinecraftServer server) {
        if (BY_ID.isEmpty()) return;

        long now = System.currentTimeMillis();
        List<TpaRequest> lapsed = new ArrayList<>();
        BY_ID.values().removeIf(request -> {
            if (!request.expired(now)) return false;
            lapsed.add(request);
            return true;
        });
        if (lapsed.isEmpty()) return;

        for (TpaRequest request : lapsed) {
            tell(server, request.from(), "msg.roll_mod.tpa.expired", request.toName());
            tell(server, request.to(), "msg.roll_mod.tpa.expired", request.fromName());
        }
        TpaViewers.resync(server);
    }

    /* ------------------------------------------- helpers -------------------------------------------- */

    private static @Nullable TpaRequest find(UUID from, UUID to) {
        for (TpaRequest request : BY_ID.values()) {
            if (request.from().equals(from) && request.to().equals(to)) return request;
        }
        return null;
    }

    /** The named player's request, or the oldest one when no name was given. */
    private static @Nullable TpaRequest pick(List<TpaRequest> requests, @Nullable String name) {
        if (requests.isEmpty()) return null;
        if (name == null || name.isBlank()) return requests.get(0);
        for (TpaRequest request : requests) {
            if (request.fromName().equalsIgnoreCase(name) || request.toName().equalsIgnoreCase(name)) {
                return request;
            }
        }
        return null;
    }

    private static void tell(MinecraftServer server, UUID id, String key, Object... args) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            player.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void refuse(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }
}
