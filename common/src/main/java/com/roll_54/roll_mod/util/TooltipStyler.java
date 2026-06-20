package com.roll_54.roll_mod.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;


public final class TooltipStyler {
    private TooltipStyler() {}

    public static Component colorName(Component base, Integer nameColorHex) {
        if (nameColorHex == null) return base;
        return base.copy().withStyle(s -> s.withColor(nameColorHex));
    }

    /** Додає N локалізованих рядків лору для itemId: loreitem.roll_mod.<path>.tooltip_line1..N */
    public static void addLore(ItemStack stack, int tooltipLines, Integer loreColorHex,
                               List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation key = stack.getItem().builtInRegistryHolder().key().location();
        String path = key.getPath();

        for (int i = 1; i <= tooltipLines; i++) {
            String locKey = "loreitem.roll_mod." + path + ".tooltip_line" + i;
            MutableComponent line = Component.translatable(locKey);
            if (loreColorHex != null) {
                line = line.withStyle(s -> s.withColor(loreColorHex));
            }
            tooltip.add(line);
        }
    }
}