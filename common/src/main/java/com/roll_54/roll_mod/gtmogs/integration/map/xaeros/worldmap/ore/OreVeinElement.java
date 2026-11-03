package com.roll_54.roll_mod.gtmogs.integration.map.xaeros.worldmap.ore;

import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import com.roll_54.roll_mod.gtmogs.integration.map.MapIntegrationUtils;
import com.roll_54.roll_mod.gtmogs.integration.map.WaypointManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import lombok.Getter;
import net.minecraft.world.level.block.Block;

public class OreVeinElement {

    @Getter
    private GeneratedVeinMetadata vein;
    @Getter
    private final String name;
    @Getter
    private final int cachedNameLength;

    public OreVeinElement(GeneratedVeinMetadata vein, String name) {
        this.vein = vein;
        this.name = name;

        this.cachedNameLength = Minecraft.getInstance().font.width(this.getName());
    }

    public void onMouseSelect() {
        int color = MapIntegrationUtils.getItemColor(getFirstMaterial());
        // TODO generalize to all possible layer types
        BlockPos center = vein.center();
        WaypointManager.toggleWaypoint("ore_veins", name, color,
                null, center.getX(), center.getY(), center.getZ());
    }

    public void toggleDepleted() {
        vein.depleted(!vein.depleted());
    }

    public Block getFirstMaterial() {
        return vein.definition().value().veinGenerator().getAllBlocks().getFirst().getBlock();
    }
}