package com.roll_54.roll_mod.blocks.entity;

import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.roll_54.roll_mod.config.MyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RollSolarPanelBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    public RollSolarPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }




    public static double getDimensionMultiplier(Level level) {
        ResourceLocation dimensionId = level.dimension().location();


        return MyConfig.SolarPanelOptions.dimensionMultipliers.getOrDefault(dimensionId, 1.0);
    }
    /**
     * Get managed storage.
     */
    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }
}
