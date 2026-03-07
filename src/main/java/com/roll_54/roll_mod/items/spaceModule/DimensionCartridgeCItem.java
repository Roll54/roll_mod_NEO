package com.roll_54.roll_mod.items.spaceModule;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import static com.roll_54.roll_mod.data.RMMComponents.CARTRIDGE_DATA;

public class DimensionCartridgeCItem extends Item {

    public DimensionCartridgeCItem(Properties properties, String dimensionRaw, String nameKey, int fuelAmount, int requiredTier) {
        super(properties.component(CARTRIDGE_DATA.get(), new CartridgeData(dimensionRaw, nameKey, fuelAmount, requiredTier)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CartridgeData data = stack.get(CARTRIDGE_DATA.get());
        if (data != null) {
            tooltip.add(Component.translatable("cartridge.destination", Component.translatable(data.name()))
                    .withStyle(net.minecraft.ChatFormatting.AQUA));
            tooltip.add(Component.translatable("cartridge.required_tier", data.requiredTier())
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            tooltip.add(Component.translatable("cartridge.fuel_required", data.fuelAmount())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
