package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * The tall strip that folds a panel away and brings it back, as on Eden_LD: a bar the panel's full height with an
 * arrow in its middle, pointing the way the panel will go. Replaces the small "»" / "«" buttons.
 */
final class HubFoldBar {

    static final int WIDTH = 9;
    private static final int FILL = 0x40000000;
    private static final int FILL_HOVER = 0x60FFFFFF;
    private static final int RIM = 0x80FFFFFF;

    private HubFoldBar() {}

    /**
     * @param folded    whether the panel is folded now
     * @param panelLeft whether the panel sits left of the bar (its arrow then reads "&lt;" while open)
     * @param toggle    folds or unfolds it
     */
    static UIElement create(BooleanSupplier folded, boolean panelLeft, Runnable toggle, String hideKey, String showKey) {
        return create(folded, panelLeft, toggle, hideKey, showKey, () -> null);
    }

    /**
     * As above; {@code label}, when it gives something, stands in for the arrow (a folded requests panel
     * shows how many wait).
     */
    static UIElement create(BooleanSupplier folded, boolean panelLeft, Runnable toggle, String hideKey, String showKey,
                            java.util.function.Supplier<String> label) {
        UIElement bar = new UIElement();
        bar.layout(l -> l.width(WIDTH).heightPercent(100).flexShrink(0));
        bar.style(s -> s.background(IGuiTexture.dynamic(() -> new GuiTextureGroup(
                new ColorRectTexture(bar.isHover() ? FILL_HOVER : FILL), new ColorBorderTexture(1, RIM)))));
        Label arrow = new Label();
        arrow.layout(l -> l.widthPercent(100).heightPercent(100));
        arrow.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));
        // Open with the panel on the left: "<" folds it left; folded, ">" brings it back. Mirrored on the right.
        String[] shown = {null};
        Runnable refresh = () -> {
            boolean shut = folded.getAsBoolean();
            String custom = label.get();
            String glyph = custom != null ? custom : shut == panelLeft ? ">" : "<";
            if (!glyph.equals(shown[0])) {
                shown[0] = glyph;
                arrow.setText(Component.literal(glyph));
            }
        };
        refresh.run();
        bar.addChild(arrow);
        bar.addEventListener(UIEvents.TICK, e -> refresh.run());
        bar.addEventListener(UIEvents.CLICK, e -> {
            toggle.run();
            refresh.run();
        });
        bar.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(folded.getAsBoolean() ? showKey : hideKey)), null, null, ItemStack.EMPTY));
        return bar;
    }
}
