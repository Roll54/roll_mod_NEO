package com.roll_54.roll_mod.blocks.entity;

import com.roll_54.roll_mod.init.BlockEntites;
import com.roll_54.roll_mod.screen.menu.RocketControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class RocketControllerBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ContainerData data;

    public RocketControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntites.ROCKET_CONTROLLER_BE.get(), pos, blockState);
        this.data = new SimpleContainerData(0);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.roll_mod.rocket_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new RocketControllerMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries));
        super.saveAdditional(pTag, pRegistries);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, RocketControllerBlockEntity blockEntity) {
        // Ticking logic if needed (currently none)
    }
}
