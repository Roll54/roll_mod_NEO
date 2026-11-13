package com.roll_54.roll_mod.minestar.chat;

import com.roll_54.roll_mod.registry.ModConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * The recent public chat (Global, Trade, and the Discord bridge's lines) for players joining: they get the
 * lines of the last {@code chat_changes.joinHistoryMinutes}, at most {@code joinHistoryLines}, under a header,
 * so they can follow what is going on. Each line goes back on its own channel, so it sorts into its tab, and
 * keeps its writer, so it can be replied to and is skipped for players ignoring them. In memory only: a
 * restart starts fresh. Ported from Eden_LD's {@code ChatHistory}.
 */
@EventBusSubscriber(modid = com.roll_54.roll_mod.RollMod.MODID)
public final class ChatHistory {

    private record Line(ChatChannel channel, Component message, @Nullable UUID sender, String senderName, long time) {}

    private static final Deque<Line> LINES = new ArrayDeque<>();

    private ChatHistory() {}

    /** A public line as everyone got it; {@code sender} is null for lines nobody in game wrote (Discord). */
    public static synchronized void add(ChatChannel channel, Component message, @Nullable ServerPlayer sender) {
        int keep = keep();
        if (keep <= 0 || minutes() <= 0) {
            LINES.clear();
            return;
        }
        // The Discord bridge's line passes through every player's system messages: kept once.
        Line last = LINES.peekLast();
        if (sender == null && last != null && last.sender() == null
                && System.currentTimeMillis() - last.time() < 2_000
                && last.message().getString().equals(message.getString())) {
            return;
        }
        LINES.addLast(new Line(channel, message, sender == null ? null : sender.getUUID(),
                sender == null ? "" : sender.getGameProfile().getName(), System.currentTimeMillis()));
        while (LINES.size() > keep) LINES.removeFirst();
    }

    private static synchronized List<Line> recent() {
        long oldest = System.currentTimeMillis() - minutes() * 60_000L;
        return LINES.stream().filter(line -> line.time() >= oldest).toList();
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || keep() <= 0 || minutes() <= 0) return;
        List<Line> recent = recent();
        if (recent.isEmpty()) return;
        ChatChannels.deliver(player, ChatChannel.GLOBAL, Component.translatable("msg.roll_mod.chat.history",
                recent.size(), minutes()).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        for (Line line : recent) {
            ChatChannels.deliver(player, line.channel(), line.message(), line.sender(), line.senderName(), line.time());
        }
        ChatChannels.deliver(player, ChatChannel.GLOBAL, Component.translatable("msg.roll_mod.chat.history.end")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (ChatHistory.class) {
            LINES.clear();
        }
    }

    private static int keep() {
        return ModConfigs.MAIN == null ? 0 : ModConfigs.MAIN.chat_changes.joinHistoryLines.get();
    }

    private static int minutes() {
        return ModConfigs.MAIN == null ? 0 : ModConfigs.MAIN.chat_changes.joinHistoryMinutes.get();
    }
}
