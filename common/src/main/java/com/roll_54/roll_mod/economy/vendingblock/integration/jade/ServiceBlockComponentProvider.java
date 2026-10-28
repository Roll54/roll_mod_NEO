package com.roll_54.roll_mod.economy.vendingblock.integration.jade;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.service.ServiceBlock;
import com.roll_54.roll_mod.economy.service.ServiceBlockEntity;
import com.roll_54.roll_mod.economy.service.ServiceMessages;
import com.roll_54.roll_mod.economy.service.ServiceType;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * The service block's service, price, stock and owner. Read from the client's copy of the block
 * entity, which the block's update packet keeps whole, so no server data provider is needed.
 */
public enum ServiceBlockComponentProvider implements IBlockComponentProvider {
  INSTANCE;

  @Override
  public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
    // The upper half has no block entity: read the lower half's.
    BlockPos lower = ServiceBlock.lowerHalf(accessor.getBlockState(), accessor.getPosition());
    if (!(accessor.getLevel().getBlockEntity(lower) instanceof ServiceBlockEntity be)) return;
    ServiceType type = be.getServiceType();
    if (type == null) {
      tooltip.add(Component.translatable("jade.roll_mod.service.not_set_up").withStyle(ChatFormatting.RED));
    } else {
      tooltip.add(Component.translatable("jade.roll_mod.service.type", type.title().copy().withStyle(ChatFormatting.GOLD)));
      tooltip.add(Component.translatable("gui.roll_mod.service.price",
          VendorUIHelper.priceText(be.getPrice(), "menu.roll_mod.buy.free"), be.batchText()));
      if (be.isUnlimited()) {
        tooltip.add(Component.translatable("gui.roll_mod.service.unlimited").withStyle(ChatFormatting.AQUA));
      } else {
        tooltip.add(type.sells()
            ? Component.translatable("gui.roll_mod.service.stock", ServiceMessages.amount(type, ServiceMessages.stock(type, be)))
            : Component.translatable("gui.roll_mod.service.room", ServiceMessages.amount(type, ServiceMessages.room(type, be))));
      }
    }
    if (be.getOwnerUser() != null) {
      tooltip.add(Component.translatable("jade.roll_mod.owner", be.getOwnerUser()));
    }
  }

  @Override
  public ResourceLocation getUid() {
    return RollMod.id("service_component");
  }
}
