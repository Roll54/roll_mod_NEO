package com.roll_54.roll_mod.minestar.chat.client;

import com.roll_54.roll_mod.minestar.chat.emoji.EmojiRegistry;
import com.roll_54.roll_mod.network.packet.EmojiPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The emoji button — a square of its own at the end of the chat input line, which is shortened to
 * make room ({@code ChatScreenEmojiBoxMixin}) — and the grid it opens above the chat tabs: every
 * Discord emoji the server has. A click puts the emoji into the chat line at the cursor — as its own
 * character, which every font here draws as the image and the server sends to Discord as the real
 * emoji — and closes the grid; Shift+click keeps it open to pick several. Hover shows its
 * {@code :name:}, which can be typed instead. The wheel scrolls a long list.
 */
@OnlyIn(Dist.CLIENT)
public final class EmojiPicker {

    /** The square's side: a little taller than the 12 px input line, so it reads as a box of its own. */
    private static final int BOX = 14;
    private static final int GAP = 2;
    /** How much of the input line's right end the square takes, gap included. */
    public static final int RESERVED = BOX + GAP;
    /** The emoji in the square, drawn this much larger than text. */
    private static final float ICON_SCALE = 1.25f;
    private static final int CELL = 12;
    private static final int COLUMNS = 12;
    private static final int ROWS = 8;
    private static final int PAD = 3;

    private static boolean open;

    private EmojiPicker() {}

    /** Adds the square (by the input line) and the grid (its bottom at {@code bottom}) to a chat screen being opened. */
    static void attach(Screen screen, Consumer<GuiEventListener> add, int bottom) {
        open = false;
        int width = COLUMNS * CELL + 2 * PAD;
        int height = ROWS * CELL + 2 * PAD;
        Grid grid = new Grid(screen, screen.width - 2 - width, bottom - height, width, height);
        add.accept(grid);
        // The input line's background runs from height - 14 to height - 2; the square is centred on it.
        add.accept(new Toggle(grid, screen.width - 2 - BOX, screen.height - 15));
    }

    private static Component glyph(EmojiPackets.EmojiRef ref) {
        return Component.literal(Character.toString(ref.codepoint())).withStyle(Style.EMPTY.withFont(EmojiRegistry.FONT));
    }

    /** The button: the first emoji of the list, or a smiley while none has loaded. */
    private static final class Toggle extends Quiet {

        private final Grid grid;

        Toggle(Grid grid, int x, int y) {
            super(x, y, BOX, BOX, Component.translatable("gui.roll_mod.chat.emoji.button"));
            this.grid = grid;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            open = !open;
            grid.visible = open;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // The input line's own background colour, so the square reads as part of that line.
            int background = Minecraft.getInstance().options.getBackgroundColor(Integer.MIN_VALUE);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, open || isHovered() ? 0x80FFFFFF : background);
            Font font = Minecraft.getInstance().font;
            List<EmojiPackets.EmojiRef> entries = EmojiFont.entries();
            Component icon = entries.isEmpty() ? Component.literal("☺") : glyph(entries.get(0));
            graphics.pose().pushPose();
            graphics.pose().translate(getX() + width / 2f, getY() + height / 2f, 0);
            graphics.pose().scale(ICON_SCALE, ICON_SCALE, 1);
            graphics.drawString(font, icon, -font.width(icon) / 2, -4, 0xFFFFFFFF, false);
            graphics.pose().popPose();
            if (isHovered()) {
                Screen screen = Minecraft.getInstance().screen;
                if (screen != null) screen.setTooltipForNextRenderPass(getMessage());
            }
        }
    }

    /** The grid of emojis, shown while the button is on. */
    private static final class Grid extends Quiet {

        private final Screen screen;
        private int scroll;

        Grid(Screen screen, int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
            this.visible = false;
            this.screen = screen;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            graphics.fill(getX(), getY(), getX() + width, getY() + height, 0xE0101010);
            Font font = Minecraft.getInstance().font;
            List<EmojiPackets.EmojiRef> entries = EmojiFont.entries();
            if (entries.isEmpty()) {
                graphics.drawString(font, Component.translatable("gui.roll_mod.chat.emoji.none"),
                        getX() + PAD, getY() + PAD, 0xFFAAAAAA, false);
            }
            EmojiPackets.EmojiRef hovered = at(mouseX, mouseY);
            for (int i = scroll * COLUMNS; i < entries.size() && i < (scroll + ROWS) * COLUMNS; i++) {
                int cell = i - scroll * COLUMNS;
                int cx = getX() + PAD + (cell % COLUMNS) * CELL;
                int cy = getY() + PAD + (cell / COLUMNS) * CELL;
                EmojiPackets.EmojiRef ref = entries.get(i);
                if (ref == hovered) graphics.fill(cx, cy, cx + CELL, cy + CELL, 0x60FFFFFF);
                graphics.drawString(font, glyph(ref), cx + 1, cy + 2, 0xFFFFFFFF, false);
            }
            graphics.pose().popPose();
            if (hovered != null) screen.setTooltipForNextRenderPass(Component.literal(":" + hovered.name() + ":"));
        }

        @Nullable
        private EmojiPackets.EmojiRef at(double mouseX, double mouseY) {
            int col = (int) Math.floor((mouseX - getX() - PAD) / CELL);
            int row = (int) Math.floor((mouseY - getY() - PAD) / CELL);
            if (col < 0 || col >= COLUMNS || row < 0 || row >= ROWS) return null;
            int index = (scroll + row) * COLUMNS + col;
            List<EmojiPackets.EmojiRef> entries = EmojiFont.entries();
            return index < entries.size() ? entries.get(index) : null;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            EmojiPackets.EmojiRef ref = at(mouseX, mouseY);
            if (ref == null) return;
            EditBox input = ChatButtonsHandler.input(screen);
            if (input != null) input.insertText(Character.toString(ref.codepoint()));
            if (!Screen.hasShiftDown()) {
                open = false;
                visible = false;
            }
            // Like the other chat buttons: the input keeps the keyboard after the click.
            Minecraft.getInstance().tell(() -> {
                if (input != null) {
                    screen.setFocused(input);
                    input.setFocused(true);
                }
            });
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (!visible || !isMouseOver(mouseX, mouseY)) return false;
            int rows = (EmojiFont.entries().size() + COLUMNS - 1) / COLUMNS;
            scroll = Math.max(0, Math.min(Math.max(0, rows - ROWS), scroll - (int) Math.signum(scrollY)));
            return true;
        }
    }

    /** Never takes focus, keys or keyboard navigation from the chat line — the same rules as the channel buttons. */
    private abstract static class Quiet extends AbstractWidget {

        Quiet(int x, int y, int width, int height, Component message) {
            super(x, y, width, height, message);
        }

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

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
