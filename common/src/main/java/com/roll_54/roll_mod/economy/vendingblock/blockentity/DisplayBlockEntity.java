package com.roll_54.roll_mod.economy.vendingblock.blockentity;

import com.roll_54.roll_mod.economy.vendingblock.registry.BlockEntityRegistry;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class DisplayBlockEntity extends BlockEntity {

  private UUID ownerID;
  private String ownerUser;

  public DisplayBlockEntity(BlockPos pos, BlockState state) {
    super(BlockEntityRegistry.DISPLAY_BE.get(), pos, state);
  }

  public final ItemStackHandler inventory =
      new ItemStackHandler(2) {
        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
          if (slot == 0 && !stack.isEmpty() && stack.getCount() > 1) {
            ItemStack dropStack = stack.copy();
            dropStack.setCount(stack.getCount() - 1);
            ;
            stack.setCount(1);
            SimpleContainer dropInv = new SimpleContainer(dropStack);
            Containers.dropContents(level, worldPosition, dropInv);
          }
          super.setStackInSlot(slot, stack);
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
          if (slot == 0) {
            return 1;
          } else {
            return 64;
          }
        }

        @Override
        protected void onContentsChanged(int slot) {
          setChanged();
          if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            level.invalidateCapabilities(getBlockPos());
          }
        }
      };

  public void setOwner(Player player) {
    this.ownerID = player.getUUID();
    this.ownerUser = player.getName().getString();
    setChanged();
  }

  public UUID getOwnerID() {
    return this.ownerID;
  }

  public String getOwnerUser() {
    return this.ownerUser;
  }

  public boolean hasOwner() {
    return this.ownerID != null || this.ownerUser != null;
  }

  public boolean isOwner(Player player) {
    if (this.ownerID != null && this.ownerID.equals(player.getUUID())) {
      return true;
    }

    if (this.ownerUser != null && this.ownerUser.equals(player.getName().getString())) {
      this.ownerID = player.getUUID();
      setChanged();
      return true;
    }

    return false;
  }

  public void drops() {
    SimpleContainer inv = new SimpleContainer(inventory.getSlots());
    for (int i = 0; i < inventory.getSlots(); i++) {
      if (i != 1) {
        inv.setItem(i, inventory.getStackInSlot(i));
      }
    }
    Containers.dropContents(this.level, this.worldPosition, inv);
  }

  @Override
  protected void saveAdditional(
      CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.put("inventory", inventory.serializeNBT(registries));
    if (this.ownerID != null) tag.putUUID("ownerID", this.ownerID);
    if (this.ownerUser != null) tag.putString("ownerUser", this.ownerUser);
  }

  @Override
  protected void loadAdditional(
      CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    if (tag.hasUUID("ownerID")) this.ownerID = tag.getUUID("ownerID");
    if (tag.contains("ownerUser")) this.ownerUser = tag.getString("ownerUser");
  }

  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
    return this.saveWithoutMetadata(registries);
  }

  public void updateOwnershipInfo(Player player) {
    if (this.ownerUser != null && this.ownerID == null) {
      if (this.ownerUser.equals(player.getName().getString())) {
        this.ownerID = player.getUUID();
        setChanged();
        if (!level.isClientSide()) {
          level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
      }
    }
  }
}
