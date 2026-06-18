package com.roll_54.roll_mod.blocks.entity;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.roll_54.roll_mod.registry.BlockEntites;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;


public class TelescopeBlockEntity  extends BlockEntity implements ISyncPersistRPCBlockEntity {
    // your fields
    @Persisted
    @DescSynced
    public int progress = 0;

    @Persisted
    @DescSynced
    public String owner = "";


    public TelescopeBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntites.TELESCOPE_BE.get(), pos, blockState);
    }


    public final ItemStackHandler inventory = new ItemStackHandler(1) /*<-
    ці магічні букви відповідають за кількість СЛОТІВ в інтерфейсі */
    {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 64; // <- потребує інт на максимальний стак предметів в слоті в СЛОТІ інтерфейсу
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()){
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3); // 3 апдейтить всі сусідні блоки
            }
        }
    };

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    /**
     * Get managed storage.
     */
    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }
}
