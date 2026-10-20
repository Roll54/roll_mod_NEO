package com.roll_54.roll_mod.economy.api;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.ledger.LedgerReason;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.model.MoneyRequest;
import com.roll_54.roll_mod.economy.currency.repository.MoneyRequestRepository;
import com.roll_54.roll_mod.economy.money.MoneyViewers;
import com.roll_54.roll_mod.minestar.interactions.InteractionSettings;
import com.roll_54.roll_mod.util.PlayerLookup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Asking another player for money — the other half of {@link CurrencyOfferService}.
 *
 * <p>An offer holds the sender's coins until it is answered; a request holds nothing. The payer's
 * balance is only touched when they agree, and then in one transaction with the end of the request
 * (see {@link MoneyRequestRepository#pay}). A payer who is short is refused and the request stays, so
 * they can answer again once they have it.
 *
 * <p>Requests live in the database for the same reason offers do — the payer may well be offline —
 * and lapse after the same week.
 */
public final class MoneyRequestService {

    public static final long EXPIRY_MILLIS = CurrencyOfferService.EXPIRY_MILLIS;

    /** Open requests one player may have out at once. Keeps a spammer from flooding someone's list. */
    public static final int MAX_OPEN = 10;

    private static final int SWEEP_INTERVAL_TICKS = CurrencyOfferService.SWEEP_INTERVAL_TICKS;

    private static final AtomicBoolean SWEEPING = new AtomicBoolean();

    private static int sweepCounter = 0;

    private MoneyRequestService() {}

    /* --------------------------------------------- making -------------------------------------------- */

    /** Asks {@code payer} for {@code amount}. */
    public static void request(ServerPlayer requester, PlayerLookup.Profile payer, CurrencyType type, long amount) {
        MinecraftServer server = requester.server;
        if (requester.getUUID().equals(payer.id())) {
            fail(requester, "command.rmc.request.error.self");
            return;
        }
        if (amount <= 0) {
            fail(requester, "command.rmc.request.error.amount");
            return;
        }
        if (InteractionSettings.ignores(payer.id(), requester.getUUID())) {
            fail(requester, Component.translatable("command.rmc.pay.error.ignored", payer.name()));
            return;
        }

        outgoing(requester.getUUID()).thenAcceptAsync(open -> {
            if (open.size() >= MAX_OPEN) {
                fail(requester, Component.translatable("command.rmc.request.error.too_many", MAX_OPEN));
                return;
            }
            MoneyRequest request = new MoneyRequest(UUID.randomUUID(),
                    requester.getUUID(), requester.getGameProfile().getName(),
                    payer.id(), payer.name(), type, amount, System.currentTimeMillis() + EXPIRY_MILLIS);
            MoneyRequestRepository.create(request).thenAcceptAsync(created -> {
                if (!created) {
                    fail(requester, "command.rmc.offer.error.failed");
                    RollMod.LOGGER.error("Could not write {}'s money request to {}",
                            request.requesterName(), request.payerName());
                    return;
                }
                tell(server, request.requester(), Component.translatable("command.rmc.request.sent",
                        request.amount(), name(type), request.payerName()));
                tell(server, request.payer(), invitation(request));
                MoneyViewers.changed(server, request.requester(), request.payer());
            }, server);
        }, server);
    }

    /** The line the payer sees, with the two answers on it. */
    private static Component invitation(MoneyRequest request) {
        return Component.translatable("command.rmc.request.received",
                        request.requesterName(), request.amount(), name(request.type()))
                .append(" ")
                .append(Component.translatable("command.rmc.request.pay")
                        .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/rollmod money payrequest " + request.requesterName()))))
                .append(" ")
                .append(Component.translatable("command.rmc.request.refuse")
                        .withStyle(style -> style.withColor(ChatFormatting.RED)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/rollmod money refuse " + request.requesterName()))));
    }

    /* ------------------------------------------ answering -------------------------------------------- */

    /** The payer agrees to the named requester's request, or the one lapsing soonest. */
    public static void pay(ServerPlayer payer, @Nullable String requesterName) {
        pickIncoming(payer, byRequesterName(requesterName), requesterName, request -> pay(payer, request));
    }

    /** As {@link #pay(ServerPlayer, String)}, for one exact request — the hub's button. */
    public static void pay(ServerPlayer payer, UUID requestId) {
        pickIncoming(payer, r -> r.id().equals(requestId), null, request -> pay(payer, request));
    }

    private static void pay(ServerPlayer payer, MoneyRequest request) {
        MinecraftServer server = payer.server;
        CurrencyCause cause = CurrencyCause.player(payer, LedgerReason.TRANSFER)
                .withSender(request.requester(), request.requesterName()).withDetail("pay request");
        MoneyRequestRepository.pay(request.id(), cause).thenAcceptAsync(payment -> {
            switch (payment.outcome()) {
                case PAID -> {
                    CurrencyService.get(server, request.payer(), request.type());
                    CurrencyService.get(server, request.requester(), request.type());
                    tell(server, request.payer(), Component.translatable("command.rmc.request.paid.payer",
                            request.amount(), name(request.type()), request.requesterName()));
                    tell(server, request.requester(), Component.translatable(
                            "command.rmc.request.paid.requester",
                            request.payerName(), request.amount(), name(request.type())));
                }
                case INSUFFICIENT -> fail(payer, Component.translatable("command.rmc.pay.error.insufficient",
                        request.amount(), name(request.type())));
                case GONE -> fail(payer, "command.rmc.request.gone");
                case FAILED -> fail(payer, "command.rmc.offer.error.failed");
            }
            MoneyViewers.changed(server, request.requester(), request.payer());
        }, server);
    }

    /** The payer says no to the named requester's request, or the one lapsing soonest. */
    public static void refuse(ServerPlayer payer, @Nullable String requesterName) {
        pickIncoming(payer, byRequesterName(requesterName), requesterName, request -> end(payer, request, false));
    }

    public static void refuse(ServerPlayer payer, UUID requestId) {
        pickIncoming(payer, r -> r.id().equals(requestId), null, request -> end(payer, request, false));
    }

    /** The requester withdraws one of their own requests. */
    public static void cancel(ServerPlayer requester, UUID requestId) {
        MinecraftServer server = requester.server;
        outgoing(requester.getUUID()).thenAcceptAsync(open -> open.stream()
                .filter(r -> r.id().equals(requestId)).findFirst()
                .ifPresentOrElse(request -> end(requester, request, true),
                        () -> fail(requester, "command.rmc.request.gone")), server);
    }

    /** A refusal or a withdrawal: nothing was held, so the row just goes. */
    private static void end(ServerPlayer actor, MoneyRequest request, boolean cancelled) {
        MinecraftServer server = actor.server;
        MoneyRequestRepository.delete(request.id()).thenAcceptAsync(deleted -> {
            if (deleted.isEmpty()) {
                fail(actor, "command.rmc.request.gone");
            } else if (cancelled) {
                tell(server, request.requester(), Component.translatable("command.rmc.request.cancelled.requester",
                        request.amount(), name(request.type()), request.payerName()));
                tell(server, request.payer(), Component.translatable("command.rmc.request.cancelled.payer",
                        request.requesterName(), request.amount(), name(request.type())));
            } else {
                tell(server, request.payer(), Component.translatable("command.rmc.request.refused.payer",
                        request.amount(), name(request.type()), request.requesterName()));
                tell(server, request.requester(), Component.translatable("command.rmc.request.refused.requester",
                        request.payerName(), request.amount(), name(request.type())));
            }
            MoneyViewers.changed(server, request.requester(), request.payer());
        }, server);
    }

    private static void pickIncoming(ServerPlayer payer, Predicate<MoneyRequest> which, @Nullable String requesterName,
                                     java.util.function.Consumer<MoneyRequest> then) {
        incoming(payer.getUUID()).thenAcceptAsync(requests -> {
            Optional<MoneyRequest> picked = requests.stream().filter(which).findFirst();
            if (picked.isEmpty()) {
                fail(payer, requesterName == null
                        ? Component.translatable("command.rmc.request.none")
                        : Component.translatable("command.rmc.request.none.from", requesterName));
                return;
            }
            then.accept(picked.get());
        }, payer.server);
    }

    private static Predicate<MoneyRequest> byRequesterName(@Nullable String name) {
        return r -> name == null || r.requesterName().equalsIgnoreCase(name);
    }

    /* -------------------------------------------- listing -------------------------------------------- */

    public static CompletableFuture<List<MoneyRequest>> incoming(UUID player) {
        return MoneyRequestRepository.incoming(player).thenApply(MoneyRequestService::live);
    }

    public static CompletableFuture<List<MoneyRequest>> outgoing(UUID player) {
        return MoneyRequestRepository.outgoing(player).thenApply(MoneyRequestService::live);
    }

    /** Hides the lapsed ones the sweep has not reached yet, so they cannot be paid. */
    private static List<MoneyRequest> live(List<MoneyRequest> requests) {
        long now = System.currentTimeMillis();
        return requests.stream().filter(request -> !request.expired(now)).toList();
    }

    /** At login: what the player has been asked to pay while away. */
    public static void greet(ServerPlayer player) {
        incoming(player.getUUID()).thenAcceptAsync(requests -> {
            if (requests.isEmpty()) return;
            player.sendSystemMessage(Component.translatable("command.rmc.request.waiting",
                    requests.size()).withStyle(ChatFormatting.GOLD));
            for (MoneyRequest request : requests) {
                player.sendSystemMessage(invitation(request));
            }
        }, player.server);
    }

    /** Lists the player's requests both ways in chat — {@code /rollmod money requests}. */
    public static void list(ServerPlayer player) {
        MinecraftServer server = player.server;
        incoming(player.getUUID()).thenCombine(outgoing(player.getUUID()), Both::new).thenAcceptAsync(both -> {
            if (both.incoming().isEmpty() && both.outgoing().isEmpty()) {
                player.sendSystemMessage(Component.translatable("command.rmc.request.list.empty")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }
            for (MoneyRequest request : both.incoming()) player.sendSystemMessage(invitation(request));
            for (MoneyRequest request : both.outgoing()) {
                player.sendSystemMessage(Component.translatable("command.rmc.request.list.outgoing",
                        request.amount(), name(request.type()), request.payerName()).withStyle(ChatFormatting.GRAY));
            }
        }, server);
    }

    private record Both(List<MoneyRequest> incoming, List<MoneyRequest> outgoing) {}

    /* -------------------------------------------- expiry --------------------------------------------- */

    /** Drops what has lapsed, every {@link #SWEEP_INTERVAL_TICKS}. Nothing is held, so nothing to refund. */
    public static void tickExpiry(MinecraftServer server) {
        if (++sweepCounter < SWEEP_INTERVAL_TICKS) return;
        sweepCounter = 0;
        if (!SWEEPING.compareAndSet(false, true)) return;

        MoneyRequestRepository.deleteExpired(System.currentTimeMillis()).whenCompleteAsync((lapsed, error) -> {
            SWEEPING.set(false);
            if (error != null) {
                RollMod.LOGGER.error("Could not sweep expired money requests", error);
                return;
            }
            for (MoneyRequest request : lapsed) {
                tell(server, request.requester(), Component.translatable("command.rmc.request.expired",
                        request.amount(), name(request.type()), request.payerName()));
                MoneyViewers.changed(server, request.requester(), request.payer());
            }
        }, server);
    }

    /* --------------------------------------------- shared -------------------------------------------- */

    private static void tell(MinecraftServer server, UUID uuid, Component message) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) player.sendSystemMessage(message);
    }

    private static void fail(ServerPlayer player, String key) {
        fail(player, Component.translatable(key));
    }

    private static void fail(ServerPlayer player, Component message) {
        player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
    }

    private static Component name(CurrencyType type) {
        return type == CurrencyType.MAIN
                ? Component.translatable("currency.rollcurrency.name")
                : Component.literal(type.id());
    }
}
