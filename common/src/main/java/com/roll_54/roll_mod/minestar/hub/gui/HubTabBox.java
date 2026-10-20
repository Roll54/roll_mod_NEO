package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;

import java.util.EnumMap;
import java.util.Map;

/**
 * The hub's bookmarks: boxes closed on all four sides, on whichever side of the window the tabs sit.
 *
 * <p>Every theme's own tab art is made for tabs along a top edge: it is open (or rounded) at the
 * bottom so the selected tab runs into the panel under it. Down the left, the right or along the
 * bottom that open edge faces the wrong way and reads as torn — so the hub uses one closed box on
 * every side, the top included, drawn in the theme's colours so it still matches it. The vanilla-looking
 * themes get vanilla's own closed box ({@code RECT_THIN}), dimmed to sit in the dark window as the
 * bookmarks did before themes.
 *
 * <p>Read through {@link IGuiTexture#dynamic} every frame, so a theme picked in the settings shows on
 * the bookmarks at once. The boxes are built once per theme and kept.
 */
final class HubTabBox {

    private record Box(IGuiTexture base, IGuiTexture hover, IGuiTexture selected) {}

    private static final Map<UiTheme, Box> BOXES = new EnumMap<>(UiTheme.class);

    /** The content panel's own wash, so the selected bookmark reads as belonging to it. */
    private static final int COLOR_CONTENT = 0x40000000;
    private static final int MC_DIM = 0xFF707070;

    static final IGuiTexture BASE = IGuiTexture.dynamic(() -> box().base());
    static final IGuiTexture HOVER = IGuiTexture.dynamic(() -> box().hover());
    static final IGuiTexture SELECTED = IGuiTexture.dynamic(() -> box().selected());

    private HubTabBox() {}

    private static Box box() {
        return BOXES.computeIfAbsent(UiThemeState.get(), HubTabBox::build);
    }

    private static Box build(UiTheme theme) {
        UiTheme.Palette palette = theme.palette();
        if (palette == null) {
            // Built from the atlas rather than MCSprites.RECT_THIN: MCSprites swaps its texture fields
            // at resource load, so holding one would race the load order (see HubUI's history).
            IGuiTexture box = SpriteTexture.of(MCSprites.MC).setSprite(48, 0, 16, 16).setBorder(3).setColor(MC_DIM);
            return new Box(box, box, new GuiTextureGroup(box, new ColorRectTexture(COLOR_CONTENT)));
        }
        return new Box(
                closed(palette.raised(), palette.line()),
                closed(palette.hovered(), palette.line()),
                closed(palette.hovered(), palette.accent()));
    }

    private static IGuiTexture closed(int fill, int border) {
        return new GuiTextureGroup(new ColorRectTexture(fill), new ColorBorderTexture(1, border));
    }
}
