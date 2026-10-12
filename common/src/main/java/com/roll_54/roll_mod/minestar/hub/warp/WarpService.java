package com.roll_54.roll_mod.minestar.hub.warp;

import com.roll_54.roll_mod.economy.currency.ledger.LedgerReason;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyLedger;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyCause;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.minestar.hub.QuestGate;
import com.roll_54.roll_mod.minestar.teleport.TeleportService;
import com.roll_54.roll_mod.network.packet.TeleportCountdownPacket;
import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

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

    /** The pause before a teleport fires; the same wait every other teleport in the mod uses. */
    public static final int WARMUP_SECONDS = TeleportService.WARMUP_SECONDS;

    /** The project's warning pink, used for the blocked-warp appeal line. */
    public static final int APPEAL_COLOR = 0xE00D50;

    /**
     * How long an instant teleport locks out the next one, in ticks.
     *
     * <p>Only official warps take the instant path, and only they need this: a warm-up teleport is
     * naturally idempotent, because a second request just replaces the pending one and one arrival
     * still follows. Arriving straight away has no such entry to overwrite, so a second
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

        // Moderators — operators, or a rank carrying the warp node — can clear out anyone's warp;
        // everyone else only their own. isModerator already includes operators.
        if (!warp.owner().equals(player.getUUID()) && !isModerator(player)) {
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

    /** A moderator's edit of a warp's name, description and price. */
    public static boolean edit(ServerPlayer moderator, Warp warp, String name, String description, long price) {
        if (name == null || name.isBlank()) {
            moderator.sendSystemMessage(Component.translatable("msg.roll_mod.warp.nameEmpty")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        return save(moderator, warp.withDetails(name, description, price), "msg.roll_mod.warp.edited");
    }

    /** Moves a warp to where the moderator stands. */
    public static boolean moveHere(ServerPlayer moderator, Warp warp) {
        return save(moderator, warp.movedTo(moderator), "msg.roll_mod.warp.moved");
    }

    /**
     * Sets, or with {@code 0} clears, the quest a visitor's team must have finished. Official warps
     * only: a player's warp is theirs to share, and gating it is not moderation's call.
     */
    public static boolean setRequiredQuest(ServerPlayer moderator, Warp warp, long quest) {
        if (quest != 0 && !warp.isAdmin()) {
            moderator.sendSystemMessage(Component.translatable("msg.roll_mod.warp.questOfficialOnly")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        return save(moderator, warp.withRequiredQuest(quest), quest == 0
                ? "msg.roll_mod.warp.questCleared" : "msg.roll_mod.warp.questSet");
    }

    private static boolean save(ServerPlayer moderator, Warp updated, String message) {
        if (!WarpData.get(moderator.server).replace(updated)) return false;
        moderator.sendSystemMessage(Component.translatable(message, LegacyText.display(updated.name()),
                QuestGate.title(false, updated.requiredQuest())));
        WarpViewers.resync(moderator.server);
        return true;
    }

    /** Whether {@code player} may use a quest-gated warp: their team finished the quest, or they moderate. */
    public static boolean questUnlocked(ServerPlayer player, Warp warp) {
        return !warp.questLocked() || isModerator(player)
                || QuestGate.isCompleted(player, warp.requiredQuest());
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
     * afford it is told immediately instead of after the countdown has run.
     *
     * <p>Official warps, and players holding the bypass permission, skip the warm-up entirely (see
     * {@link #begin}), but not these gates: the fee is independent of the wait, so an official warp
     * that charges still checks the balance here and still takes the money on arrival.
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

        // An official warp behind a quest opens once the player's team has finished it.
        if (!questUnlocked(player, warp)) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.questLocked",
                    QuestGate.title(false, warp.requiredQuest())).withStyle(ChatFormatting.RED));
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
        // An official warp is staff-made and staff-vouched, so there is nothing for a countdown to
        // protect against and it travels at once; the bypass permission says the same about a rank.
        // Note that "instant" means no warm-up, not no latency: a priced one has already been
        // through requestTeleport's balance lookup, which hops off the main thread and back, so it
        // lands a tick or more after the click either way.
        if (warp.isAdmin() || LuckPermsCompat.canBypassWarmup(player)) {
            long now = player.server.getTickCount();
            Long last = INSTANT.get(player.getUUID());
            if (last != null && now - last < INSTANT_COOLDOWN_TICKS) return;
            INSTANT.put(player.getUUID(), now);
            arrive(player, warp.id());
            return;
        }

        // The hub stays open through the warm-up: its countdown window shows the seconds the action
        // bar would (the hub covers the hotbar) and offers Cancel. arrive() closes it. Outside the
        // hub the action bar alone counts down, as before.
        UUID id = warp.id();
        TeleportService.schedule(player, WARMUP_SECONDS, arriving -> arrive(arriving, id));
        PacketDistributor.sendToPlayer(player, new TeleportCountdownPacket(WARMUP_SECONDS, warp.name()));
    }

    /** The countdown window's Cancel: stops the warm-up and closes the window. */
    public static void cancel(ServerPlayer player) {
        cancel(player.getUUID());
        PacketDistributor.sendToPlayer(player, TeleportCountdownPacket.stopped());
    }

    public static void cancel(UUID playerId) {
        TeleportService.cancel(playerId);
        // Logout comes through here too, so the instant lock-out does not outlive the session that
        // earned it — a player who reconnects within the second is not silently refused.
        INSTANT.remove(playerId);
    }

    private static void arrive(ServerPlayer player, UUID warpId) {
        Warp warp = WarpData.get(player.server).byId(warpId);
        if (warp == null) return;

        ServerLevel level = level(player.server, warp);
        if (level == null) return;
        // Again on arrival: a moderator may have gated it during the warm-up.
        if (!questUnlocked(player, warp)) return;

        // Out of the hub and back to the world. Only here, never on a refusal: a player who cannot
        // afford the trip keeps the list open to pick something else.
        player.closeContainer();
        PacketDistributor.sendToPlayer(player, TeleportCountdownPacket.stopped());
        TeleportService.teleport(player, level, warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch());
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
        CurrencyCause fee = CurrencyCause.player(player, LedgerReason.WARP)
                .withSender(warp.owner(), warp.ownerName())
                .withDetail("warp fee: " + LegacyText.plain(warp.name()));
        if (owner != null) {
            CurrencyLedger.logged(CurrencyLedger.logged(
                            CurrencyService.transfer(player, owner, CurrencyType.MAIN, price, player.server, fee),
                            player.getUUID(), CurrencyType.MAIN, -price, LedgerReason.WARP),
                    owner.getUUID(), CurrencyType.MAIN, price, LedgerReason.WARP);
        } else {
            // The owner is offline, so there is no ServerPlayer to credit through the service.
            // Take the fee anyway and settle it against their stored balance directly.
            CurrencyService.withdraw(player, CurrencyType.MAIN, price, fee).thenAccept(ok -> {
                if (ok) {
                    CurrencyLedger.record(player.getUUID(), CurrencyType.MAIN, -price, LedgerReason.WARP);
                    CurrencyLedger.logged(com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository
                            .addBalance(warp.owner(), CurrencyType.MAIN, price,
                                    fee.withSender(player).withDetail(fee.detail() + " (owner offline)")),
                            warp.owner(), CurrencyType.MAIN, price, LedgerReason.WARP);
                }
            });
        }
        player.sendSystemMessage(Component.translatable("msg.roll_mod.warp.paid", price));
    }

    private static ServerLevel level(MinecraftServer server, Warp warp) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, warp.dimension()));
    }
}
