package com.roll_54.roll_mod_client.compat.tei.tree;

import dev.lorp.tei.api.TeiDisplay;
import dev.lorp.tei.client.gui.RecipeScreen;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Picks a recipe for one node of the {@link TeiRecipeTreeScreen} through TEI's own recipe screen, the way TEI's
 * recipe tree does: {@link RecipeScreen#openResolve} opens it in "resolve" mode, where choosing a recipe's output,
 * the heart (default recipe) button or key, or the "no recipe" button beside it hands the choice to TEI's tree
 * ({@code RecipeTreeScreen.setResolution}) or default recipes ({@code DefaultRecipes.toggle*}) and goes back a
 * screen. The mixins in {@code compat.tei.mixin} catch those calls while this picker is waiting and send the
 * recipe here instead, so neither TEI's tree nor its hearts change.
 */
public final class TeiRecipeTreePicker {

    private static @Nullable TeiRecipeTreeScreen owner;
    private static @Nullable TreeIngredient target;
    /** TEI's {@code RecipeScreen.resolve} as set by our {@link #pick}; the pick is ours while it is still that. */
    private static @Nullable ItemStack marker;
    private static @Nullable Field resolveField;
    private static boolean resolveFieldMissing;

    private TeiRecipeTreePicker() {}

    /** Opens TEI's recipes for {@code ingredient} (a single stack) in resolve mode; false when TEI has none. */
    static boolean pick(TeiRecipeTreeScreen screen, TreeIngredient ingredient, @Nullable TeiDisplay current) {
        if (!ingredient.single()) return false;
        if (!RecipeScreen.openResolve(ingredient.first(), current)) return false;
        owner = screen;
        target = ingredient;
        marker = currentResolve();
        return true;
    }

    /**
     * Called by the mixins for every recipe TEI is about to store in its own tree or as a default (null: "no
     * recipe"). True when the recipe screen was opened by {@link #pick}; the choice then went to the roll_mod tree
     * and TEI's call must be cancelled.
     */
    public static boolean offer(@Nullable TeiDisplay recipe) {
        if (owner == null || target == null || marker == null || currentResolve() != marker) return false;
        if (recipe == null) owner.unpick(target);
        else owner.choose(target, recipe);
        target = null;
        marker = null;
        return true;
    }

    /** TEI's {@code RecipeScreen.resolve} (package-private): the stack whose recipe is being chosen, or null. */
    private static @Nullable ItemStack currentResolve() {
        if (resolveFieldMissing) return null;
        try {
            if (resolveField == null) {
                resolveField = RecipeScreen.class.getDeclaredField("resolve");
                resolveField.setAccessible(true);
            }
            return (ItemStack) resolveField.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            resolveFieldMissing = true;
            return null;
        }
    }
}
