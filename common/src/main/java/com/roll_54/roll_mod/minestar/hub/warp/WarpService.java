package com.roll_54.roll_mod.minestar.hub.warp;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creating, deleting and travelling to warps. Every rule lives here rather than in the packet
 * handlers, so the UI can ask the same questions it does before offering a button.
 */
public final class WarpService {

    /** How many warps an ordinary player may keep, and how many a prime player may. */
    public static final int DEFAULT_LIMIT = 1;
    public static final int PRIME_LIMIT = 3;

    /**
     * Per-rank override, following {@code LUCKPERMS_VALUE_PERMISSIONS.md}: numbers belong in meta,
     * not in permission nodes. Unset falls back to the prime/default pair above.
     */
    private static final String LIMIT_META = "rollmod.warps.max";

    /** The pause before a teleport fires. Long enough to be interruptible, short enough to bear. */
    public static final int WARMUP_SECONDS = 5;

    /** The project's warning pink, used for the blocked-warp appeal line. */
    public static final int APPEAL_COLOR = 0xE00D50;

    /** Moving further than this from where the countdown started cancels it. */
    private static final double CANCEL_DISTANCE_SQR = 0.5 * 0.5;

    /** A teleport waiting out its warm-up. */
    private record Pending(UUID warp, Vec3 origin, long readyAtTick) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    /**
     * How long an instant teleport locks out the next one, in ticks.
     *
     * <p>Only official warps take the instant path, and only they need this: a warm-up teleport is
     * naturally idempotent, because a second request just overwrites the {@link #PENDING} entry and
     * one arrival still follows. Arriving straight away has no such entry to overwrite, so a second
     * packet would charge the fee twice. The button stays clickable until the container-close packet
     * has made the round trip, which is long enough for a double-click on a poor connection to get
     * two packets out.
     */
    private static final long INSTANT_COOLDOWN_TICKS = 20L;

    /** Player → the tick their last instant teleport fired. */
    private static final Map<UUID, Long> INSTANT = new ConcurrentHashMap<>();

    private WarpService() {}

    /* -------------------------------------------- limits -------------------------------------------- */

    public static int limitFor(ServerPlayer player) {
        Integer meta = LuckPermsCompat.warpLimit(player);
        if (meta != null) return Math.max(0, meta);
        return LuckPermsCompat.isPrime(player) ? PRIME_LIMIT : DEFAULT_LIMIT;
    }

    /* -------------------------------------------- create -------------------------------------------- */

    public static boolean create(ServerPlayer player, String name, String description, long price) {
        WarpData data = WarpData.get(player.server);
        int limit = limitFor(player);
        if (data.countFor(player.getUUID()) >= limit) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.limit", limit)
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (name == null || name.isBlank()) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.needName")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        Warp warp = Warp.at(player, name, description, price);
        data.add(warp);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.created",
                LegacyText.display(warp.name())));
        WarpViewers.resync(player.server);
        return true;
    }

    /* -------------------------------------------- delete -------------------------------------------- */

    public static boolean delete(ServerPlayer player, UUID warpId) {
        WarpData data = WarpData.get(player.server);
        Warp warp = data.byId(warpId);
        if (warp == null) return false;

        // Operators can clear out anyone's warp; everyone else only their own.
        if (!warp.owner().equals(player.getUUID()) && !player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.notOwner")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        data.remove(warpId);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.deleted",
                LegacyText.display(warp.name())));
        WarpViewers.resync(player.server);
        return true;
    }

    /* ------------------------------------------ moderation ------------------------------------------ */

    /**
     * Records a moderator's verdict. The moderator's name is kept with it so a blocked owner is told
     * who to appeal to rather than being left guessing.
     */
    public static boolean setApproval(ServerPlayer moderator, Warp warp, WarpApproval approval) {
        WarpData data = WarpData.get(moderator.server);
        Warp updated = warp.withApproval(approval, moderator.getGameProfile().getName());
        if (!data.replace(updated)) return false;

        moderator.sendSystemMessage(Component.translatable("msg.roll_mod.warp.approvalSet",
                LegacyText.display(updated.name()), approval.line()));

        // Tell the owner, if they are here to hear it: being blocked is not something to discover
        // by walking into it.
        ServerPlayer owner = moderator.server.getPlayerList().getPlayer(updated.owner());
        if (owner != null && approval == WarpApproval.DISAPPROVED) {
            owner.sendSystemMessage(appealMessage(updated));
        }

        WarpViewers.resync(moderator.server);
        return true;
    }

    /**
     * The line a blocked owner sees, in the project's warning pink. Shown both in chat when the
     * verdict lands and on the warp's own panel.
     */
    public static Component appealMessage(Warp warp) {
        return Component.translatable("msg.roll_mod.warp.disapproved",
                        warp.moderator().isEmpty() ? "?" : warp.moderator())
                .withStyle(style -> style.withColor(APPEAL_COLOR));
    }

    /**
     * Files a report. Each player counts once, so the number means "how many people objected", not
     * "how many times someone clicked".
     */
    public static void report(ServerPlayer reporter, Warp warp) {
        if (warp.reports().contains(reporter.getUUID())) {
            reporter.sendSystemMessage(Component.translatable("msg.roll_mod.warp.alreadyReported")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        Set<UUID> reports = new HashSet<>(warp.reports());
        reports.add(reporter.getUUID());
        Warp updated = warp.withReports(reports);
        WarpData.get(reporter.server).replace(updated);

        reporter.sendSystemMessage(Component.translatable("msg.roll_mod.warp.reported",
                LegacyText.display(updated.name())));
        notifyModerators(reporter, updated);
        WarpViewers.resync(reporter.server);
    }

    /**
     * Pings everyone who can act on a report. "Moderation and higher" is either an operator or a
     * rank carrying {@code rollmod.warps.moderate}, so a moderator who is not an op still hears.
     */
    private static void notifyModerators(ServerPlayer reporter, Warp warp) {
        Component message = Component.translatable("msg.roll_mod.warp.reportedToStaff",
                reporter.getGameProfile().getName(), LegacyText.display(warp.name()),
                warp.reports().size())
                .withStyle(ChatFormatting.GOLD);

        for (ServerPlayer staff : reporter.server.getPlayerList().getPlayers()) {
            if (isModerator(staff)) staff.sendSystemMessage(message);
        }
        RollMod.LOGGER.info("[Warps] {} reported '{}' ({} report(s)).",
                reporter.getGameProfile().getName(), warp.name(), warp.reports().size());
    }

    public static boolean isModerator(ServerPlayer player) {
        return player.hasPermissions(2) || LuckPermsCompat.canModerateWarps(player);
    }

    /* ------------------------------------------- teleport ------------------------------------------- */

    /**
     * Starts the warm-up. The price is checked now rather than on arrival, so a player who cannot
     * afford it is told immediately instead of after standing still for five seconds.
     *
     * <p>Official warps skip the warm-up entirely (see {@link #begin}), but not these gates: the
     * fee is independent of the wait, so an official warp that charges still checks the balance
     * here and still takes the money on arrival.
     */
    public static void requestTeleport(ServerPlayer player, UUID warpId) {
        WarpData data = WarpData.get(player.server);
        Warp warp = data.byId(warpId);
        if (warp == null) return;

        if (level(player.server, warp) == null) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.noDimension")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Moderation blocked it, so nobody but the owner is going there.
        if (!warp.canTeleport(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.blocked")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        long price = priceFor(player, warp);
        if (price > 0) {
            CurrencyService.get(player, CurrencyType.MAIN).thenAcceptAsync(balance -> {
                if (balance < price) {
                    player.sendSystemMessage(Component.translatable(
                            "msg.roll_mod.warp.cannotAfford", price).withStyle(ChatFormatting.RED));
                } else {
                    begin(player, warp);
                }
            }, player.server);
        } else {
            begin(player, warp);
        }
    }

    /** @see Warp#priceFor(UUID) */
    public static long priceFor(ServerPlayer player, Warp warp) {
        return warp.priceFor(player.getUUID());
    }

    private static void begin(ServerPlayer player, Warp warp) {
        // Out of the hub and back to the world. Only here, never on a refusal: a player who cannot
        // afford the trip keeps the list open to pick something else. It matters more for warps than
        // for anything else in the hub — the warm-up cancels on movement, and the countdown that
        // says so arrives in chat, behind a screen that covers most of it.
        player.closeContainer();

        // An official warp is staff-made and staff-vouched, so there is nothing for a countdown to
        // protect against and it travels at once. Note that "instant" means no warm-up, not no
        // latency: a priced one has already been through requestTeleport's balance lookup, which
        // hops off the main thread and back, so it lands a tick or more after the click either way.
        if (warp.isAdmin()) {
            long now = player.server.getTickCount();
            Long last = INSTANT.get(player.getUUID());
            if (last != null && now - last < INSTANT_COOLDOWN_TICKS) return;
            INSTANT.put(player.getUUID(), now);
            arrive(player, warp.id());
            return;
        }

        PENDING.put(player.getUUID(), new Pending(warp.id(), player.position(),
                player.server.getTickCount() + WARMUP_SECONDS * 20L));
        player.sendSystemMessage(
                Component.translatable("msg.roll_mod.warp.warmup", WARMUP_SECONDS,
                        LegacyText.display(warp.name())));
    }

    public static void cancel(UUID playerId) {
        PENDING.remove(playerId);
        // Logout comes through here too, so the instant lock-out does not outlive the session that
        // earned it — a player who reconnects within the second is not silently refused.
        INSTANT.remove(playerId);
    }

    /**
     * Advances every warm-up. Called once a tick from {@code HubEvents}; a player who moved out of
     * the starting spot loses the teleport, which is the whole point of the delay.
     */
    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;

        PENDING.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) return true;

            Pending pending = entry.getValue();
            if (player.position().distanceToSqr(pending.origin()) > CANCEL_DISTANCE_SQR) {
                player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.moved")
                        .withStyle(ChatFormatting.RED));
                return true;
            }
            if (server.getTickCount() < pending.readyAtTick()) return false;

            arrive(player, pending.warp());
            return true;
        });
    }

    private static void arrive(ServerPlayer player, UUID warpId) {
        Warp warp = WarpData.get(player.server).byId(warpId);
        if (warp == null) return;

        ServerLevel level = level(player.server, warp);
        if (level == null) return;

        player.teleportTo(level, warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch());
        player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.arrived", LegacyText.display(warp.name())));

        // Counted only once the player is actually there, for the same reason the fee is. withVisit
        // returns the warp untouched for the owner alone, so an owner cannot push their own warp up
        // the list; every other arrival is a real change and is saved and pushed to open hubs.
        Warp visited = warp.withVisit(player.getUUID());
        if (visited != warp) {
            WarpData.get(player.server).replace(visited);
            WarpViewers.resync(player.server);
        }

        // Paid only once the player is actually there, so a failed teleport is never charged for.
        long price = priceFor(player, warp);
        if (price <= 0) return;

        ServerPlayer owner = player.server.getPlayerList().getPlayer(warp.owner());
        if (owner != null) {
            CurrencyService.transfer(player, owner, CurrencyType.MAIN, price, player.server);
        } else {
            // The owner is offline, so there is no ServerPlayer to credit through the service.
            // Take the fee anyway and settle it against their stored balance directly.
            CurrencyService.withdraw(player, CurrencyType.MAIN, price).thenAccept(ok -> {
                if (ok) {
                    com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository
                            .addBalance(warp.owner(), CurrencyType.MAIN, price);
                }
            });
        }
        player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.paid", price));
    }

    private static ServerLevel level(MinecraftServer server, Warp warp) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, warp.dimension()));
    }
}
