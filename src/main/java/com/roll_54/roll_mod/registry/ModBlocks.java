package com.roll_54.roll_mod.registry;

import com.roll_54.roll_mod.Roll_mod;
import com.roll_54.roll_mod.machine.LargeChemicalReactorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Roll_mod.MODID);

    public static final DeferredHolder<Block, Block> LARGE_CHEMICAL_REACTOR =
            BLOCKS.register("large_chemical_reactor", LargeChemicalReactorBlock::new);

    private ModBlocks() {}
}