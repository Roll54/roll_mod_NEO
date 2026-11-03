package com.roll_54.roll_mod_client.compat.emi.tree;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Picks a recipe for one node of the {@link RecipeTreeScreen} through EMI's own recipe screen, the way
 * EMI's recipe tree does: the recipe screen is opened in "resolve" mode ({@link RecipeScreen#resolve}),
 * which gives every recipe its pick button and makes EMI's "view recipes" / "set default" keys on a recipe
 * pick it. EMI then hands the pick to its own tree ({@code BoM}) and goes back a screen;
 * {@code BoMRecipeTreePickMixin} catches that call and sends the recipe here instead.
 */
public final class RecipeTreePicker {

    private static @Nullable RecipeTreeScreen owner;
    private static @Nullable EmiIngredient target;

    private RecipeTreePicker() {}

    /** Opens EMI's recipes for {@code ingredient}; false when EMI has none to show. */
    static boolean pick(RecipeTreeScreen screen, EmiIngredient ingredient) {
        Minecraft mc = Minecraft.getInstance();
        EmiApi.displayRecipes(ingredient);
        if (!(mc.screen instanceof RecipeScreen recipeScreen)) return false;

        owner = screen;
        target = ingredient;
        RecipeScreen.resolve = ingredient;
        // Same as EMI's BoMScreen: the first init ran before resolve was set, so the pick buttons are missing.
        recipeScreen.init(mc, recipeScreen.width, recipeScreen.height);
        return true;
    }

    /**
     * Called by the mixin for every recipe EMI is about to store in its own tree. True when the recipe
     * screen was opened by {@link #pick}, in which case the recipe went to the roll_mod tree and EMI's
     * call must be cancelled.
     */
    public static boolean offer(EmiRecipe recipe) {
        if (owner == null || target == null || RecipeScreen.resolve != target) return false;
        owner.choose(target, recipe);
        target = null;
        return true;
    }

    /** EMI's "no recipe" button while picking: the node's recipe is removed. True when it was ours. */
    public static boolean offerNone() {
        if (owner == null || target == null || RecipeScreen.resolve != target) return false;
        owner.unpick(target);
        target = null;
        return true;
    }
}
