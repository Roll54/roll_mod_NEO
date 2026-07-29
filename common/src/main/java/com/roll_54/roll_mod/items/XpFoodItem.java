package com.roll_54.roll_mod.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A food item that grants experience points to the player when fully eaten.
 * The nutrition / saturation / effects are supplied through the vanilla
 * {@link net.minecraft.world.food.FoodProperties} on the item {@link Properties};
 * this class only adds the extra XP reward on top of normal eating.
 */
public class XpFoodItem extends Item {

    private final int experiencePoints;

    public XpFoodItem(Properties properties, int experiencePoints) {
        super(properties);
        this.experiencePoints = experiencePoints;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // Let vanilla apply nutrition, saturation and any FoodProperties effects first.
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide && entity instanceof Player player && experiencePoints > 0) {
            player.giveExperiencePoints(experiencePoints);
        }

        return result;
    }
}
