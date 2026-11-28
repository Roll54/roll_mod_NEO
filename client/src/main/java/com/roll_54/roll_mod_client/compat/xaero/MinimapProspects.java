package com.roll_54.roll_mod_client.compat.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xaero.common.HudMod;
import xaero.common.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xaero.hud.minimap.element.render.MinimapElementReader;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;
import xaero.hud.minimap.element.render.MinimapElementRenderProvider;
import xaero.hud.minimap.element.render.MinimapElementRenderer;

import java.util.Iterator;

/**
 * Prospected ores' icons on Xaero's Minimap, through its public element API (the minimap's over-map handler): the
 * main ore of each prospected chunk at its centre, one flat icon per chunk as the minimap shows mobs. Icons only —
 * the ore layer is a World Map mode.
 */
@OnlyIn(Dist.CLIENT)
public final class MinimapProspects {

    /**
     * An icon's side in GUI pixels, about the size of the minimap's mob icons: the same at every zoom, scaled only by its
     * own icon scale ({@code optionalScale}).
     */
    private static final int ICON_SIZE = 9;

    /** The config's size for Minimap icons, kept sane. */
    private static float userScale() {
        return (float) Math.max(0.25, Math.min(4.0, ProspectMapState.config().minimapIconScale));
    }

    private static Object registeredOn;

    private MinimapProspects() {}

    /** Adds the renderer to the minimap once it exists (again if Xaero made a new one). Safe to call every tick. */
    public static void tryRegister() {
        if (HudMod.INSTANCE == null) return;
        var minimap = HudMod.INSTANCE.getMinimap();
        if (minimap == null || minimap == registeredOn) return;
        minimap.getOverMapRendererHandler().add(new Renderer(new Reader(), new Provider(), new Object()));
        registeredOn = minimap;
    }

    static final class Provider extends MinimapElementRenderProvider<ProspectVein, Object> {
        private Iterator<ProspectVein> chunks = java.util.Collections.emptyIterator();

        @Override
        public void begin(MinimapElementRenderLocation location, Object context) {
            chunks = ProspectMapState.config().showOnMinimap ? ProspectVein.current()
                    : java.util.Collections.emptyIterator();
        }

        @Override
        public boolean hasNext(MinimapElementRenderLocation location, Object context) {
            return chunks.hasNext();
        }

        @Override
        public ProspectVein getNext(MinimapElementRenderLocation location, Object context) {
            return chunks.next();
        }

        @Override
        public void end(MinimapElementRenderLocation location, Object context) {
            chunks = java.util.Collections.emptyIterator();
        }
    }

    static final class Reader extends MinimapElementReader<ProspectVein, Object> {
        @Override public boolean isHidden(ProspectVein e, Object c) { return false; }
        @Override public double getRenderX(ProspectVein e, Object c, float p) { return e.x(); }
        @Override public double getRenderY(ProspectVein e, Object c, float p) {
            var player = Minecraft.getInstance().player;
            return player == null ? 64 : player.getY();
        }
        @Override public double getRenderZ(ProspectVein e, Object c, float p) { return e.z(); }
        @Override public int getInteractionBoxLeft(ProspectVein e, Object c, float p) { return -half(); }
        @Override public int getInteractionBoxRight(ProspectVein e, Object c, float p) { return half(); }
        @Override public int getInteractionBoxTop(ProspectVein e, Object c, float p) { return -half(); }
        @Override public int getInteractionBoxBottom(ProspectVein e, Object c, float p) { return half(); }
        @Override public int getRenderBoxLeft(ProspectVein e, Object c, float p) { return -half(e); }
        @Override public int getRenderBoxRight(ProspectVein e, Object c, float p) { return half(e); }
        @Override public int getRenderBoxTop(ProspectVein e, Object c, float p) { return -half(e); }
        @Override public int getRenderBoxBottom(ProspectVein e, Object c, float p) { return half(e); }
        @Override public int getLeftSideLength(ProspectVein e, Minecraft mc) { return 0; }

        private static int half() {
            return (int) Math.ceil(ICON_SIZE * userScale()) + 1;
        }

        /** The icon's half-size plus how far it is shifted aside from its spot. */
        private static int half(ProspectVein e) {
            float size = ICON_SIZE * userScale() * e.iconScale();
            return (int) Math.ceil(size / 2 + Math.abs(e.offset(size))) + 1;
        }
        @Override public String getMenuName(ProspectVein e) { return ""; }
        @Override public String getFilterName(ProspectVein e) { return ""; }
        @Override public int getMenuTextFillLeftPadding(ProspectVein e) { return 0; }
        @Override public int getRightClickTitleBackgroundColor(ProspectVein e) { return 0; }
        @Override public boolean shouldScaleBoxWithOptionalScale() { return true; }
    }

    static final class Renderer extends MinimapElementRenderer<ProspectVein, Object> {

        Renderer(Reader reader, Provider provider, Object context) {
            super(reader, provider, context);
        }

        @Override
        public boolean renderElement(ProspectVein e, boolean highlighted, boolean outOfBounds, double optionalDepth,
                                     float optionalScale, double partialX, double partialY,
                                     MinimapElementRenderInfo info, GuiGraphics graphics,
                                     MultiBufferSource.BufferSource buffers) {
            // Chunks past the minimap's edge are not pinned to it, as waypoints are: they simply are not shown.
            if (outOfBounds) return false;
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(partialX, partialY, 0);
            float size = ICON_SIZE * userScale() * e.iconScale() * optionalScale;
            pose.pushPose();
            pose.translate(e.offset(size), 0, 0);
            ProspectIcons.draw(graphics, e.ores(), size);
            pose.popPose();
            pose.popPose();
            return false;
        }

        @Override
        public void preRender(MinimapElementRenderInfo info, MultiBufferSource.BufferSource buffers,
                              MultiTextureRenderTypeRendererProvider rendererProvider) {
        }

        @Override
        public void postRender(MinimapElementRenderInfo info, MultiBufferSource.BufferSource buffers,
                               MultiTextureRenderTypeRendererProvider rendererProvider) {
        }

        @Override
        public boolean shouldRender(MinimapElementRenderLocation location) {
            return ProspectMapState.visible() && ProspectMapState.config().showOnMinimap;
        }
    }
}
