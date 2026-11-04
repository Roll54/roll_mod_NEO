package com.roll_54.roll_mod.minestar.debug;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /rollmod admin debug orepalette [all_possible_variants]}: every ore material ({@link OreDefinitions}) in a flat
 * square centred under the player — to see all of them, or scan them, at a glance. Overwrites what is there.
 * Plain: one block per material, its first base ore. {@code all_possible_variants}: every ore block there is, material
 * by material — each base layer's ore, then each rock variant's (granite, tuff, basalt...).
 */
public final class OrePalette {

    private OrePalette() {}

    /** Places the square; returns how many blocks, and the side length, as {@code {count, side}}. */
    public static int[] place(ServerPlayer player, boolean allVariants) {
        List<Block> ores = new ArrayList<>();
        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
            List<String> ids = new ArrayList<>();
            if (allVariants) {
                def.bases().forEach(base -> ids.add(base.id() + "_" + def.id()));
                def.variants().forEach(variant -> ids.add(def.variantBlockId(variant)));
            } else {
                ids.add(def.bases().getFirst().id() + "_" + def.id());
            }
            for (String id : ids) BuiltInRegistries.BLOCK.getOptional(RollMod.id(id)).ifPresent(ores::add);
        }
        int side = (int) Math.ceil(Math.sqrt(ores.size()));
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition().below().offset(-side / 2, 0, -side / 2);
        for (int i = 0; i < ores.size(); i++) {
            level.setBlock(origin.offset(i % side, 0, i / side), ores.get(i).defaultBlockState(), 3);
        }
        return new int[]{ores.size(), side};
    }
}
