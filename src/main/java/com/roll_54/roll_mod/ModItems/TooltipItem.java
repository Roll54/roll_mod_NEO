package com.roll_54.roll_mod.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Item, який додає N рядків опису (lore) з ключами виду:
 * loreitem.roll_mod.<path>.tooltip_line<1..N>
 *
 * <path> береться з реєстраційного імені предмета (наприклад, "supersteel_gear").
 */
public class TooltipItem extends Item {
    private final int tooltipLines;

    public TooltipItem(Properties props, int tooltipLines) {
        super(props);
        if (tooltipLines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
        this.tooltipLines = tooltipLines;
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        // Дістаємо registry name предмета: namespace:path
        // і використовуємо тільки path для складання ключів локалізації.
        ResourceLocation key = stack.getItem().builtInRegistryHolder().key().location();
        String path = key.getPath(); // напр. "supersteel_gear"

        for (int i = 1; i <= tooltipLines; i++) {
            String locKey = "loreitem.roll_mod." + path + ".tooltip_line" + i;
            tooltip.add(Component.translatable(locKey));
        }
    }
}
