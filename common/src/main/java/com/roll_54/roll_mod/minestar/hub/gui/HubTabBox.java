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

    /**
     * The bookmarks on every theme are AE2-shaped buttons ({@link HubBevel}): a 1px outline, a light top-left edge and a
     * shaded bottom-right one; the open tab is the same box pressed in (the edges swapped, the face a step darker).
     * The colours are the theme's: AE2's own on AE2, vanilla's greys on Minecraft, the palette's elsewhere.
     */
    private static Box build(UiTheme theme) {
        if (theme == UiTheme.AE2) {
            return new Box(new HubBevel(0xFFCBCCD4, 0xFFF2F2F2, 0xFF878FA5, 0xFF413F54),
                    new HubBevel(0xFFE2E3E9, 0xFFFFFFFF, 0xFF878FA5, 0xFF413F54),
                    new HubBevel(0xFFADB0C4, 0xFF878FA5, 0xFFF2F2F2, 0xFF413F54));
        }
        UiTheme.Palette palette = theme.palette();
        int face = palette == null ? 0xFF8B8B8B : palette.raised();
        int hover = palette == null ? 0xFF9B9B9B : palette.hovered();
        // The palette's line where it is a solid colour; Dusk-family lines are faint white hairlines, which would
        // not read as an outline, so those take a dark step of the face instead.
        int outline = palette != null && (palette.line() >>> 24) == 0xFF ? palette.line() : HubBevel.scale(face, 0.35f);
        HubBevel up = HubBevel.of(face, outline);
        HubBevel lit = HubBevel.of(hover, outline);
        int pressed = HubBevel.scale(face, 0.82f);
        HubBevel down = new HubBevel(pressed, HubBevel.scale(pressed, 0.72f), HubBevel.scale(pressed, 1.25f), outline);
        return new Box(up, lit, down);
    }

    private static IGuiTexture closed(int fill, int border) {
        return new GuiTextureGroup(new ColorRectTexture(fill), new ColorBorderTexture(1, border));
    }
}
