package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannels;
import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod_server.RollModServer;
import com.roll_54.roll_mod.RollMod;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * Global and local chat, both as commands: {@code /gc <message>} and {@code /lc <message>}, what the
 * Global and Local (and All) buttons over the chat box send through, alongside {@code /fc} and
 * {@code /tc}. Plain chat is locked — the client never sends it, so anything that arrives as plain
 * chat is refused with a pointer to the buttons and commands.
 *
 * <p>Global still goes through a {@link ServerChatEvent}: {@code /gc} posts one itself, so other
 * listeners — chat_ld's Discord bridge, relays, loggers — keep seeing global messages, and may still
 * cancel or edit them before they are delivered. Local never posts one, so it never leaves the
 * {@value #LOCAL_RADIUS}-block radius.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public class ChatHandler {

    private static final double LOCAL_RADIUS = 100.0;
    private static final double LOCAL_RADIUS_SQUARED = LOCAL_RADIUS * LOCAL_RADIUS;

    /** The chat event {@code /gc} is posting right now, which the plain-chat lock must let through. */
    private static final ThreadLocal<ServerChatEvent> RELAYING = new ThreadLocal<>();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gc")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            sendGlobal(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "message").trim());
                            return 1;
                        })));
        event.getDispatcher().register(Commands.literal("lc")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            sendLocal(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "message").trim());
                            return 1;
                        })));
    }

    /**
     * Plain chat is locked: cancelled before any other listener sees it, so it can neither be
     * broadcast by vanilla nor carried anywhere by a bridge. Only the event {@code /gc} posts
     * itself passes.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerChat(ServerChatEvent event) {
        if (RELAYING.get() == event) return;
        event.setCanceled(true);
        event.getPlayer().sendSystemMessage(Component.translatable("msg.roll_mod.chat.use_commands").withStyle(ChatFormatting.GRAY));
    }

    private static void sendGlobal(ServerPlayer sender, String text) {
        if (text.isEmpty()) return;
        if (MuteStore.isMuted(sender.getUUID())) {
            sender.sendSystemMessage(muteNotice());
            return;
        }

        // Posted so every other chat listener still gets global messages; a listener that cancels
        // stops the message, one that edits it is honoured.
        ServerChatEvent event = new ServerChatEvent(sender, text, Component.literal(text));
        RELAYING.set(event);
        try {
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) return;
        } finally {
            RELAYING.remove();
        }
        String content = event.getMessage().getString().trim();
        if (content.isEmpty()) return;
        handleGlobalChat(sender, content);
    }

    private static void sendLocal(ServerPlayer sender, String text) {
        if (text.isEmpty()) return;
        if (MuteStore.isMuted(sender.getUUID())) {
            sender.sendSystemMessage(muteNotice());
            return;
        }
        handleLocalChat(sender, text);
    }

    private static Component muteNotice() {
        return Adventure.miniMessage(
                """
                <red> Ви в муті.
                """.strip()
        );
    }

    private static void handleGlobalChat(ServerPlayer sender, String content) {
        MinecraftServer server = sender.getServer();

        if (server == null) {
            RollMod.LOGGER.error("Could not handle global chat: Server instance was null for player {}.", sender.getGameProfile().getName());
            return;
        }

        Component messageComponent = Component.literal("")
                .append(lpPrefix(sender))
                .append(sender.getDisplayName())
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(content).withStyle(ChatFormatting.WHITE));

        List<ServerPlayer> recipients = server.getPlayerList().getPlayers();
        // Tagged with its channel, so the client can sort it under the Global button.
        // Skipping whoever ignores the sender (InteractionSettings).
        recipients.forEach(player -> ChatChannels.deliver(player, ChatChannel.GLOBAL, messageComponent, sender.getUUID()));

        logGlobal(sender, content, recipients);
    }

    /**
     * Delivers a local message to everyone within {@value #LOCAL_RADIUS} blocks.
     *
     * <p>Never posts a chat event, so no bridge or relay ever carries it past the radius.
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

        if (sender.isSpectator()) {
            handleSpectatorLocalChat(sender, content, nearbyPlayers);
            return;
        }

        if (nearbyPlayers.size() <= 1) {
            Component noFoundNearbyPlayers;
            noFoundNearbyPlayers = Component.translatable("msg.roll_mod.chat.local.nobody").withStyle(ChatFormatting.GRAY);

            // On the local channel, so it still shows while the chat is filtered to Local.
            ChatChannels.deliver(sender, ChatChannel.LOCAL, noFoundNearbyPlayers);

            logLocalNobody(sender, content);
            return;
        }

        Component messageComponent = Component.literal("")
                .append(lpPrefix(sender))
                .append(sender.getDisplayName())
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(content).withStyle(ChatFormatting.GRAY));

        nearbyPlayers.forEach(player -> ChatChannels.deliver(player, ChatChannel.LOCAL, messageComponent, sender.getUUID()));

        logLocal(sender, content, nearbyPlayers);
    }


    /**
     * A spectator's local message is silent: it reaches no player in survival or creative, only the
     * spectators in range — the sender included — and to them it shows in yellow, tagged silent. The
     * sender is warned that nobody heard them, since nobody playing did.
     */
    private static void handleSpectatorLocalChat(ServerPlayer sender, String content, List<ServerPlayer> nearbyPlayers) {
        List<ServerPlayer> spectators = nearbyPlayers.stream().filter(ServerPlayer::isSpectator).collect(Collectors.toList());

        Component messageComponent = Component.literal("")
                .append(Component.translatable("msg.roll_mod.chat.local.silent_tag").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" "))
                .append(lpPrefix(sender))
                .append(sender.getDisplayName())
                .append(Component.literal(": ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(content).withStyle(ChatFormatting.YELLOW));

        spectators.forEach(player -> ChatChannels.deliver(player, ChatChannel.LOCAL, messageComponent, sender.getUUID()));
        ChatChannels.deliver(sender, ChatChannel.LOCAL,
                Component.translatable("msg.roll_mod.chat.local.spectator_nobody").withStyle(ChatFormatting.GRAY));

        logLocalSpectator(sender, content, spectators);
    }

    /**
     * The player's LuckPerms prefix as a Component, parsed as MiniMessage, or an empty Component when
     * it is unset or LuckPerms is unavailable, so it can be appended unconditionally. Chat shows only
     * the prefix; the (coloured) suffix is left out.
     */
    static Component lpPrefix(ServerPlayer player) {
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
            if (user == null) return Component.empty();
            String value = user.getCachedData().getMetaData().getPrefix();
            if (value == null || value.isEmpty()) return Component.empty();
            return Adventure.miniMessage(value);
        } catch (Exception e) {
            RollMod.LOGGER.warn("Failed to resolve LuckPerms prefix for {}", player.getGameProfile().getName(), e);
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

    private static void logLocalSpectator(ServerPlayer sender, String message, List<ServerPlayer> recipients) {
        String recList = recipients.stream().map(p -> p.getGameProfile().getName()).collect(Collectors.joining(", "));
        Vec3 pos = sender.position();

        String logMessage = String.format(Locale.US, "[LOCAL] %s (%s) @%s pos=%.1f %.1f %.1f -> \"%s\" SPECTATOR_SILENT recipients=[%s]",
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