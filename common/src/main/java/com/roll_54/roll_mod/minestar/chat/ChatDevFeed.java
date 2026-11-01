package com.roll_54.roll_mod.minestar.chat;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.network.packet.ChatAccessPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.Locale;
import java.util.Set;

/**
 * What the chat's DEV ("Команди") tab shows, to operators only ({@link ModerationPermissions#canSeeDevChat}):
 * <ul>
 *   <li>admin commands run by a player or the console, as typed — those a regular player (permission
 *       level 0) could not run; everyday commands like {@code /home} stay out;</li>
 *   <li>what those commands answered ({@code CommandSourceStackDevFeedMixin}), failures in red;</li>
 *   <li>errors KubeJS logs, from a log4j appender, at most {@link #ERRORS_PER_WINDOW} per
 *       {@link #ERROR_WINDOW_MS}; the rest are counted and summed up in one line.</li>
 * </ul>
 * Each player is told at login whether they get the tab ({@link ChatAccessPacket}).
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ChatDevFeed {

    /** Chat, not commands: what people say in private stays out of the feed. */
    private static final Set<String> CHAT_COMMANDS =
            Set.of("gc", "lc", "fc", "tc", "pm", "msg", "tell", "w", "me", "teammsg", "tm", "r");

    private static final int ERRORS_PER_WINDOW = 20;
    private static final long ERROR_WINDOW_MS = 10_000L;
    private static final int MAX_TEXT = 300;
    private static final String APPENDER_NAME = "roll_mod_chat_dev";

    private static volatile MinecraftServer server;
    private static DevAppender appender;

    /** Whether the command being run right now on the server thread is in the feed, so its answers are too. */
    private static boolean currentShown;

    private static long windowStart;
    private static int windowCount;
    private static int skipped;

    private ChatDevFeed() {}

    /* --------------------------------------------------- feed --------------------------------------------------- */

    /** Sends {@code line} to every staff member online. Server thread only. */
    public static void deliver(Component line) {
        MinecraftServer current = server;
        if (current == null) return;
        for (ServerPlayer player : current.getPlayerList().getPlayers()) {
            if (ModerationPermissions.canSeeDevChat(player)) ChatChannels.deliver(player, ChatChannel.DEV, line);
        }
    }

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String input = event.getParseResults().getReader().getString().strip();
        if (input.startsWith("/")) input = input.substring(1);
        int space = input.indexOf(' ');
        String root = (space < 0 ? input : input.substring(0, space)).toLowerCase(Locale.ROOT);
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        currentShown = !CHAT_COMMANDS.contains(root) && fromPlayerOrConsole(source) && admin(event.getParseResults());
        if (!currentShown) return;
        deliver(Component.literal("")
                .append(Component.literal("[⌘] ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(source.getTextName()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("/" + clip(input)).withStyle(ChatFormatting.WHITE)));
    }

    /** A command's answer to {@code source}; from {@code CommandSourceStackDevFeedMixin}. */
    public static void output(CommandSourceStack source, Component message, boolean failure) {
        if (server == null || !currentShown || !fromPlayerOrConsole(source)) return;
        if (!source.getServer().isSameThread()) return;
        deliver(Component.literal("")
                .append(Component.literal("  ↳ ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(source.getTextName() + ": ").withStyle(ChatFormatting.GRAY))
                .append(message.copy().withStyle(failure ? ChatFormatting.RED : ChatFormatting.GRAY)));
    }

    /**
     * An admin command: some part of it (the command, a sub-command, an {@code execute … run} target)
     * is closed to the same source at permission level 0, i.e. to a regular player.
     */
    private static boolean admin(ParseResults<CommandSourceStack> parse) {
        CommandSourceStack regular = parse.getContext().getSource().withPermission(0);
        for (CommandContextBuilder<CommandSourceStack> context = parse.getContext(); context != null; context = context.getChild()) {
            for (ParsedCommandNode<CommandSourceStack> node : context.getNodes()) {
                if (!node.getNode().canUse(regular)) return true;
            }
        }
        return false;
    }

    /** Players and the console; command blocks and functions would flood the tab. */
    private static boolean fromPlayerOrConsole(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer) return true;
        return source.getEntity() == null && source.hasPermission(4) && "Server".equals(source.getTextName());
    }

    private static String clip(String text) {
        return text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT) + "…";
    }

    /* ------------------------------------------------- errors -------------------------------------------------- */

    /** Off the server thread: rate-limited here, handed to the server thread to send. */
    private static void reportError(String logger, String message, Throwable thrown) {
        MinecraftServer current = server;
        if (current == null) return;
        String summary;
        synchronized (ChatDevFeed.class) {
            long now = System.currentTimeMillis();
            if (now - windowStart > ERROR_WINDOW_MS) {
                summary = skipped > 0 ? String.valueOf(skipped) : null;
                windowStart = now;
                windowCount = 0;
                skipped = 0;
            } else {
                summary = null;
            }
            if (++windowCount > ERRORS_PER_WINDOW) {
                skipped++;
                return;
            }
        }
        String shortName = logger.substring(logger.lastIndexOf('.') + 1);
        MutableComponent text = Component.literal(clip(message)).withStyle(ChatFormatting.RED);
        if (thrown != null) {
            text.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal(clip(thrown.toString())))));
        }
        Component line = Component.literal("")
                .append(Component.literal("[ERR] ").withStyle(ChatFormatting.DARK_RED))
                .append(Component.literal(shortName + ": ").withStyle(ChatFormatting.GRAY))
                .append(text);
        current.execute(() -> {
            if (summary != null) {
                deliver(Component.translatable("msg.roll_mod.chat.dev.skipped", summary).withStyle(ChatFormatting.DARK_GRAY));
            }
            deliver(line);
        });
    }

    /** Whose errors go to the tab: KubeJS's (script errors). */
    private static boolean watched(String logger) {
        String lower = logger.toLowerCase(Locale.ROOT);
        return lower.contains("kubejs");
    }

    private static final class DevAppender extends AbstractAppender {

        DevAppender() {
            super(APPENDER_NAME, null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            if (!event.getLevel().isMoreSpecificThan(Level.ERROR)) return;
            String logger = event.getLoggerName() == null ? "" : event.getLoggerName();
            if (!watched(logger)) return;
            reportError(logger, event.getMessage().getFormattedMessage(), event.getThrown());
        }
    }

    /* ------------------------------------------------ lifecycle ------------------------------------------------ */

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            appender = new DevAppender();
            appender.start();
            context.getConfiguration().getRootLogger().addAppender(appender, Level.ERROR, null);
            context.updateLoggers();
        } catch (RuntimeException | LinkageError e) {
            // A logging setup this cannot hook into only costs the error lines; commands still show.
            RollMod.LOGGER.warn("[Chat] could not watch the log for the DEV tab", e);
            appender = null;
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        server = null;
        if (appender == null) return;
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.getConfiguration().getRootLogger().removeAppender(APPENDER_NAME);
            context.updateLoggers();
            appender.stop();
        } catch (RuntimeException | LinkageError ignored) {
        }
        appender = null;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new ChatAccessPacket(ModerationPermissions.canSeeDevChat(player)));
        }
    }
}
