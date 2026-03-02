package com.roll_54.roll_mod.modItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

import java.util.List;

public class RocketItem extends Item {

    private final int tier;

    public RocketItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    /**
     * Bakes the rocket_tier into CUSTOM_DATA on every new stack so the
     * packet handler can read it without knowing which item class it is.
     */
    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {
        super.verifyComponentsAfterLoad(stack);
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        if (existing == null || !existing.copyTag().contains("rocket_tier")) {
            CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
            tag.putInt("rocket_tier", tier);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tier " + tier + " Rocket")
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        tooltip.add(Component.literal("Required fuel: " + (5 * tier))
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
