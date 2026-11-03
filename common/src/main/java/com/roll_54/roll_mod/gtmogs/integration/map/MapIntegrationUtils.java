package com.roll_54.roll_mod.gtmogs.integration.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;

import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;

public class MapIntegrationUtils {

    public static int getItemColor(Block b) {
        return b.defaultMapColor().col;
    }

    public static ResourceLocation getItemIcon(Block b) {
        return ResourceLocation.parse(BuiltInRegistries.BLOCK.getKey(b).toString());
    }

    public static TextureAtlasSprite getFirstBlockFace(Block b) {
        RandomSource random = RandomSource.create();
        var sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(b.defaultBlockState())
                .getQuads(b.defaultBlockState(), Direction.NORTH, random).getFirst().getSprite();
        return sprite;
    }

    public static String veinCenter(GeneratedVeinMetadata vein) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(String.format("%2s", vein.center().getX()));
        sb.append(", ");
        sb.append(String.format("%2s", vein.center().getY()));
        sb.append(", ");
        sb.append(String.format("%2s", vein.center().getZ()));
        sb.append("]");
        return sb.toString();
    }
}
