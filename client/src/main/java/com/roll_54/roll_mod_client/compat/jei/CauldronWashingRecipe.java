package com.roll_54.roll_mod_client.compat.jei;

import com.roll_54.roll_mod.registry.RecipeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * One cauldron wash as JEI and EMI show it, read from a {@code roll_mod:cauldron_washing} recipe
 * ({@link com.roll_54.roll_mod.recipe.CauldronWashingRecipe}), so recipes added or removed by datapacks and KubeJS
 * show up as they are.
 */
public final class CauldronWashingRecipe {
    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack output;
    private final List<Item> byproducts;
    private final float dustDropChance;

    public CauldronWashingRecipe(ResourceLocation id, Ingredient ingredient, ItemStack output, List<Item> byproducts,
                                 float dustDropChance) {
        this.id = id;
        this.ingredient = ingredient;
        this.output = output;
        this.byproducts = byproducts;
        this.dustDropChance = dustDropChance;
    }

    /** Every cauldron-washing recipe the client knows (recipes are synced from the server). */
    public static List<CauldronWashingRecipe> collect() {
        List<CauldronWashingRecipe> recipes = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return recipes;
        for (RecipeHolder<com.roll_54.roll_mod.recipe.CauldronWashingRecipe> holder : mc.getConnection()
                .getRecipeManager().getAllRecipesFor(RecipeRegistry.CAULDRON_WASHING_TYPE.get())) {
            var r = holder.value();
            recipes.add(new CauldronWashingRecipe(holder.id(), r.ingredient(), r.result(), r.byproducts(),
                    r.byproductChance()));
        }
        return recipes;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    /** The items the ingredient takes. */
    public List<ItemStack> getInputs() {
        return List.of(ingredient.getItems());
    }

    public ItemStack getOutput() {
        return output;
    }

    public List<Item> getByproducts() {
        return byproducts;
    }

    public float getDustDropChance() {
        return dustDropChance;
    }
}
