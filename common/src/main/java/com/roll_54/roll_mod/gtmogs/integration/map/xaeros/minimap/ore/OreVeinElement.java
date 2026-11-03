package com.roll_54.roll_mod.gtmogs.integration.map.xaeros.minimap.ore;

import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import net.minecraft.client.Minecraft;

import lombok.Getter;
import net.minecraft.world.level.block.state.BlockState;

public class OreVeinElement {

    @Getter
    private final GeneratedVeinMetadata vein;
    @Getter
    private final String name;
    @Getter
    private final int cachedNameLength;

    public OreVeinElement(GeneratedVeinMetadata vein, String name) {
        this.vein = vein;
        this.name = name;

        this.cachedNameLength = Minecraft.getInstance().font.width(this.getName());
    }

    public BlockState getFirstMaterial() {
        return vein.definition().value().veinGenerator().getAllBlocks().getFirst();
    }
}
