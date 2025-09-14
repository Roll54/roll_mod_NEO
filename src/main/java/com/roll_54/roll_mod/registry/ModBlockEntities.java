package com.roll_54.roll_mod.registry;

import com.roll_54.roll_mod.Roll_mod;
import com.roll_54.roll_mod.machine.LargeChemicalReactorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Roll_mod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LargeChemicalReactorBlockEntity>> LARGE_CHEMICAL_REACTOR_BE =
            BLOCK_ENTITIES.register("large_chemical_reactor",
                    () -> BlockEntityType.Builder.of(
                            LargeChemicalReactorBlockEntity::new,
                            ModBlocks.LARGE_CHEMICAL_REACTOR.get()
                    ).build(null)
            );

    private ModBlockEntities() {}
}
