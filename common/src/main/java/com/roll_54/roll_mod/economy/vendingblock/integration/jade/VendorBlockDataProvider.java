package com.roll_54.roll_mod.economy.vendingblock.integration.jade;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum VendorBlockDataProvider implements IServerDataProvider<BlockAccessor> {
  INSTANCE;

  @Override
  public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
    if (accessor.getBlockEntity() instanceof VendorBlockEntity entity) {
      String ownerUser = entity.getOwnerUser();
      if (ownerUser != null) tag.putString("owner", ownerUser);

      tag.putInt("positionCount", entity.getPositions().size());

      tag.putBoolean("hasError", entity.hasError);
      tag.putInt("errorCode", entity.errorCode);
    }
  }

  @Override
  public ResourceLocation getUid() {
    return RollMod.id("vendor_data");
  }
}
