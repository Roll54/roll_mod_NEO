package com.roll_54.roll_mod.minestar.chat.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannelState;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The PM tab's list of who to write to: every other player online, with their head, over the chat
 * tabs. Shown while the PM tab is open and either nobody is picked yet or the input is an
 * unfinished {@code @name} (it then narrows to the names that match). A click picks the player; the
 * tab writes to them until another is picked, typed as {@code @name}, or writes first.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class PmPicker {

    private static final int ENTRY_H = 12;
    private static final int GAP = 2;
    private static final int PAD = 3;
    private static final int MAX_ROWS = 6;
    private static final int TITLE_COLOR = 0xFFFF55FF;
    private static final int TEXT_COLOR = 0xFFAAAAAA;

    /** The entries of the chat screen now open, and the panel they sit on. */
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static int panelTop, panelBottom, panelRight;

    private PmPicker() {}

    /** Builds the list for a chat screen being opened; {@code bottom} is the top of the tab rows. */
    static void attach(Screen screen, Consumer<GuiEventListener> add, int bottom) {
        ENTRIES.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return;
        Font font = mc.font;

        List<PlayerInfo> players = new ArrayList<>(mc.getConnection().getOnlinePlayers());
        players.removeIf(info -> info.getProfile().getId().equals(mc.player.getUUID()));
        players.sort(Comparator.comparing(info -> info.getProfile().getName().toLowerCase(Locale.ROOT)));

        // Rows, left to right, wrapping at the screen edge; laid out top-down under the two text lines.
        int maxX = screen.width - 4;
        List<List<Entry>> rows = new ArrayList<>();
        List<Entry> row = new ArrayList<>();
        int x = 2 + PAD;
        for (PlayerInfo info : players) {
            Component label = label(info);
            int width = 8 + 4 + font.width(label) + 6;
            if (x + width > maxX && !row.isEmpty()) {
                rows.add(row);
                if (rows.size() == MAX_ROWS) break;
                row = new ArrayList<>();
                x = 2 + PAD;
            }
            row.add(new Entry(info, label, x, 0, width, screen));
            x += width + GAP;
        }
        if (!row.isEmpty() && rows.size() < MAX_ROWS) rows.add(row);

        int textLines = 2;
        int height = PAD + textLines * (font.lineHeight + 1) + rows.size() * (ENTRY_H + GAP) + PAD;
        panelBottom = bottom;
        panelTop = bottom - height;
        panelRight = 2;
        int y = panelTop + PAD + textLines * (font.lineHeight + 1);
        for (List<Entry> entries : rows) {
            for (Entry entry : entries) {
                entry.setY(y);
                panelRight = Math.max(panelRight, entry.getX() + entry.getWidth() + PAD);
                ENTRIES.add(entry);
                add.accept(entry);
            }
            y += ENTRY_H + GAP;
        }
        panelRight = Math.max(panelRight, 2 + PAD + font.width(Component.translatable("gui.roll_mod.chat.pm.help")) + PAD);
    }

    /** The tab-list name the server gave the player (with its rank), or just their name. */
    private static Component label(PlayerInfo info) {
        Component display = info.getTabListDisplayName();
        return display != null ? display : Component.literal(info.getProfile().getName());
    }

    /** Whether the list is up: PM tab, and nobody picked or an unfinished {@code @name} typed. */
    private static boolean shown(Screen screen) {
        if (ChatChannelState.get().send().channel() != ChatChannel.PM) return false;
        if (ClientChatStore.pmPartner() == null) return true;
        String typed = typed(screen);
        return typed.startsWith("@") && !typed.contains(" ");
    }

    private static String typed(Screen screen) {
        EditBox input = ChatButtonsHandler.input(screen);
        return input == null ? "" : input.getValue();
    }

    /** Shows or hides the entries for this frame and draws the panel under them. */
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen screen) || ENTRIES.isEmpty() && !shown(screen)) return;
        boolean shown = shown(screen);
        String typed = typed(screen);
        String filter = typed.startsWith("@") ? typed.substring(1).toLowerCase(Locale.ROOT) : "";
        for (Entry entry : ENTRIES) {
            entry.visible = shown && entry.name().toLowerCase(Locale.ROOT).startsWith(filter);
        }
        if (!shown) return;

        GuiGraphics graphics = event.getGuiGraphics();
        UiTheme.Palette palette = UiThemeState.get().palette();
        graphics.fill(2, panelTop, panelRight, panelBottom, palette != null ? (palette.raised() & 0x00FFFFFF) | 0xCC000000 : 0xCC000000);
        Font font = Minecraft.getInstance().font;
        int y = panelTop + PAD;
        graphics.drawString(font, Component.translatable("gui.roll_mod.chat.pm.help"), 2 + PAD, y, TEXT_COLOR, false);
        y += font.lineHeight + 1;
        Component title = ENTRIES.isEmpty()
                ? Component.translatable("gui.roll_mod.chat.pm.nobody")
                : Component.translatable("gui.roll_mod.chat.pm.pick");
        graphics.drawString(font, title, 2 + PAD, y, TITLE_COLOR, false);
    }

    /** One player in the list. */
    private static final class Entry extends AbstractButton {

        private final PlayerInfo info;
        private final Screen screen;

        Entry(PlayerInfo info, Component label, int x, int y, int width, Screen screen) {
            super(x, y, width, ENTRY_H, label);
            this.info = info;
            this.screen = screen;
            this.visible = false;
        }

        String name() {
            return info.getProfile().getName();
        }

        @Override
        public void onPress() {
            ClientChatStore.setPmPartner(name());
            EditBox input = ChatButtonsHandler.input(screen);
            if (input != null && input.getValue().startsWith("@")) input.setValue("");
            ChatButtonsHandler.updateHint(screen);
            // Same reason as the channel buttons: give the input its focus back after the click.
            Minecraft.getInstance().tell(() -> {
                if (input != null) {
                    screen.setFocused(input);
                    input.setFocused(true);
                }
            });
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x0 = getX(), y0 = getY(), x1 = x0 + width, y1 = y0 + height;
            graphics.fill(x0, y0, x1, y1, isHovered() ? 0x60FFFFFF : 0x30FFFFFF);
            PlayerFaceRenderer.draw(graphics, info.getSkin(), x0 + 2, y0 + 2, 8);
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, getMessage().copy().withStyle(style -> style.getColor() == null
                    ? style.withColor(ChatFormatting.WHITE) : style), x0 + 14, y0 + 2, 0xFFFFFFFF, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }

        /** Like the channel buttons: never takes focus, keys or keyboard navigation from the input. */
        @Override
        public void setFocused(boolean focused) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
            return null;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return false;
        }
    }
}
