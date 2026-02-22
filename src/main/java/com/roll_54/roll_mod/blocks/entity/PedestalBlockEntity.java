package com.roll_54.roll_mod.blocks.entity;

import com.roll_54.roll_mod.init.BlockEntites;
import com.roll_54.roll_mod.screen.menu.PedestalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class PedestalBlockEntity extends BlockEntity implements MenuProvider {

    private float rotation;

    public final ItemStackHandler inventory = new ItemStackHandler(1) /*<-
    ці магічні букви відповідають за кількість СЛОТІВ в інтерфейсі */
    {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1; // <- потребує інт на максимальний стак предметів в слоті в СЛОТІ інтерфейсу
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()){
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3); // 3 апдейтить всі сусідні блоки
            }
        }
    };


    public PedestalBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntites.PEDESTAL_BE.get(), pos, blockState);
    }

    public void clearContents() {
        inventory.setStackInSlot(0, ItemStack.EMPTY);
    } // для очищення слота 0 який є першим, а цей метод треба для взаємодії з фізичним блоком

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for(int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    public float getRenderingRotation() {
        rotation += 0.5f;
        if(rotation >= 360) {
            rotation = 0;
        }
        return rotation;
    } // крутім - вертім штукі

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); // <- ЙОБНУТЬСЯ який важливий метод для збереження данних блок-ентіті
        tag.put("inventory", inventory.serializeNBT(registries)); // <- збереження інвентаря в нбт тегу під назвою "inventory"
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); // <- ЙОБНУТЬСЯ який важливий метод для завантаження данних блок-ентіті ГРАВЦЕВІ ЧИ ЧОМУСЬ НЕБУДЬ ЗЗОВІ
        inventory.deserializeNBT(registries, tag.getCompound("inventory")); // <- завантаження інвентаря з нбт тегу під назвою "inventory"
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }
    // ці два методи зверху для синхронізації данних в блоці.

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.roll_mod.pedestal_block"); // <- назва блока зліва зверху, ОБОВ'ЯЗКОВО РОБИТИ ПЕРЕКЛАДАЄМИМ!
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PedestalMenu(i, inventory, this); // надо повертатись до цього в кінці коли ґуі готове
    }


}
