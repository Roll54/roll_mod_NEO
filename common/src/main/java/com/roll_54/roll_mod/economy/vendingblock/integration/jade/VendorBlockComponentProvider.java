package com.roll_54.roll_mod.economy.vendingblock.integration.jade;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.VendorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum VendorBlockComponentProvider implements IBlockComponentProvider {
  INSTANCE;

  @Override
  public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
    if (accessor.getBlockEntity() instanceof VendorBlockEntity) {
      CompoundTag serverData = accessor.getServerData();

      int positionCount = serverData.getInt("positionCount");
      if (positionCount > 0) {
        tooltip.add(Component.translatable("jade.roll_mod.positions", positionCount));
      }

      if (serverData.contains("owner"))
        tooltip.add(
            Component.translatable("jade.roll_mod.owner", serverData.getString("owner")));

      if (serverData.contains("hasError")) {
        boolean hasError = serverData.getBoolean("hasError");
        int errorCode = serverData.getInt("errorCode");
        if (hasError) {
          switch (errorCode) {
            case 1:
              tooltip.add(
                  Component.translatable("jade.roll_mod.error.sold")
                      .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xba3c3c))));
              break;
            case 2:
              tooltip.add(
                  Component.translatable("jade.roll_mod.error.full")
                      .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xba3c3c))));
              break;
            case 3:
              tooltip.add(
                  Component.translatable("jade.roll_mod.error.empty")
                      .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xba3c3c))));
              break;
            default:
              break;
          }
        }
      }
    }
  }

  @Override
  public ResourceLocation getUid() {
    return RollMod.id("vendor_component");
  }
}
