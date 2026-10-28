package com.roll_54.roll_mod.minestar.chat.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatButton;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.hub.gui.HubBadge;
import com.roll_54.roll_mod.minestar.chat.ChatChannelState;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The channel buttons over the chat box, and what they do to the message being sent.
 *
 * <p>One small button per visible channel, in the order set in the hub's Settings tab, in a row just
 * above the input line. A built-in button switches both the view and where messages go; a custom
 * one where they go, and the view to All. The button messages go through is drawn yellow, the one
 * being viewed (All, when a custom one is sending) green.
 *
 * <p>Sending rewrites the typed text for the chosen button — see {@link ChatButton#outgoing}. With
 * the Settings switch on, a message starting with {@code !} goes to Global instead, without the
 * {@code !}. Typed
 * commands never reach {@link ClientChatEvent}, so they are left alone. A rewrite that turns into a
 * command (Friends' {@code /fc}, or a custom {@code /msg …}) is sent as one here and the chat
 * message itself cancelled: the event fires after vanilla has already decided this is chat.
 *
 * <p>Ctrl + left Alt, pressed and released on its own while the chat is open, moves to the next
 * button — the same as clicking it. Hardcoded rather than a key mapping, since a mapping cannot be
 * just two modifiers. Only the left Alt counts: Windows sends AltGr as left Ctrl + right Alt, so a
 * tap of AltGr on European layouts must not switch channels.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class ChatButtonsHandler {

    private static final int BUTTON_H = 12;
    /** {@code exclamation_mark.png} is 7x7, drawn at its own size. */
    private static final int BADGE = 7;
    private static final int BADGE_OVERHANG = 2;
    private static final int GAP = 2;
    /** Above the input line, which sits 14px from the bottom of the screen and is 12px tall. */
    private static final int BOTTOM_OFFSET = 14 + BUTTON_H + 2;

    private ChatButtonsHandler() {}

    /**
     * Ctrl and left Alt are both down with nothing else pressed since. Switching waits for the release
     * so AltGr characters and other Ctrl+Alt shortcuts still work: their third key disarms it.
     */
    private static boolean cycleArmed;

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ChatScreen screen)) return;
        Font font = Minecraft.getInstance().font;
        List<ChannelButton> buttons = new ArrayList<>();
        int x = 2;
        int y = screen.height - BOTTOM_OFFSET;
        for (ChatButton channel : ChatChannelState.get().visible()) {
            int width = font.width(channel.title()) + 10;
            ChannelButton button = new ChannelButton(channel, x, y, width, screen, buttons);
            buttons.add(button);
            event.addListener(button);
            x += width + GAP;
        }
        buttons.forEach(ChannelButton::restyle);
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;
        int key = event.getKeyCode();
        // Read from the key state, so it is right whichever of the two went down first.
        cycleArmed = (isControl(key) || key == GLFW.GLFW_KEY_LEFT_ALT) && Screen.hasControlDown()
                && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_ALT);
    }

    @SubscribeEvent
    public static void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen screen)) return;
        int key = event.getKeyCode();
        if (!cycleArmed || !(isControl(key) || key == GLFW.GLFW_KEY_LEFT_ALT)) return;
        cycleArmed = false;

        ChatChannelState state = ChatChannelState.get();
        List<ChatButton> visible = state.visible();
        if (visible.size() < 2) return;
        String current = state.send().id();
        int index = 0;
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).id().equals(current)) {
                index = i;
                break;
            }
        }
        state.pick(visible.get((index + 1) % visible.size()));
        ClientChatStore.refresh();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof ChannelButton button) button.restyle();
        }
    }

    private static boolean isControl(int key) {
        return key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL;
    }

    /**
     * The typed text, sent as the command of the button messages go through. The server locks plain
     * chat, so every message leaves as a command; a command typed by hand is left alone.
     */
    @SubscribeEvent
    public static void onSend(ClientChatEvent event) {
        String text = event.getMessage();
        if (text.isBlank() || text.startsWith("/")) return;
        ChatChannelState state = ChatChannelState.get();
        event.setCanceled(true);
        String outgoing;
        if (state.bangGlobal() && text.startsWith(ChatButton.GLOBAL_PREFIX)) {
            String stripped = ChatButton.withoutGlobalPrefix(text);
            // Nothing but "!": nothing to say.
            if (stripped.isEmpty()) return;
            outgoing = ChatButton.GLOBAL_BUTTON.outgoing(stripped);
        } else {
            outgoing = state.send().outgoing(text);
        }
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.sendCommand(outgoing.substring(1));
    }

    /** One channel button. Keeps its siblings so a click can restyle the whole row. */
    private static final class ChannelButton extends Button {

        private final ChatButton channel;

        ChannelButton(ChatButton channel, int x, int y, int width, Screen screen, List<ChannelButton> row) {
            super(x, y, width, BUTTON_H, channel.title(), b -> {
                ChatChannelState.get().pick(channel);
                // Every pick can change what is shown: a custom button switches the view to All.
                ClientChatStore.refresh();
                row.forEach(ChannelButton::restyle);
                // Deferred: vanilla hands focus to the clicked widget *after* onPress returns, so a
                // refocus here was undone at once and every keystroke went to the button — typing
                // was locked. Queued, it runs once the click is fully handled.
                Minecraft.getInstance().tell(() -> refocusInput(screen));
            }, DEFAULT_NARRATION);
            this.channel = channel;
            setTooltip(Tooltip.create(tooltip(channel)));
        }

        /** Never holds keyboard focus: the chat input must keep it, and Tab must not stop here. */
        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        /**
         * Never a stop for keyboard navigation. The row sits just above the input, so the arrow-up
         * that recalls history was also an UP navigation for vanilla, which moved the screen's focus
         * onto a button: the input lost focus and Enter no longer sent the recalled text.
         */
        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
            return null;
        }

        /**
         * Never takes a key. The click still makes this the screen's focused child until the
         * deferred refocus lands, and the screen hands keys to its focused child before ChatScreen
         * looks at them — so a Button's own Enter/Space handling pressed it again and swallowed the
         * Enter meant to send, which took a second press. Clicks are the only way to use it.
         */
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return false;
        }

        /**
         * Drawn in the hub's theme (the one picked in Settings) rather than as a vanilla button:
         * the theme's control colour, its hover colour, a hairline, and the accent under the button
         * messages go through. The vanilla-looking themes keep the vanilla button.
         */
        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            UiTheme.Palette palette = UiThemeState.get().palette();
            if (palette == null) {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
                badge(graphics);
                return;
            }
            ChatChannelState state = ChatChannelState.get();
            boolean sending = state.send().id().equals(channel.id());
            boolean viewing = state.view().id().equals(channel.id());
            int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;

            graphics.fill(x0, y0, x1, y1, isHovered() ? palette.hovered() : palette.raised());
            // Hairline: top, bottom, left, right.
            graphics.fill(x0, y0, x1, y0 + 1, palette.line());
            graphics.fill(x0, y1 - 1, x1, y1, palette.line());
            graphics.fill(x0, y0, x0 + 1, y1, palette.line());
            graphics.fill(x1 - 1, y0, x1, y1, palette.line());
            if (sending) graphics.fill(x0 + 1, y1 - 2, x1 - 1, y1, palette.accent());

            int color = sending ? palette.accent() : viewing ? palette.text() : palette.dim();
            Font font = Minecraft.getInstance().font;
            Component caption = channel.title();
            graphics.drawString(font, caption, x0 + (width - font.width(caption)) / 2,
                    y0 + (height - font.lineHeight) / 2 + 1, color, false);
            badge(graphics);
        }

        /**
         * The hub's exclamation mark on the top-right corner, overhanging it, while this channel has
         * lines the view has not shown. Only real channels: All shows everything, and a custom
         * button views All.
         */
        private void badge(GuiGraphics graphics) {
            ChatChannel shown = channel.channel();
            if (shown == null || !ClientChatStore.unread(shown)) return;
            graphics.pose().pushPose();
            // Over the caption and the frame, not under them.
            graphics.pose().translate(0, 0, 10);
            graphics.blit(HubBadge.TEXTURE, getX() + width - BADGE + BADGE_OVERHANG, getY() - BADGE_OVERHANG,
                    0, 0, BADGE, BADGE, BADGE, BADGE);
            graphics.pose().popPose();
        }

        void restyle() {
            ChatChannelState state = ChatChannelState.get();
            MutableComponent caption = channel.title().copy();
            if (state.send().id().equals(channel.id())) {
                caption.withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
            } else if (state.view().id().equals(channel.id())) {
                caption.withStyle(ChatFormatting.GREEN);
            } else {
                caption.withStyle(ChatFormatting.GRAY);
            }
            setMessage(caption);
        }

        private static Component tooltip(ChatButton channel) {
            if (channel.custom()) {
                return Component.translatable("gui.roll_mod.chat.button.custom", channel.template());
            }
            return Component.translatable("gui.roll_mod.chat.button." + channel.id());
        }

        /** Clicking a button takes the focus off the input; give it back so typing carries on. */
        private static void refocusInput(Screen screen) {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof EditBox box) {
                    screen.setFocused(box);
                    box.setFocused(true);
                    return;
                }
            }
        }
    }
}
