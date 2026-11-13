package com.roll_54.roll_mod.minestar.chat.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatButton;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannelState;
import com.roll_54.roll_mod.mixin.client.ChatComponentAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The message toolbar, replying and mentioning in the chat box, ported from Eden_LD:
 * <ul>
 *   <li><b>Toolbar</b>: over a message in the open chat, a small bar at the right end of the hovered line with
 *       when it was sent ("14:35" today, "11.10 14:35" before), ↩ to reply (someone else's message only) and
 *       ⧉ to copy its text.</li>
 *   <li><b>Reply</b>: ↩ or a right click on a player's message switches to its channel and puts "@Name " in the
 *       input, so they get the ping ({@code ChatMentions}). On a private message it makes the writer the one the
 *       PM tab writes to instead.</li>
 *   <li><b>Tab after "@"</b>: completes the online player names starting with what is typed; Tab again cycles
 *       through them.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class ChatReply {

    /** One toolbar button's width. */
    private static final int TOOL = 11;
    private static final DateTimeFormatter TODAY = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter EARLIER = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    /** The completion in progress: the text before the "@", the matches, and the one shown. */
    @Nullable private static String base;
    private static List<String> matches = List.of();
    private static int index;
    @Nullable private static String completed;

    private ChatReply() {}

    /* ------------------------------------------------ toolbar ------------------------------------------------- */

    private enum Tool { REPLY, COPY }

    /** The bar over the hovered line: where it is and what it holds. */
    private record Bar(ClientChatStore.Written written, String time, List<Tool> tools, int left, int toolsLeft,
                       int top, int bottom) {

        @Nullable
        Tool toolAt(double x, double y) {
            if (y < top || y >= bottom || x < toolsLeft) return null;
            int i = (int) ((x - toolsLeft) / TOOL);
            return i >= 0 && i < tools.size() ? tools.get(i) : null;
        }

        boolean contains(double x, double y) {
            return x >= left && x < toolsLeft + tools.size() * TOOL && y >= top && y < bottom;
        }
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;
        Bar bar = barAt(event.getMouseX(), event.getMouseY());
        if (bar == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int right = bar.toolsLeft() + bar.tools().size() * TOOL;
        int textY = bar.top() + (bar.bottom() - bar.top() - 8) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        graphics.fill(bar.left(), bar.top(), right, bar.bottom(), 0xE0262626);
        graphics.drawString(font, bar.time(), bar.left() + 3, textY, 0xFF9A9A9A, false);
        Tool over = bar.toolAt(event.getMouseX(), event.getMouseY());
        int x = bar.toolsLeft();
        for (Tool tool : bar.tools()) {
            String symbol = tool == Tool.REPLY ? "↩" : "⧉";
            if (tool == over) graphics.fill(x, bar.top(), x + TOOL, bar.bottom(), 0x40FFFFFF);
            graphics.drawString(font, symbol, x + (TOOL - font.width(symbol)) / 2 + 1, textY,
                    tool == over ? 0xFFFFFFFF : 0xFFC0C0C0, false);
            x += TOOL;
        }
        graphics.pose().popPose();
        if (over != null) {
            graphics.renderTooltip(font, Component.translatable(over == Tool.REPLY
                    ? "gui.roll_mod.chat.reply" : "gui.roll_mod.chat.copy"), event.getMouseX(), event.getMouseY());
        }
    }

    /** The toolbar of the line under the mouse in the open chat, or null. */
    @Nullable
    private static Bar barAt(double mouseX, double mouseY) {
        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        ClientChatStore.Written written = messageAt(mouseX, mouseY);
        if (written == null) return null;
        ChatComponentAccessor access = (ChatComponentAccessor) chat;
        // Where vanilla draws the hovered line (ChatComponent.render): rows of getLineHeight() up from 40px
        // over the bottom, scaled by the chat scale, and k + 8 wide after a 4px shift.
        double scale = chat.getScale();
        int lineHeight = access.roll_mod$getLineHeight();
        int row = Mth.floor(access.roll_mod$screenToChatY(mouseY));
        int base = Mth.floor((Minecraft.getInstance().getWindow().getGuiScaledHeight() - 40) / (float) scale);
        int bottom = (int) Math.round((base - row * lineHeight) * scale);
        int top = Math.min(bottom - 9, (int) Math.round((base - (row + 1) * lineHeight) * scale));
        int right = (int) Math.round((Mth.ceil(chat.getWidth() / (float) scale) + 12) * scale);

        List<Tool> tools = new ArrayList<>();
        if (fromOther(written)) tools.add(Tool.REPLY);
        tools.add(Tool.COPY);
        String time = time(written.time());
        int toolsLeft = right - tools.size() * TOOL;
        int left = toolsLeft - Minecraft.getInstance().font.width(time) - 6;
        return new Bar(written, time, tools, left, toolsLeft, top, bottom);
    }

    /** "14:35" today, "11.10 14:35" before. */
    private static String time(long millis) {
        ZonedDateTime when = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault());
        return when.format(when.toLocalDate().equals(LocalDate.now()) ? TODAY : EARLIER);
    }

    /** A player's message, not the player's own. */
    private static boolean fromOther(ClientChatStore.Written written) {
        if (written.sender().isEmpty()) return false;
        var player = Minecraft.getInstance().player;
        return player == null || !written.sender().equalsIgnoreCase(player.getGameProfile().getName());
    }

    @SubscribeEvent
    public static void onMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen screen)) return;
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Bar bar = barAt(mouseX, mouseY);
            if (bar == null || !bar.contains(mouseX, mouseY)) return;
            event.setCanceled(true);
            Tool tool = bar.toolAt(mouseX, mouseY);
            if (tool == Tool.REPLY) reply(screen, bar.written());
            else if (tool == Tool.COPY) copy(bar.written());
        } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ClientChatStore.Written written = messageAt(mouseX, mouseY);
            if (written == null || !fromOther(written)) return;
            event.setCanceled(true);
            reply(screen, written);
        }
    }

    /** The message under the mouse in the open chat, or null. */
    @Nullable
    private static ClientChatStore.Written messageAt(double mouseX, double mouseY) {
        ChatComponentAccessor chat = (ChatComponentAccessor) Minecraft.getInstance().gui.getChat();
        int end = chat.roll_mod$getMessageEndIndexAt(chat.roll_mod$screenToChatX(mouseX), chat.roll_mod$screenToChatY(mouseY));
        if (end < 0) return null;
        // Wrapped lines go in newest first, each message's last line first: counting those last lines up to
        // the one under the mouse gives the message's place among all messages (also newest first).
        List<GuiMessage.Line> lines = chat.roll_mod$trimmedMessages();
        int message = -1;
        for (int i = 0; i <= end && i < lines.size(); i++) {
            if (lines.get(i).endOfEntry()) message++;
        }
        List<GuiMessage> messages = chat.roll_mod$allMessages();
        if (message < 0 || message >= messages.size()) return null;
        return ClientChatStore.writtenBy(messages.get(message).content());
    }

    private static void copy(ClientChatStore.Written written) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.keyboardHandler.setClipboard(written.text());
        minecraft.gui.setOverlayMessage(Component.translatable("gui.roll_mod.chat.copied"), false);
    }

    private static void reply(ChatScreen screen, ClientChatStore.Written written) {
        ChatChannelState state = ChatChannelState.get();
        ChatButton button = buttonOf(state, written.channel());
        if (button != null && !button.id().equals(state.send().id())) {
            state.pick(button);
            ClientChatStore.refresh();
            ChatButtonsHandler.afterPick(screen);
        }
        EditBox input = ChatButtonsHandler.input(screen);
        if (written.channel() == ChatChannel.PM) {
            // The PM tab writes to its partner; "@Name" there would only pick them.
            ClientChatStore.setPmPartner(written.sender());
            ChatButtonsHandler.afterPick(screen);
        } else if (input != null) {
            String mention = "@" + written.sender() + " ";
            String text = input.getValue();
            if (!text.startsWith(mention)) input.setValue(mention + text);
            input.moveCursorToEnd(false);
        }
        if (input != null) screen.setFocused(input);
    }

    /** The shown button of {@code channel}, or null when the player has none (the message is then answered in the current one). */
    @Nullable
    private static ChatButton buttonOf(ChatChannelState state, ChatChannel channel) {
        for (ChatButton button : state.visible()) {
            if (!button.custom() && button.channel() == channel && !channel.readOnly()) return button;
        }
        return null;
    }

    /* ------------------------------------------------ mention ------------------------------------------------- */

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        if (event.getKeyCode() != GLFW.GLFW_KEY_TAB || !(event.getScreen() instanceof ChatScreen screen)) return;
        EditBox input = ChatButtonsHandler.input(screen);
        if (input == null || input.getValue().startsWith("/")) return;
        if (complete(input)) event.setCanceled(true);
    }

    /** Completes the "@part" at the end of the input; false when there is none, so vanilla's Tab runs. */
    private static boolean complete(EditBox input) {
        String text = input.getValue();
        if (completed == null || !text.equals(completed)) {
            int at = text.lastIndexOf('@');
            if (at < 0 || text.substring(at).contains(" ") || (at > 0 && !Character.isWhitespace(text.charAt(at - 1)))) {
                completed = null;
                return false;
            }
            String part = text.substring(at + 1).toLowerCase(Locale.ROOT);
            matches = new ArrayList<>();
            for (String name : onlineNames()) {
                if (name.toLowerCase(Locale.ROOT).startsWith(part)) matches.add(name);
            }
            if (matches.isEmpty()) return true;
            matches.sort(String.CASE_INSENSITIVE_ORDER);
            base = text.substring(0, at);
            index = 0;
        } else {
            index = (index + 1) % matches.size();
        }
        input.setValue(base + "@" + matches.get(index) + " ");
        input.moveCursorToEnd(false);
        completed = input.getValue();
        return true;
    }

    private static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return names;
        String self = Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getGameProfile().getName();
        for (PlayerInfo info : connection.getOnlinePlayers()) {
            String name = info.getProfile().getName();
            if (!name.equals(self)) names.add(name);
        }
        return names;
    }
}
