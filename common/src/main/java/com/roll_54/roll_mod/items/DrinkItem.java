package com.roll_54.roll_mod.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * A food that is drunk, not eaten: the drinking animation and sound. What is left after it (a glass
 * bottle, say) comes from its {@link net.minecraft.world.food.FoodProperties}' {@code usingConvertsTo}.
 */
public class DrinkItem extends Item {

    public DrinkItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
