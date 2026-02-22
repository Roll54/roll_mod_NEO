package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.entity.*;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class BlockEntites {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, RollMod.MODID);

    public static final Supplier<BlockEntityType<PedestalBlockEntity>> PEDESTAL_BE =
            BLOCK_ENTITIES.register("pedestal_be", () -> BlockEntityType.Builder.of(
                    PedestalBlockEntity::new, BlockRegistry.PEDESTAL_BLOCK.get()).build(null));

    public static final Supplier<BlockEntityType<GrowthChamberBlockEntity>> GROWTH_CHAMBER_BE =
            BLOCK_ENTITIES.register("growth_chamber_be", () -> BlockEntityType.Builder.of(
                    GrowthChamberBlockEntity::new, BlockRegistry.GROWTH_CHAMBER.get()).build(null));

    public static final Supplier<BlockEntityType<ResearchWorkbenchBlockEntity>> RESEARCH_WORKBENCH_BE =
            BLOCK_ENTITIES.register("research_workbench_be", () -> BlockEntityType.Builder.of(
                    ResearchWorkbenchBlockEntity::new, BlockRegistry.RESEARCH_WORKBENCH.get()).build(null));



    public static void register(IEventBus eventBus){
            BLOCK_ENTITIES.register(eventBus);
    }


}
