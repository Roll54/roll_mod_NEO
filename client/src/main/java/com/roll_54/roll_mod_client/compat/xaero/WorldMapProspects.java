package com.roll_54.roll_mod_client.compat.xaero;

import com.roll_54.roll_mod.prospecting.Discovery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xaero.map.WorldMap;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Prospected ores on Xaero's World Map, through its public element API ({@code WorldMap.mapElementRenderHandler}).
 * ICONS: one element per vein (neighbouring chunks of one ore vein merged, {@link ProspectVein}), its main ore as one
 * icon at the chunks' middle, bigger the more chunks it spans; on hover its chunks are tinted in that ore's colour and
 * the tooltip lists its ores (with their counts once enough was mined). ORE_LAYER: every recorded ore block within the chosen height (±
 * {@code oreLayerSpread}) as a square in the ore scanner's colour, drawn over the map.
 *
 * <p>Element units are GUI pixels: one block is {@code ElementRenderInfo.scale} of them, the pose's origin is the
 * element's position (rounded, the rest passed as {@code partialX/partialY}).
 */
@OnlyIn(Dist.CLIENT)
public final class WorldMapProspects {

    private static boolean registered;
    /** The vein under the mouse this frame, set while the map draws and used once after it. */
    private static ProspectVein hoveredVein;

    private WorldMapProspects() {}

    /** Adds the renderer once World Map's handler exists. Safe to call every tick. */
    public static void tryRegister() {
        if (registered || WorldMap.mapElementRenderHandler == null) return;
        WorldMap.mapElementRenderHandler.add(new Renderer(new Context(), new Provider(), new Reader()));
        registered = true;
    }

    /**
     * The hovered chunk's ores (name, heights, and the vein's size once {@code exactCountAfter} of it were mined) at
     * the mouse, over the whole map screen: drawn inside the map's own pass it would sit under later elements.
     */
    static void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ProspectVein e = hoveredVein;
        hoveredVein = null;
        if (e == null || ProspectMapState.oreLayer() || mouseX == Integer.MIN_VALUE) return;
        List<Component> lines = ProspectMapState.chunkTooltip(ProspectVeins.name(e.key(), e.ores()), e.ores());
        if (!lines.isEmpty()) graphics.renderComponentTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
    }

    /**
     * An icon's side in GUI pixels, as Xaero draws mob and player icons: the same on screen at every zoom, scaled only
     * by the map's own icon scale ({@code optionalScale}).
     */
    static final int ICON_SIZE = 20;

    /** The config's size for World Map icons, kept sane. */
    static float userScale() {
        return (float) Math.max(0.25, Math.min(4.0, ProspectMapState.config().worldMapIconScale));
    }


    static final class Context {
        double scale = 1;
    }

    static final class Provider extends ElementRenderProvider<ProspectVein, Context> {
        private Iterator<ProspectVein> chunks = java.util.Collections.emptyIterator();

        @Override
        public void begin(ElementRenderLocation location, Context context) {
            chunks = ProspectMapState.config().showOnWorldMap ? ProspectVein.current()
                    : java.util.Collections.emptyIterator();
        }

        @Override
        public boolean hasNext(ElementRenderLocation location, Context context) {
            return chunks.hasNext();
        }

        @Override
        public ProspectVein getNext(ElementRenderLocation location, Context context) {
            return chunks.next();
        }

        @Override
        public void end(ElementRenderLocation location, Context context) {
            chunks = java.util.Collections.emptyIterator();
        }
    }

    static final class Reader extends ElementReader<ProspectVein, Context, Renderer> {
        private static final int LAYER_BOX = 4096;

        @Override
        public boolean isHidden(ProspectVein e, Context c) {
            return false;
        }

        @Override
        public double getRenderX(ProspectVein e, Context c, float partial) {
            return e.x();
        }

        @Override
        public double getRenderZ(ProspectVein e, Context c, float partial) {
            return e.z();
        }

        @Override
        public boolean isInteractable(ElementRenderLocation location, ProspectVein e) {
            return !ProspectMapState.oreLayer();
        }

        @Override public int getInteractionBoxLeft(ProspectVein e, Context c, float p) { return -iconBox(e); }
        @Override public int getInteractionBoxRight(ProspectVein e, Context c, float p) { return iconBox(e); }
        @Override public int getInteractionBoxTop(ProspectVein e, Context c, float p) { return -iconBox(e); }
        @Override public int getInteractionBoxBottom(ProspectVein e, Context c, float p) { return iconBox(e); }
        // The ore layer's element spans its whole chunk, which grows with the zoom: never culled for its box.
        @Override public int getRenderBoxLeft(ProspectVein e, Context c, float p) { return -box(e, c); }
        @Override public int getRenderBoxRight(ProspectVein e, Context c, float p) { return box(e, c); }
        @Override public int getRenderBoxTop(ProspectVein e, Context c, float p) { return -box(e, c); }
        @Override public int getRenderBoxBottom(ProspectVein e, Context c, float p) { return box(e, c); }
        @Override public int getLeftSideLength(ProspectVein e, Minecraft mc) { return 0; }
        @Override public String getMenuName(ProspectVein e) { return ""; }
        @Override public String getFilterName(ProspectVein e) { return ""; }
        @Override public int getMenuTextFillLeftPadding(ProspectVein e) { return 0; }
        @Override public int getRightClickTitleBackgroundColor(ProspectVein e) { return 0; }
        @Override public boolean shouldScaleBoxWithOptionalScale() { return true; }
        @Override public boolean isRightClickValid(ProspectVein e) { return false; }

        /** In the ore layer and while hovered (the vein's chunks tinted) the element spans its chunks: never culled. */
        private static int box(ProspectVein e, Context c) {
            if (ProspectMapState.oreLayer() || e.chunks().size() > 1 || e.slots() > 1) return LAYER_BOX;
            return Math.max(iconBox(e), (int) Math.ceil(8 * c.scale) + 1);
        }

        private static int iconBox(ProspectVein e) {
            float size = ICON_SIZE * userScale() * e.iconScale();
            return (int) Math.ceil(size / 2 + Math.abs(e.offset(size))) + 1;
        }
    }

    static final class Renderer extends ElementRenderer<ProspectVein, Context, Renderer> {

        Renderer(Context context, Provider provider, Reader reader) {
            super(context, provider, reader);
        }

        @Override
        public void preRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers,
                              MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
            context.scale = info.scale;
        }

        @Override
        public void postRender(ElementRenderInfo info, MultiBufferSource.BufferSource buffers,
                               MultiTextureRenderTypeRendererProvider rendererProvider, boolean shadow) {
        }

        @Override
        public void renderElementShadow(ProspectVein e, boolean hovered, float optionalScale, double partialX,
                                        double partialY, ElementRenderInfo info, GuiGraphics graphics,
                                        MultiBufferSource.BufferSource buffers,
                                        MultiTextureRenderTypeRendererProvider rendererProvider) {
        }

        @Override
        public boolean renderElement(ProspectVein e, boolean hovered, double optionalDepth, float optionalScale,
                                     double partialX, double partialY, ElementRenderInfo info, GuiGraphics graphics,
                                     MultiBufferSource.BufferSource buffers,
                                     MultiTextureRenderTypeRendererProvider rendererProvider) {
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(partialX, partialY, 0);
            if (ProspectMapState.oreLayer()) {
                drawLayer(graphics, e, (float) info.scale);
            } else {
                float size = ICON_SIZE * userScale() * e.iconScale() * optionalScale;
                // Hovered when the mouse is over the icon (info.mouseX/Z: the block under it, info.scale: GUI pixels a
                // block) or anywhere over the vein's chunks, whether or not Xaero flags it.
                float shift = e.offset(size);
                double dx = (info.mouseX - e.x()) * info.scale - shift;
                double dz = (info.mouseZ - e.z()) * info.scale;
                boolean overIcon = Math.abs(dx) <= size / 2 && Math.abs(dz) <= size / 2;
                // Only the icon itself: the mouse over the vein's chunks elsewhere is not enough.
                if (overIcon && !ProspectVeinPanel.coversMouse()) {
                    hoveredVein = e;
                    tintChunks(graphics, e, info.scale);
                }
                pose.pushPose();
                pose.translate(shift, 0, 0);
                ProspectIcons.draw(graphics, e.ores(), size);
                pose.popPose();
                // The tooltip itself is drawn by the screen, after the whole map ({@link #renderHoveredTooltip}).
            }
            pose.popPose();
            return false;
        }


        /** The vein's chunks filled in its main ore's colour, see-through, under its icon. */
        private static void tintChunks(GuiGraphics graphics, ProspectVein e, double scale) {
            Discovery main = ProspectIcons.main(e.ores());
            if (main == null) return;
            int color = (ProspectMapState.color(main) & 0xFFFFFF) | 0x66000000;
            var pose = graphics.pose();
            pose.pushPose();
            pose.scale((float) scale, (float) scale, 1f);
            for (ProspectChunk c : e.chunks()) {
                float x0 = (float) (c.x() * 16 - e.x()), z0 = (float) (c.z() * 16 - e.z());
                pose.pushPose();
                pose.translate(x0, z0, 0);
                graphics.fill(0, 0, 16, 16, color);
                pose.popPose();
            }
            pose.popPose();
        }

        /** The chunk's recorded ore blocks within the chosen height, a square each, one block = {@code scale}. */
        private static void drawLayer(GuiGraphics graphics, ProspectVein vein, float scale) {
            ProspectChunk e = vein.chunks().getFirst();
            int y = ProspectMapState.layerY();
            int spread = Math.max(0, ProspectMapState.config().oreLayerSpread);
            var pose = graphics.pose();
            pose.pushPose();
            pose.scale(scale, scale, 1f);
            for (Discovery d : e.ores()) {
                int color = ProspectMapState.color(d);
                for (int packed : d.positions) {
                    if (Math.abs(Discovery.y(packed) - y) > spread) continue;
                    int x = Discovery.x(packed), z = Discovery.z(packed);
                    graphics.fill(x, z, x + 1, z + 1, color);
                }
            }
            pose.popPose();
        }

        @Override
        public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
            return !shadow && location == ElementRenderLocation.WORLD_MAP && ProspectMapState.visible()
                    && ProspectMapState.config().showOnWorldMap;
        }
    }
}
