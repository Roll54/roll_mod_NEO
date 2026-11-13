package com.roll_54.roll_mod.minestar.title;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * Vanilla's yellow splash line — same random text, tilt and pulse — centred on this element's box
 * instead of beside the vanilla logo.
 *
 * <p>Drawn by vanilla's own {@link SplashRenderer}, which always pivots on
 * {@code (screenWidth / 2 + 123, 69)}: the pose is shifted first so that point lands on the box's
 * centre. Respects the "Hide Splash Texts" accessibility option.
 */
@OnlyIn(Dist.CLIENT)
final class TitleSplashElement extends UIElement {

    /** Where {@link SplashRenderer#render} pivots, relative to the screen's horizontal centre and top. */
    private static final float VANILLA_PIVOT_X = 123f;
    private static final float VANILLA_PIVOT_Y = 69f;
    /** Full alpha, in the form {@link SplashRenderer#render} ORs onto its colour. */
    private static final int OPAQUE = 0xFF000000;

    @Nullable private final SplashRenderer splash;

    TitleSplashElement(@Nullable SplashRenderer splash) {
        this.splash = splash;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        Minecraft mc = Minecraft.getInstance();
        if (splash == null || mc.options.hideSplashTexts().get() || mc.screen == null) return;

        int screenWidth = mc.screen.width;
        float centerX = getPositionX() + getSizeWidth() / 2f;
        float centerY = getPositionY() + getSizeHeight() / 2f;

        GuiGraphics graphics = context.graphics;
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - (screenWidth / 2f + VANILLA_PIVOT_X), centerY - VANILLA_PIVOT_Y, 0f);
        splash.render(graphics, screenWidth, mc.font, OPAQUE);
        graphics.pose().popPose();
        graphics.flush();
    }
}
