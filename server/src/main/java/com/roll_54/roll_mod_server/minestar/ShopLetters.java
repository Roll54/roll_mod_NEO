package com.roll_54.roll_mod_server.minestar;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import com.roll_54.roll_mod.minestar.letters.LetterReward;
import com.roll_54.roll_mod.minestar.letters.ShopLetterFormat;
import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ua.com.minestar.model.ShopProduct;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ua.com.minestar.Minestar.minestar;

/**
 * Minestar shop purchases, delivered as personal letters.
 *
 * <p>Each pending product becomes one letter: a thank-you for supporting Project Minestar, with the
 * product attached as a console command reward ({@link LetterReward#command(String, String, int,
 * boolean)}) — its commands, its name, its quantity. Accepting the letter runs the commands from the
 * console, {@code %player%} replaced, exactly as {@code /shop-receive} used to run them on the spot.
 * The letter never lapses: it is something paid for.
 *
 * <p>Checked at login, every {@link #INTERVAL_TICKS} for everyone online, and whenever the player
 * runs {@code /shop-receive}.
 *
 * <p><b>No product becomes two letters.</b> Once its letter is written the product's id goes into
 * {@code minestar/shop_letters.json}, <em>before</em> Minestar is asked to drop it from the pending
 * list. If that call fails the product is still pending next time, and the id says its letter
 * already exists. Ids leave the file once Minestar has confirmed the drop.
 */
@EventBusSubscriber(modid = RollModServer.MODID, value = Dist.DEDICATED_SERVER)
public final class ShopLetters {

    /** Ten minutes. */
    private static final int INTERVAL_TICKS = 20 * 60 * 10;

    /** Players with a check on its way to or from Minestar — one at a time each. */
    private static final Set<UUID> IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private static int ticks = 0;

    private ShopLetters() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++ticks < INTERVAL_TICKS) return;
        ticks = 0;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            deliver(player, false);
        }
    }

    /**
     * Turns whatever the player has waiting in the Minestar shop into letters.
     *
     * @param verbose whether to say so when there is nothing — true for {@code /shop-receive}, which
     *                the player asked for; the login check and the timer stay quiet
     */
    public static void deliver(ServerPlayer player, boolean verbose) {
        UUID id = player.getUUID();
        if (!IN_FLIGHT.add(id)) return;
        MinecraftServer server = player.server;

        minestar.getUserPendingShopProductsByProfileUuidAndServerId(id)
                // onSuccess runs on Minestar's own thread; letters are server state.
                .onSuccess(products -> server.execute(() -> {
                    try {
                        convert(server, player, products == null ? List.of() : products, verbose);
                    } finally {
                        IN_FLIGHT.remove(id);
                    }
                }))
                .onFailure(cause -> {
                    IN_FLIGHT.remove(id);
                    RollMod.LOGGER.error("[Shop] Could not read pending products of {}.",
                            player.getGameProfile().getName(), cause);
                    if (verbose) server.execute(() -> player.sendSystemMessage(
                            Component.translatable("msg.roll_mod.letters.shop.failed").withStyle(ChatFormatting.RED)));
                });
    }

    private static void convert(MinecraftServer server, ServerPlayer player, List<ShopProduct> products,
                                boolean verbose) {
        Set<Long> written = load();
        Set<Long> pending = new HashSet<>();
        List<ShopProduct> fresh = new ArrayList<>();
        String name = player.getGameProfile().getName();

        for (ShopProduct product : products) {
            pending.add(product.id());
            if (written.contains(product.id())) continue;
            if (product.commands() == null || product.commands().isEmpty()) {
                RollMod.LOGGER.warn("[Shop] Product {} ('{}') has no commands; skipped.", product.id(), product.name());
                continue;
            }
            fresh.add(product);
        }

        // Everything in one letter, listed in the body and attached one reward per product. A letter
        // holds Letter.MAX_REWARDS; past that the rest goes in a further letter rather than being lost.
        List<ShopLetterFormat.Product> listed = fresh.stream()
                .map(product -> new ShopLetterFormat.Product(product.name(),
                        String.join("\n", product.commands()), product.quantity()))
                .toList();
        int letters = 0;
        for (ShopLetterFormat.Draft draft : ShopLetterFormat.drafts(name, listed)) {
            ShopLetterFormat.send(server, player.getUUID(), draft);
            for (ShopProduct product : fresh.subList(draft.from(), draft.to())) {
                ShopReceiveLogger.logReceived(player, product);
                written.add(product.id());
            }
            letters++;
        }
        // Written before Minestar is told, so a failed confirmation cannot become a second letter.
        save(written);

        if (letters > 0) {
            player.sendSystemMessage(notice(letters));
        } else if (verbose) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.letters.shop.none")
                    .withStyle(ChatFormatting.YELLOW));
        }

        // Everything of this player's that has a letter and is still pending in Minestar — this
        // round's, and any whose confirmation failed before.
        Set<Long> confirm = new HashSet<>(pending);
        confirm.retainAll(written);
        if (confirm.isEmpty()) return;
        minestar.deleteUserPendingShopProductsByIds(confirm)
                .onSuccess(unit -> server.execute(() -> {
                    Set<Long> left = load();
                    left.removeAll(confirm);
                    save(left);
                    RollMod.LOGGER.info("[Shop] Confirmed delivery of [{}] to {} as letters.",
                            confirm.stream().map(String::valueOf).collect(Collectors.joining(", ")), name);
                }))
                .onFailure(cause -> RollMod.LOGGER.error(
                        "[Shop] Could not confirm delivery of {} to {}; kept on file, no letter will be repeated.",
                        confirm, name, cause));
    }

    /** "You got a new letter!" — with a click that opens the hub on the letters. */
    private static Component notice(int count) {
        Component open = Component.translatable("msg.roll_mod.letters.shop.open")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rollmod hub letters"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("msg.roll_mod.letters.shop.open.tip"))));
        return Component.translatable(count == 1 ? "msg.roll_mod.letters.shop.new" : "msg.roll_mod.letters.shop.new.many",
                        count).withStyle(ChatFormatting.GOLD)
                .append(" ").append(open);
    }

    /* --------------------------------------------- file --------------------------------------------- */

    private static Path file() {
        return MinestarFiles.dir().resolve("shop_letters.json");
    }

    private static synchronized Set<Long> load() {
        Set<Long> ids = new LinkedHashSet<>();
        JsonElement json = MinestarFiles.read(file());
        if (json != null && json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                try {
                    ids.add(element.getAsLong());
                } catch (RuntimeException ignored) {
                    // A hand-edited entry that is not a number is dropped.
                }
            }
        }
        return ids;
    }

    private static synchronized void save(Set<Long> ids) {
        JsonArray array = new JsonArray();
        ids.forEach(array::add);
        MinestarFiles.write(file(), array);
    }
}
