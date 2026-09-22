package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod_server.RollModServer;
import com.roll_54.roll_mod.RollMod;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


@EventBusSubscriber(modid = RollModServer.MODID)
public class ChatHandler {

    private static final char GLOBAL_PREFIX = '!';
    private static final double LOCAL_RADIUS = 100.0;
    private static final double LOCAL_RADIUS_SQUARED = LOCAL_RADIUS * LOCAL_RADIUS;

    /**
     * Local chat: delivered inside the radius and then cancelled outright.
     *
     * <p>Runs at {@link EventPriority#HIGHEST} and cancels, so no other listener sees the message
     * at all. That is the whole point of a local channel — a chat bridge, a relay or a logger that
     * fires at the default priority would otherwise have already carried a message meant for
     * {@value #LOCAL_RADIUS} blocks to the whole server (and to Discord) before a cancel at
     * {@code LOWEST} ever ran. The price is that edits another listener would have made to the
     * text are not honoured for local chat, which is the trade this channel is worth.
     *
     * <p>The cancel is unconditional rather than conditional on delivery succeeding: a local
     * message that could not be delivered must still not fall through to the vanilla server-wide
     * broadcast, which is the opposite of what the channel is for.
     *
     * <p>No {@link ChatDelivery} mark here — cancelling already prevents the vanilla broadcast, and
     * a leftover mark would swallow this player's next message.
     *
     * <p>This is also where the global prefix is removed from the outgoing message, for the same
     * reason the local cancel lives at this priority: it is the last moment before any other
     * listener reads the text.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerChatLocal(ServerChatEvent event) {
        try {
            if (isGlobal(event)) {
                // Global chat is routed by the listener below, once every other listener has had
                // the message — but with the prefix already gone, so that nothing downstream (a
                // bridge, a relay, a logger) ever repeats the "!" the player typed.
                stripGlobalPrefix(event);
                return;
            }

            String rawMessage = messageText(event);
            if (rawMessage.isEmpty()) {
                return;
            }

            ServerPlayer sender = event.getPlayer();
            event.setCanceled(true);

            if (MuteStore.isMuted(sender.getUUID())) {
                sender.sendSystemMessage(muteNotice());
                return;
            }

            handleLocalChat(sender, rawMessage);
        } catch (Exception e) {
            RollMod.LOGGER.error("An unexpected error occurred in the chat handler:", e);
        }
    }

    /**
     * Global chat, and the mute that stops it.
     *
     * <p>Never cancels, so every other {@link ServerChatEvent} listener still receives global
     * messages; the duplicate vanilla broadcast is dropped afterwards by
     * {@code PlayerListChatMixin} through {@link ChatDelivery}.
     *
     * <p>Runs at {@link EventPriority#LOWEST}: a mod that wants to cancel a global message gets to
     * do so before anything is sent, and the text is read from {@link ServerChatEvent#getMessage()}
     * so edits made by earlier listeners are honoured. That text no longer carries the prefix — it
     * was stripped at {@code HIGHEST} — so the channel is recognised by {@link #isGlobal} instead.
     * Local chat never reaches this listener; it was cancelled at {@code HIGHEST}.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onServerChat(ServerChatEvent event) {
        try {
            if (!isGlobal(event)) {
                return;
            }

            ServerPlayer sender = event.getPlayer();

            if (MuteStore.isMuted(sender.getUUID())) {
                sender.sendSystemMessage(muteNotice());
                // Nothing was sent to anyone, and vanilla must not send it either.
                ChatDelivery.markHandled(sender);
                return;
            }

            // Marked only once delivery succeeded: if routing bailed out, the message still
            // goes through the untouched vanilla broadcast instead of disappearing.
            if (handleGlobalChat(sender, messageText(event))) {
                ChatDelivery.markHandled(sender);
            }
        } catch (Exception e) {
            RollMod.LOGGER.error("An unexpected error occurred in the chat handler:", e);
        }
    }

    /** The message as both listeners read it: the component's text, or the raw text behind it. */
    private static String messageText(ServerChatEvent event) {
        String message = event.getMessage().getString().trim();
        return message.isEmpty() ? event.getRawText().trim() : message;
    }

    /**
     * {@return whether the player asked for the global channel}
     *
     * <p>Read from {@link ServerChatEvent#getRawText()}, the text exactly as typed, which no
     * listener can modify. The message component cannot be used to tell the channels apart: the
     * prefix is stripped out of it at {@code HIGHEST}, before anything else reads the message.
     */
    private static boolean isGlobal(ServerChatEvent event) {
        String raw = event.getRawText().trim();
        return !raw.isEmpty() && raw.charAt(0) == GLOBAL_PREFIX;
    }

    /** Rewrites the event's message without the global prefix, leaving the raw text untouched. */
    private static void stripGlobalPrefix(ServerChatEvent event) {
        String message = messageText(event);
        String content = withoutGlobalPrefix(message);
        if (!content.equals(message)) {
            event.setMessage(Component.literal(content));
        }
    }

    /** {@return {@code message} with a leading {@value #GLOBAL_PREFIX} removed, if it has one} */
    private static String withoutGlobalPrefix(String message) {
        return message.isEmpty() || message.charAt(0) != GLOBAL_PREFIX ? message : message.substring(1).trim();
    }

    private static Component muteNotice() {
        return Adventure.miniMessage(
                """
                <red> Ви в муті.
                """.strip()
        );
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ChatDelivery.forget(event.getEntity().getUUID());
    }

    /** {@return whether the message was delivered here, so vanilla must not broadcast it} */
    private static boolean handleGlobalChat(ServerPlayer sender, String message) {
        // Already stripped at HIGHEST; stripped again only if that listener never ran.
        String content = withoutGlobalPrefix(message);
        MinecraftServer server = sender.getServer();

        if (server == null) {
            RollMod.LOGGER.error("Could not handle global chat: Server instance was null for player {}.", sender.getGameProfile().getName());
            return false;
        }

        if (content.isEmpty()) {
            sender.sendSystemMessage(Component.literal("Порожнє глобальне повідомлення.").withStyle(ChatFormatting.RED));
            logGlobal(sender, content, List.of());
            return true;
        }

        Component messageComponent = Component.literal("")
                .append(lpMeta(sender, true))
                .append(sender.getDisplayName())
                .append(lpMeta(sender, false))
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(content).withStyle(ChatFormatting.WHITE));

        List<ServerPlayer> recipients = server.getPlayerList().getPlayers();
        recipients.forEach(player -> player.sendSystemMessage(messageComponent));

        logGlobal(sender, content, recipients);
        return true;
    }

    /**
     * Delivers a local message to everyone within {@value #LOCAL_RADIUS} blocks.
     *
     * <p>Nothing is reported back: the caller has cancelled the event either way, because a local
     * message that failed to deliver must not fall back to a server-wide broadcast.
     */
    private static void handleLocalChat(ServerPlayer sender, String content) {
        MinecraftServer server = sender.getServer();

        if (server == null) {
            RollMod.LOGGER.error("Could not handle local chat: Server instance was null for player {}.", sender.getGameProfile().getName());
            return;
        }

        List<ServerPlayer> nearbyPlayers = server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level().dimension().equals(sender.level().dimension()))
                .filter(player -> player.distanceToSqr(sender) <= LOCAL_RADIUS_SQUARED)
                .collect(Collectors.toList());

        if (nearbyPlayers.size() <= 1) {
            Component noFoundNearbyPlayers;
            noFoundNearbyPlayers = Adventure.miniMessage(
                    """
                     <gray>Вас ніхто не чує. Постав <white>!</white> на початку, щоб написати в глобальний чат.
                    """.strip()
            );

            sender.sendSystemMessage(noFoundNearbyPlayers);

            logLocalNobody(sender, content);
            return;
        }

        Component messageComponent = Component.literal("")
                .append(lpMeta(sender, true))
                .append(sender.getDisplayName())
                .append(lpMeta(sender, false))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(content).withStyle(ChatFormatting.GRAY));

        nearbyPlayers.forEach(player -> player.sendSystemMessage(messageComponent));

        logLocal(sender, content, nearbyPlayers);
    }


    /**
     * Returns the player's LuckPerms prefix ({@code prefix == true}) or suffix
     * ({@code prefix == false}) as a Component, parsed as MiniMessage. Returns an
     * empty Component when the value is unset or LuckPerms is unavailable, so it
     * can be appended unconditionally.
     */
    private static Component lpMeta(ServerPlayer player, boolean prefix) {
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
            if (user == null) return Component.empty();
            CachedMetaData meta = user.getCachedData().getMetaData();
            String value = prefix ? meta.getPrefix() : meta.getSuffix();
            if (value == null || value.isEmpty()) return Component.empty();
            return Adventure.miniMessage(value);
        } catch (Exception e) {
            RollMod.LOGGER.warn("Failed to resolve LuckPerms {} for {}",
                    prefix ? "prefix" : "suffix", player.getGameProfile().getName(), e);
            return Component.empty();
        }
    }

    private static void logGlobal(ServerPlayer sender, String message, List<ServerPlayer> recipients) {
        String recList = recipients.stream().map(p -> p.getGameProfile().getName()).collect(Collectors.joining(", "));
        if (recList.isEmpty()) recList = "none";
        Vec3 pos = sender.position();

        String logMessage = String.format(Locale.US, "[GLOBAL] %s (%s) @%s pos=%.1f %.1f %.1f -> \"%s\" recipients=[%s]",
                sender.getGameProfile().getName(), sender.getUUID(), sender.level().dimension().location(),
                pos.x, pos.y, pos.z, message, recList);
        RollMod.LOGGER.info(logMessage);
    }

    private static void logLocal(ServerPlayer sender, String message, List<ServerPlayer> recipients) {
        String recList = recipients.stream().map(p -> p.getGameProfile().getName()).collect(Collectors.joining(", "));
        Vec3 pos = sender.position();

        String logMessage = String.format(Locale.US, "[LOCAL] %s (%s) @%s pos=%.1f %.1f %.1f -> \"%s\" recipients=[%s]",
                sender.getGameProfile().getName(), sender.getUUID(), sender.level().dimension().location(),
                pos.x, pos.y, pos.z, message, recList);
        RollMod.LOGGER.info(logMessage);
    }

    private static void logLocalNobody(ServerPlayer sender, String message) {
        Vec3 pos = sender.position();

        String logMessage = String.format(Locale.US, "[LOCAL] %s (%s) @%s pos=%.1f %.1f %.1f -> \"%s\" NOBODY_HEARD",
                sender.getGameProfile().getName(), sender.getUUID(), sender.level().dimension().location(),
                pos.x, pos.y, pos.z, message);
        RollMod.LOGGER.info(logMessage);
    }
}