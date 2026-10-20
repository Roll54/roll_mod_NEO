package com.roll_54.roll_mod.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes what a smithing transform recipe takes and makes. {@code getIngredients()} is empty for
 * it, so the minimal price computation (economy/minprice) would otherwise not see netherite gear's
 * recipe at all.
 */
@Mixin(SmithingTransformRecipe.class)
public interface SmithingTransformRecipeAccessor {

    @Accessor("template")
    Ingredient roll_mod$getTemplate();

    @Accessor("base")
    Ingredient roll_mod$getBase();

    @Accessor("addition")
    Ingredient roll_mod$getAddition();

    @Accessor("result")
    ItemStack roll_mod$getResult();
}
