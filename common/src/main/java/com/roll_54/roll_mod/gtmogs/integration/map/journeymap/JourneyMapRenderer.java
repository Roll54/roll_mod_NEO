package com.roll_54.roll_mod.gtmogs.integration.map.journeymap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.worldgen.WorldGenLayers;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreVeinUtil;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.integration.map.GenericMapRenderer;
import com.roll_54.roll_mod.gtmogs.integration.map.MapIntegrationUtils;
import com.roll_54.roll_mod.gtmogs.integration.map.WaypointManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.*;
import journeymap.api.v2.client.fullscreen.ModPopupMenu;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.util.UIState;
import lombok.Getter;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A map renderer for Journeymap, uses Journeymap's own tooltip rendering to fit existing theming better
 */
public class JourneyMapRenderer extends GenericMapRenderer {

    protected static final ResourceLocation STONE = ResourceLocation.withDefaultNamespace("block/stone");
    protected static final Map<Block, NativeImage> MATERIAL_ICONS = new HashMap<>();

    @Getter
    private static final Map<String, Overlay> markers = new Object2ObjectOpenHashMap<>();

    public JourneyMapRenderer() {
        super();
    }

    @Override
    public boolean addMarker(String name, ResourceKey<Level> dim, GeneratedVeinMetadata vein, String id) {
        IClientAPI api = JourneyMapPlugin.getJmApi();
        if (!api.playerAccepts(GTMOGS.MOD_ID, DisplayType.Image)) {
            return false;
        }
        MarkerOverlay marker = createMarker(name, id, dim, vein);
        markers.put(id, marker);
        if (this.doShowLayer("ore_veins")) {
            try {
                api.show(marker);
            } catch (Exception e) {
                // It never actually throws anything...
                GTMOGS.LOGGER.error("Failed to enable marker with name {}", name, e);
            }
        }
        return true;
    }

    @Override
    public boolean removeMarker(ResourceKey<Level> dim, String id) {
        Overlay marker = markers.remove(id);
        if (marker == null) {
            return false;
        }
        IClientAPI api = JourneyMapPlugin.getJmApi();
        api.remove(marker);
        return true;
    }

    @Override
    public boolean doShowLayer(String name) {
        return JourneyMapPlugin.getOptions().showLayer(name);
    }

    @Override
    public void setLayerActive(String name, boolean active) {
        JourneyMapPlugin.getOptions().toggleLayer(name, active);
    }

    @Override
    public void clear() {
        var api = JourneyMapPlugin.getJmApi();
        markers.forEach((id, marker) -> api.remove(marker));
        markers.clear();
    }

    private MarkerOverlay createMarker(String name, String id, ResourceKey<Level> dim, GeneratedVeinMetadata vein) {
        BlockPos center = OreVeinUtil.getVeinCenter(vein.originChunk(), RandomSource.create(0)).get();
        @SuppressWarnings("DataFlowIssue")
        MapImage image = new MapImage(createOreImage(vein));
        image.centerAnchors()
                .setDisplayWidth(ConfigHolder.INSTANCE.compat.minimap.oreIconSize)
                .setDisplayHeight(ConfigHolder.INSTANCE.compat.minimap.oreIconSize);

        MarkerOverlay overlay = new MarkerOverlay(GTMOGS.MOD_ID, center, image);
        overlay.setDisplayOrder(1); // supposedly the default is 1000. i don't know what the z-index for tooltips is.

        overlay.setDimension(dim);
        name = name + ": " + MapIntegrationUtils.veinCenter(vein);
        if (vein.definition().value().layer().equals(WorldGenLayers.DEEPSLATE)) {
            name = "+ " + name;
        }
        if (vein.definition().value().layer().equals(WorldGenLayers.STONE)) {
            name = name + "\n ";
        }
        overlay.setLabel("")
                .setTitle(name)
                .setOverlayListener(new MarkerListener(vein, name));

        return overlay;
    }

    private static NativeImage createOreImage(GeneratedVeinMetadata vein) {
        var material = vein.definition().value().veinGenerator().getAllBlocks().getFirst().getBlock();

        var sprite = MapIntegrationUtils.getFirstBlockFace(material);
        return sprite.contents().getOriginalImage();
    }

    /**
     * Listener for events on a MarkerOverlay instance.
     */
    @ParametersAreNonnullByDefault
    private static class MarkerListener implements IOverlayListener {

        private final GeneratedVeinMetadata oreVein;
        private final String label;

        private MarkerListener(GeneratedVeinMetadata oreVein, String label) {
            this.oreVein = oreVein;
            this.label = label;
        }

        @Override
        public void onActivate(UIState uiState) {}

        @Override
        public void onDeactivate(UIState uiState) {}

        @Override
        public void onMouseMove(UIState uiState, Point2D.Double mousePosition, BlockPos blockPosition) {}

        @Override
        public void onMouseOut(UIState mapState, Point2D.Double mousePosition, BlockPos blockPosition) {}

        @Override
        public boolean onMouseClick(UIState uiState, Point2D.Double mousePosition, BlockPos blockPosition, int button,
                                    boolean doubleClick) {
            if (button == 0 && doubleClick) {
                if (oreVein != null) {
                    Block firstMaterial = oreVein.definition().value().veinGenerator().getAllBlocks().getFirst()
                            .getBlock();
                    int color = MapIntegrationUtils.getItemColor(firstMaterial);

                    BlockPos center = oreVein.center();
                    WaypointManager.toggleWaypoint("ore_veins", label, color,
                            null, center.getX(), center.getY(), center.getZ());
                }
                return false;
            }
            return true;
        }

        @Override
        public void onOverlayMenuPopup(UIState mapState, Point2D.Double mousePosition, BlockPos blockPosition,
                                       ModPopupMenu modPopupMenu) {
            modPopupMenu.addMenuItem("button.gtmogs.mark_as_depleted.name", (b) -> {
                if (oreVein != null) {
                    oreVein.depleted(!oreVein.depleted());
                }
            });
            modPopupMenu.addMenuItem("button.gtmogs.toggle_waypoint.name", (b) -> {
                if (oreVein != null) {
                    Block firstMaterial = oreVein.definition().value().veinGenerator().getAllBlocks().getFirst()
                            .getBlock();
                    int color = MapIntegrationUtils.getItemColor(firstMaterial);
                    BlockPos center = oreVein.center();
                    WaypointManager.toggleWaypoint("ore_veins", label, color,
                            null, center.getX(), center.getY(), center.getZ());
                }
            });
        }
    }
}
