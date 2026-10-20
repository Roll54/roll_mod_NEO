package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.roll_54.roll_mod.util.ItemMentions;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A {@link Label} whose text can hold links — {@code OPEN_URL} click events, as
 * {@code LetterText.display} builds them — and items ({@link ItemMentions}), whose icon it draws
 * and whose tooltip shows on hover. LDLib's text element draws styled text but never looks at
 * click or hover events, so this finds the run under the mouse itself.
 *
 * <p>An item's icon goes into the blank {@link ItemMentions#ICON} slot the mention leaves for it,
 * drawn over the text once the text is down.
 *
 * <p>The hit test and the icons redo exactly the layout {@code TextElement} drew: the same
 * {@code TextUtilities.computeFormattedLines} call, lines {@code fontSize + lineSpacing} apart from
 * the top of the content box, each line placed by the horizontal alignment.
 */
public class LinkLabel extends Label {

    public LinkLabel() {
        addEventListener(UIEvents.CLICK, e -> {
            if (FMLEnvironment.dist != Dist.CLIENT || e.button != 0) return;
            ClickEvent click = ClientLinks.styleAt(this, e.x, e.y) instanceof Style style ? style.getClickEvent() : null;
            if (click != null && click.getAction() == ClickEvent.Action.OPEN_URL) {
                ClientLinks.open(click.getValue());
            }
        });
        addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            Style style = ClientLinks.styleAt(this, e.x, e.y);
            HoverEvent hover = style == null ? null : style.getHoverEvent();
            if (hover == null) return;
            Component text = hover.getValue(HoverEvent.Action.SHOW_TEXT);
            HoverEvent.ItemStackInfo item = hover.getValue(HoverEvent.Action.SHOW_ITEM);
            if (text != null) {
                e.hoverTooltips = new HoverTooltips(List.of(text), null, null, ItemStack.EMPTY);
            } else if (item != null) {
                e.hoverTooltips = ClientLinks.itemTooltip(item.getItemStack());
            }
        });
    }

    /** The text {@link #hasIcons} last looked at, so the walk runs once per text, not per frame. */
    @Nullable private Component iconsChecked;
    private boolean hasIcons;

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (hasIcons()) ClientLinks.drawIcons(this, context);
    }

    private boolean hasIcons() {
        Component text = getText();
        if (text != iconsChecked) {
            iconsChecked = text;
            hasIcons = text != null && text.getString().contains(ItemMentions.ICON);
        }
        return hasIcons;
    }

    /** Isolated holder for the client-only half, as {@code HubUI.ClientReturn} is. */
    private static final class ClientLinks {

        /**
         * Every {@link ItemMentions#ICON} slot in the label, its item drawn into it: one line-height
         * square, centred in the slot's advance.
         */
        static void drawIcons(LinkLabel label, GUIContext context) {
            var font = label.getFont();
            var textStyle = label.getTextStyle();
            float fontSize = textStyle.fontSize();
            float scale = fontSize / font.lineHeight;
            float width = label.getContentWidth();
            var lines = com.lowdragmc.lowdraglib2.utils.TextUtilities.computeFormattedLines(font,
                    com.lowdragmc.lowdraglib2.utils.TextUtilities.withFont(label.getText(), textStyle.font()),
                    fontSize, width);
            float lineStep = fontSize + textStyle.lineSpacing();
            Horizontal align = textStyle.textAlignHorizontal();
            var graphics = context.graphics;

            for (int i = 0; i < lines.size(); i++) {
                var line = lines.get(i);
                float lineWidth = line.getB();
                float x = label.getContentX();
                if (align == Horizontal.CENTER) x += (width - lineWidth) / 2f;
                else if (align == Horizontal.RIGHT) x += width - lineWidth;
                float lineX = x;
                float lineY = label.getContentY() + i * lineStep;

                float[] advance = {0f};
                line.getA().accept((index, style, codePoint) -> {
                    float glyph = font.width(net.minecraft.network.chat.FormattedText.of(
                            Character.toString(codePoint), style));
                    HoverEvent hover = style.getHoverEvent();
                    HoverEvent.ItemStackInfo item = hover == null ? null : hover.getValue(HoverEvent.Action.SHOW_ITEM);
                    if (item != null && codePoint == ItemMentions.ICON.codePointAt(0)
                            && ItemMentions.ICON_FONT.equals(style.getFont())) {
                        float size = fontSize;
                        float slot = glyph * scale;
                        drawItem(graphics, item.getItemStack(), lineX + advance[0] * scale + (slot - size) / 2f,
                                lineY - 0.5f * scale, size);
                    }
                    advance[0] += glyph;
                    return true;
                });
            }
        }

        /** {@code stack} scaled into a {@code size}-pixel square at ({@code x}, {@code y}). */
        private static void drawItem(net.minecraft.client.gui.GuiGraphics graphics, ItemStack stack,
                                     float x, float y, float size) {
            graphics.flush();
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(size / 16f, size / 16f, 1f);
            com.lowdragmc.lowdraglib2.gui.util.DrawerHelper.drawItemStack(graphics, stack, 0, 0, -1, null);
            graphics.pose().popPose();
        }

        @Nullable
        static Style styleAt(LinkLabel label, float mouseX, float mouseY) {
            var font = label.getFont();
            var textStyle = label.getTextStyle();
            float fontSize = textStyle.fontSize();
            float scale = fontSize / font.lineHeight;
            float width = label.getContentWidth();
            var lines = com.lowdragmc.lowdraglib2.utils.TextUtilities.computeFormattedLines(font,
                    com.lowdragmc.lowdraglib2.utils.TextUtilities.withFont(label.getText(), textStyle.font()),
                    fontSize, width);
            float lineStep = fontSize + textStyle.lineSpacing();
            float dy = mouseY - label.getContentY();
            if (dy < 0f || lineStep <= 0f) return null;
            int index = (int) (dy / lineStep);
            if (index >= lines.size()) return null;

            var line = lines.get(index);
            float lineWidth = line.getB();
            float x = label.getContentX();
            Horizontal align = textStyle.textAlignHorizontal();
            if (align == Horizontal.CENTER) x += (width - lineWidth) / 2f;
            else if (align == Horizontal.RIGHT) x += width - lineWidth;
            float dx = mouseX - x;
            if (dx < 0f || dx > lineWidth) return null;
            return font.getSplitter().componentStyleAtWidth(line.getA(), (int) (dx / scale));
        }

        /** The item's own tooltip, image included — what an {@code <item:…>} tag shows on hover. */
        static HoverTooltips itemTooltip(ItemStack stack) {
            var lines = net.minecraft.client.gui.screens.Screen.getTooltipFromItem(
                    net.minecraft.client.Minecraft.getInstance(), stack);
            return new HoverTooltips(lines, stack.getTooltipImage().orElse(null), null, stack);
        }

        /**
         * Straight to the browser, without vanilla's "are you sure" screen: opening another screen
         * over the hub would close its menu, and every letter is written by staff or the server
         * itself. The address is on the link's tooltip before it is clicked. A player who switched
         * chat links off in their settings gets nothing, as in chat.
         */
        static void open(String url) {
            var minecraft = net.minecraft.client.Minecraft.getInstance();
            if (!minecraft.options.chatLinks().get()) return;
            try {
                var uri = new java.net.URI(url);
                String scheme = uri.getScheme();
                if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return;
                net.minecraft.Util.getPlatform().openUri(uri);
            } catch (java.net.URISyntaxException ignored) {
                // LetterText only links what looks like an address; one that still fails to parse
                // is simply not opened.
            }
        }
    }
}
