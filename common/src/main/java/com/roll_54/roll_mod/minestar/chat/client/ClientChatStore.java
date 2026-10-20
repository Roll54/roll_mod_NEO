package com.roll_54.roll_mod.minestar.chat.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatButton;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannelState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;

/**
 * Every chat line this session, with the channel it came on, so the chat box can show one channel at
 * a time and switch back without losing anything.
 *
 * <p>Channel lines arrive as {@code ChatChannelPacket}s and are added to the vanilla chat here only
 * when the current view shows them. Everything else — the Discord bridge, advancements, storm
 * warnings, command output — arrives the vanilla way, is kept as "other" (no channel), and is
 * cancelled from the chat unless the view is All.
 *
 * <p>Switching view clears the vanilla chat (keeping the sent-message history) and refills it from
 * here, oldest first. The vanilla chat keeps 100 lines; this keeps {@link #CAPACITY}, so a quiet
 * channel still has its history after a busy one scrolled it out of the vanilla list.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class ClientChatStore {

    private static final int CAPACITY = 500;

    private record Line(@Nullable ChatChannel channel, Component message) {}

    private static final Deque<Line> LINES = new ArrayDeque<>();

    /** Channels that got a line while the view did not show them — what the buttons badge. */
    private static final EnumSet<ChatChannel> UNREAD = EnumSet.noneOf(ChatChannel.class);

    private ClientChatStore() {}

    /** A channel line from the server. */
    public static void receive(ChatChannel channel, Component message) {
        remember(channel, message);
        if (shows(channel)) {
            chat().addMessage(message);
        } else {
            UNREAD.add(channel);
        }
    }

    /** Whether {@code channel} has lines the player has not had on screen yet. */
    public static boolean unread(ChatChannel channel) {
        return UNREAD.contains(channel);
    }

    /**
     * Everything that is not a channel line. At {@code LOWEST}, so the stored copy carries whatever
     * other mods changed, and not at all when another listener already cancelled it.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onReceived(ClientChatReceivedEvent event) {
        // The action bar is not chat.
        if (event instanceof ClientChatReceivedEvent.System system && system.isOverlay()) return;
        remember(null, event.getMessage());
        if (!shows(null)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LINES.clear();
        UNREAD.clear();
    }

    /** Refills the vanilla chat with what the current view shows — after a channel button is clicked. */
    public static void refresh() {
        // The view now on screen has been read: All shows every channel, a channel view its own.
        ChatButton view = ChatChannelState.get().view();
        if (ChatButton.ALL.equals(view.id())) {
            UNREAD.clear();
        } else if (view.channel() != null) {
            UNREAD.remove(view.channel());
        }
        ChatComponent chat = chat();
        chat.clearMessages(false);
        for (Line line : LINES) {
            if (shows(line.channel())) chat.addMessage(line.message());
        }
    }

    /** Whether the current view shows a line of {@code channel} ({@code null} = other). */
    private static boolean shows(@Nullable ChatChannel channel) {
        ChatButton view = ChatChannelState.get().view();
        if (ChatButton.ALL.equals(view.id())) return true;
        return channel != null && channel == view.channel();
    }

    private static void remember(@Nullable ChatChannel channel, Component message) {
        LINES.addLast(new Line(channel, message));
        while (LINES.size() > CAPACITY) LINES.removeFirst();
    }

    private static ChatComponent chat() {
        return Minecraft.getInstance().gui.getChat();
    }
}
