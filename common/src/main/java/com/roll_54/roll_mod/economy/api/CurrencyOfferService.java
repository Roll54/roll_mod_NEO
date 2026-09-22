package com.roll_54.roll_mod.economy.api;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.model.CurrencyOffer;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyOfferRepository;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyOfferRepository.Payee;
import com.roll_54.roll_mod.util.PlayerLookup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player-to-player payments, which the recipient has to accept.
 *
 * <p>The money is held, not promised: {@link #offer} withdraws it before the offer exists, and the
 * amount is paid to the recipient on an accept or back to the sender on a deny or an expiry. So what
 * a recipient is shown is money that is really there, a sender cannot offer the same coins to two
 * people, and an offer nobody answers costs nothing in the end.
 *
 * <p>Offers live in the database rather than in memory, which is the opposite of {@code TpaService}
 * and for the opposite reason: a lost teleport request is a promise nobody remembers, but a lost
 * offer is money that stopped existing. That is also what lets an offer wait — {@link
 * #EXPIRY_MILLIS} is a week, long enough for someone to be paid while they are away and read it on
 * their next login.
 *
 * <p>Admins do not go through any of this. {@code /rollmod money admin add} credits the balance
 * outright, which is the point of it being the admin command.
 */
public final class CurrencyOfferService {

    /** How long an unanswered offer stands. A week: the recipient may simply not be playing. */
    public static final long EXPIRY_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    /** How often the lapsed ones are swept up and refunded. */
    public static final int SWEEP_INTERVAL_TICKS = 20 * 60;

    /** One sweep at a time. Each runs on the database executor and may outlast its interval. */
    private static final AtomicBoolean SWEEPING = new AtomicBoolean();

    private static int sweepCounter = 0;

    private CurrencyOfferService() {}

    /* --------------------------------------------- making -------------------------------------------- */

    /**
     * Offers {@code amount} to {@code recipient}, holding it until they answer.
     *
     * <p>The recipient is a {@link PlayerLookup.Profile} and not a {@link ServerPlayer}: paying
     * somebody who is offline is the common case for an offer, not the exception.
     */
    public static void offer(ServerPlayer sender, PlayerLookup.Profile recipient,
                             CurrencyType type, long amount) {
        MinecraftServer server = sender.server;

        if (sender.getUUID().equals(recipient.id())) {
            fail(sender, "command.rmc.pay.error.self");
            return;
        }

        CurrencyOffer offer = new CurrencyOffer(UUID.randomUUID(),
                sender.getUUID(), sender.getGameProfile().getName(),
                recipient.id(), recipient.name(), type, amount,
                System.currentTimeMillis() + EXPIRY_MILLIS);

        // The debit and the row go in together, so there is no moment where the money has left the
        // sender with nothing recording who is owed it. Nothing here needs a refund path.
        CurrencyOfferRepository.hold(offer).thenAcceptAsync(held -> {
            switch (held) {
                case HELD -> {
                    // The balance changed inside that transaction, so the client's copy is stale.
                    CurrencyService.get(server, sender.getUUID(), type);
                    announce(server, offer);
                }
                case INSUFFICIENT -> fail(sender, Component.translatable(
                        "command.rmc.pay.error.insufficient", amount, name(type)));
                case FAILED -> {
                    fail(sender, "command.rmc.offer.error.failed");
                    RollMod.LOGGER.error("Could not hold {} {} for {}'s offer to {}",
                            amount, type.id(), offer.senderName(), offer.recipientName());
                }
            }
        }, server);
    }

    /** Tells both ends about a new offer — the recipient with the two answers on the line. */
    private static void announce(MinecraftServer server, CurrencyOffer offer) {
        ServerPlayer sender = online(server, offer.sender());
        if (sender != null) {
            sender.sendSystemMessage(Component.translatable("command.rmc.offer.sent",
                    offer.amount(), name(offer.type()), offer.recipientName()));
        }

        ServerPlayer recipient = online(server, offer.recipient());
        if (recipient != null) {
            recipient.sendSystemMessage(invitation(offer));
        }
    }

    /** The line the recipient sees. Chat is where this is answered, so the answers are on it. */
    private static Component invitation(CurrencyOffer offer) {
        return Component.translatable("command.rmc.offer.received",
                        offer.senderName(), offer.amount(), name(offer.type()))
                .append(" ")
                .append(Component.translatable("command.rmc.offer.accept")
                        .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/rollmod money accept " + offer.senderName()))))
                .append(" ")
                .append(Component.translatable("command.rmc.offer.deny")
                        .withStyle(style -> style.withColor(ChatFormatting.RED)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                        "/rollmod money deny " + offer.senderName()))));
    }

    /* ------------------------------------------ answering -------------------------------------------- */

    /** Takes the money: the named sender's offer, or the one lapsing soonest. */
    public static void accept(ServerPlayer recipient, @Nullable String senderName) {
        answer(recipient, senderName, Payee.RECIPIENT);
    }

    /** Sends it back: same choice of offer, the other destination. */
    public static void deny(ServerPlayer recipient, @Nullable String senderName) {
        answer(recipient, senderName, Payee.SENDER);
    }

    private static void answer(ServerPlayer recipient, @Nullable String senderName, Payee payee) {
        MinecraftServer server = recipient.server;

        incoming(recipient.getUUID()).thenAcceptAsync(offers -> {
            Optional<CurrencyOffer> picked = pick(offers, senderName);
            if (picked.isEmpty()) {
                fail(recipient, senderName == null
                        ? Component.translatable("command.rmc.offer.none")
                        : Component.translatable("command.rmc.offer.none.from", senderName));
                return;
            }

            CurrencyOffer offer = picked.get();
            CurrencyOfferRepository.claim(offer.id(), payee).thenAcceptAsync(claimed -> {
                if (claimed.isEmpty()) {
                    // Answered or swept between the list and the claim. Nothing was paid, and
                    // saying so is better than a message about money that did not move.
                    fail(recipient, "command.rmc.offer.gone");
                    return;
                }
                settled(server, claimed.get(), payee);
            }, server);
        }, server);
    }

    /**
     * Tells both ends where the money went, and refreshes whoever is online.
     *
     * <p>The balances were changed inside the claim's transaction, so the client caches are stale
     * until they are read back; {@link CurrencyService#get} does the read and the push.
     */
    private static void settled(MinecraftServer server, CurrencyOffer offer, Payee payee) {
        CurrencyService.get(server, offer.recipient(), offer.type());
        CurrencyService.get(server, offer.sender(), offer.type());

        String suffix = payee == Payee.RECIPIENT ? "accepted" : "denied";
        tell(server, offer.recipient(), Component.translatable(
                "command.rmc.offer." + suffix + ".recipient",
                offer.amount(), name(offer.type()), offer.senderName()));
        tell(server, offer.sender(), Component.translatable(
                "command.rmc.offer." + suffix + ".sender",
                offer.amount(), name(offer.type()), offer.recipientName()));
    }

    /** The named sender's offer, or the one closest to lapsing. Names are matched as typed. */
    private static Optional<CurrencyOffer> pick(List<CurrencyOffer> offers,
                                                @Nullable String senderName) {
        return offers.stream()
                .filter(offer -> senderName == null
                        || offer.senderName().equalsIgnoreCase(senderName))
                .findFirst();
    }

    /* -------------------------------------------- listing -------------------------------------------- */

    public static CompletableFuture<List<CurrencyOffer>> incoming(UUID player) {
        return CurrencyOfferRepository.incoming(player).thenApply(CurrencyOfferService::live);
    }

    public static CompletableFuture<List<CurrencyOffer>> outgoing(UUID player) {
        return CurrencyOfferRepository.outgoing(player).thenApply(CurrencyOfferService::live);
    }

    /**
     * Drops the ones that have lapsed but have not been swept up yet.
     *
     * <p>The sweep runs on its own schedule, so between an offer's deadline and its refund there is
     * a window where the row is still there. Hiding it in that window is what stops a recipient
     * taking money the sender has already been told they would get back.
     */
    private static List<CurrencyOffer> live(List<CurrencyOffer> offers) {
        long now = System.currentTimeMillis();
        return offers.stream().filter(offer -> !offer.expired(now)).toList();
    }

    /**
     * Greets a player with what is waiting for them.
     *
     * <p>An offer can be a week old by the time it is read, so the login message is the only thing
     * that makes offline paying work at all.
     */
    public static void greet(ServerPlayer player) {
        incoming(player.getUUID()).thenAcceptAsync(offers -> {
            if (offers.isEmpty()) return;
            player.sendSystemMessage(Component.translatable("command.rmc.offer.waiting",
                    offers.size()).withStyle(ChatFormatting.GOLD));
            for (CurrencyOffer offer : offers) {
                player.sendSystemMessage(invitation(offer));
            }
        }, player.server);
    }

    /* -------------------------------------------- expiry --------------------------------------------- */

    /**
     * Refunds what has lapsed, every {@link #SWEEP_INTERVAL_TICKS}.
     *
     * <p>Called from the economy's tick handler. The counter is on the server thread; the sweep
     * itself is not, which is why one is not started while another is still running.
     */
    public static void tickExpiry(MinecraftServer server) {
        if (++sweepCounter < SWEEP_INTERVAL_TICKS) return;
        sweepCounter = 0;
        if (!SWEEPING.compareAndSet(false, true)) return;

        CurrencyOfferRepository.claimExpired(System.currentTimeMillis())
                .whenCompleteAsync((refunded, error) -> {
                    SWEEPING.set(false);
                    if (error != null) {
                        RollMod.LOGGER.error("Could not sweep expired currency offers", error);
                        return;
                    }
                    for (CurrencyOffer offer : refunded) {
                        CurrencyService.get(server, offer.sender(), offer.type());
                        tell(server, offer.sender(), Component.translatable(
                                "command.rmc.offer.expired",
                                offer.amount(), name(offer.type()), offer.recipientName()));
                    }
                }, server);
    }

    /* --------------------------------------------- shared -------------------------------------------- */

    @Nullable
    private static ServerPlayer online(MinecraftServer server, UUID uuid) {
        return server.getPlayerList().getPlayer(uuid);
    }

    /** Says something to a player if they are there to hear it. */
    private static void tell(MinecraftServer server, UUID uuid, Component message) {
        ServerPlayer player = online(server, uuid);
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
