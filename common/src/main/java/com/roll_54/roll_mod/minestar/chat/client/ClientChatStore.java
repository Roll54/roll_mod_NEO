package com.roll_54.roll_mod.minestar.chat.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatButton;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannelState;
import com.roll_54.roll_mod.minestar.chat.emoji.EmojiRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Every chat line this session, with the channel it came on, so the chat box can show one channel at
 * a time and switch back without losing anything.
 *
 * <p>Channel lines arrive as {@code ChatChannelPacket}s. Everything else arrives the vanilla way and
 * is sorted here ({@link #classify}): advancements, deaths, joins and other notices to Spam,
 * vanilla {@code /msg} to PM, Discord lines and other mods' player chat to Global. A line
 * is added to the vanilla chat only when the current view shows it; otherwise its channel is marked
 * unread.
 *
 * <p><b>Repeats</b>: the same text again on the same channel, as that channel's latest line, does not
 * add a line — the earlier one gets a gray {@code (N)} and moves down to the newest place, so spam
 * takes one line however long it goes on.
 *
 * <p>Switching view clears the vanilla chat (keeping the sent-message history) and refills it from
 * here, oldest first. The vanilla chat keeps 100 lines; this keeps {@link #CAPACITY}, so a quiet
 * channel still has its history after a busy one scrolled it out of the vanilla list.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class ClientChatStore {

    private static final int CAPACITY = 500;

    private static final class Line {
        final ChatChannel channel;
        final Component message;
        final String text;
        int count = 1;

        Line(ChatChannel channel, Component message) {
            this.channel = channel;
            this.message = message;
            this.text = message.getString();
        }

        /** The line as shown: with its repeat count once it has one. */
        Component shown() {
            if (count == 1) return message;
            return message.copy().append(Component.literal(" (" + count + ")").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Oldest first. A list rather than a deque: a repeat moves its line from the middle to the end. */
    private static final List<Line> LINES = new ArrayList<>();

    /** Channels that got a line while the view did not show them — what the buttons badge. */
    private static final EnumSet<ChatChannel> UNREAD = EnumSet.noneOf(ChatChannel.class);

    /** Whether the server gave this player the DEV tab (see {@code ChatAccessPacket}). */
    private static boolean devAccess;

    /** Who the PM tab writes to: the last conversation, a pick from its list, or an {@code @name}. */
    @Nullable
    private static String pmPartner;

    private ClientChatStore() {}

    /* ------------------------------------------------ incoming ------------------------------------------------ */

    /** A channel line from the server; {@code partner} names the other side of a PM line. */
    public static void receive(ChatChannel channel, Component message, String partner) {
        if (channel == ChatChannel.PM && !partner.isEmpty()) pmPartner = partner;
        add(channel, message);
    }

    /**
     * Everything that is not a channel line. At {@code LOWEST}, so the stored copy carries whatever
     * other mods changed, and not at all when another listener already cancelled it. Always cancelled:
     * the line reaches the vanilla chat through {@link #add}, like the channel lines.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onReceived(ClientChatReceivedEvent event) {
        // The action bar is not chat.
        if (event instanceof ClientChatReceivedEvent.System system && system.isOverlay()) return;
        Component message = event.getMessage();
        ChatChannel channel = classify(message, event instanceof ClientChatReceivedEvent.Player);
        event.setCanceled(true);
        add(channel, message);
    }

    /**
     * Which tab an untagged line belongs in. By translation key where vanilla has one, so it works in
     * every language; Discord lines by the bridge's {@code [Discord]} tag.
     */
    static ChatChannel classify(Component message, boolean playerChat) {
        String plain = message.getString().stripLeading().toLowerCase(Locale.ROOT);
        // The server locks plain chat, so player chat that still arrives is a bridge or another mod's.
        if (playerChat || plain.startsWith("[discord") || plain.startsWith("[дискорд") || plain.startsWith("[ds]")) {
            return ChatChannel.GLOBAL;
        }
        if (message.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if (key.startsWith("commands.message.display.")) {
                Object[] args = translatable.getArgs();
                if (args.length > 0) {
                    String name = args[0] instanceof Component c ? c.getString() : String.valueOf(args[0]);
                    if (!name.isBlank()) pmPartner = name;
                }
                return ChatChannel.PM;
            }
            // "[Name: did something]", the echo of other operators' commands.
            if (key.equals("chat.type.admin") && devAccess) return ChatChannel.DEV;
        }
        // Advancements, deaths, joins and leaves, announcements and everything else.
        return ChatChannel.SPAM;
    }

    /**
     * Links in a line nobody made clickable — vanilla chat in singleplayer, a bridge's plain text —
     * become links here, GIFs the {@code [GIF]} that opens {@link GifViewer}. Lines the server already
     * rendered (they carry click events) are left as they are.
     */
    private static Component decorate(Component message) {
        if (!message.getString().contains("http") || hasClick(message)) return message;
        return EmojiRegistry.render(message);
    }

    private static boolean hasClick(Component component) {
        if (component.getStyle().getClickEvent() != null) return true;
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component argument && hasClick(argument)) return true;
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (hasClick(sibling)) return true;
        }
        return false;
    }

    private static void add(ChatChannel channel, Component raw) {
        Component message = decorate(raw);
        Line repeated = latest(channel);
        Line line;
        if (repeated != null && repeated.text.equals(message.getString())) {
            // Spam: one line, counted, brought back down to the newest place.
            LINES.remove(repeated);
            repeated.count++;
            LINES.add(repeated);
            line = repeated;
        } else {
            line = new Line(channel, message);
            LINES.add(line);
            while (LINES.size() > CAPACITY) LINES.removeFirst();
        }

        if (!shows(channel)) {
            UNREAD.add(channel);
        } else if (line.count > 1) {
            // The vanilla chat cannot change a line in place; rebuild it from here.
            rebuild();
        } else {
            show(chat(), line.shown());
        }
    }

    @Nullable
    private static Line latest(ChatChannel channel) {
        for (int i = LINES.size() - 1; i >= 0; i--) {
            if (LINES.get(i).channel == channel) return LINES.get(i);
        }
        return null;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LINES.clear();
        UNREAD.clear();
        devAccess = false;
        pmPartner = null;
    }

    /* ------------------------------------------------- state -------------------------------------------------- */

    /** Whether {@code channel} has lines the player has not had on screen yet. */
    public static boolean unread(ChatChannel channel) {
        return UNREAD.contains(channel);
    }

    public static boolean devAccess() {
        return devAccess;
    }

    public static void setDevAccess(boolean on) {
        devAccess = on;
    }

    @Nullable
    public static String pmPartner() {
        return pmPartner;
    }

    public static void setPmPartner(@Nullable String name) {
        pmPartner = name == null || name.isBlank() ? null : name;
    }

    /** Whether any private message has been seen this session — the PM tab shows its player list until then. */
    public static boolean hasPmLines() {
        return latest(ChatChannel.PM) != null;
    }

    /* ------------------------------------------------- views -------------------------------------------------- */

    /** Refills the vanilla chat with what the current view shows — after a channel button is clicked. */
    public static void refresh() {
        // The view now on screen has been read: All shows every channel, a channel view its own.
        ChatButton view = ChatChannelState.get().view();
        if (ChatButton.ALL.equals(view.id())) {
            UNREAD.clear();
        } else if (view.channel() != null) {
            UNREAD.remove(view.channel());
        }
        rebuild();
    }

    private static void rebuild() {
        ChatComponent chat = chat();
        chat.clearMessages(false);
        for (Line line : LINES) {
            if (shows(line.channel)) show(chat, line.shown());
        }
    }

    /** True while this class itself is putting a line in the vanilla chat. */
    private static boolean showing;

    private static void show(ChatComponent chat, Component message) {
        showing = true;
        try {
            chat.addMessage(message);
        } finally {
            showing = false;
        }
    }

    /**
     * A line some mod put straight into the chat window rather than sending it as a message —
     * update notices, greetings, client-side tips ({@code ChatComponentStoreMixin}). Sorted like any
     * other untagged line, so it lands in its tab (usually Spam). False when it is this class's own
     * line, which the chat window should take as it is.
     */
    public static boolean intercept(Component message) {
        if (showing) return false;
        add(classify(message, false), message);
        return true;
    }

    /** Whether the current view shows a line of {@code channel}. */
    private static boolean shows(ChatChannel channel) {
        ChatButton view = ChatChannelState.get().view();
        if (ChatButton.ALL.equals(view.id())) return true;
        return channel == view.channel();
    }

    private static ChatComponent chat() {
        return Minecraft.getInstance().gui.getChat();
    }
}
