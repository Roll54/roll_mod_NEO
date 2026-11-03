package com.roll_54.roll_mod_client.compat.emi.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod_client.compat.emi.tree.RecipeTreePicker;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.bom.MaterialTree;
import dev.emi.emi.screen.widget.ResolutionButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The stack button beside EMI's recipe screen while resolving means "no recipe, keep it a base
 * resource", and it writes straight into EMI's own tree ({@code BoM.tree}) instead of going through
 * {@code BoM}. While the roll_mod recipe tree is picking, it clears that node's recipe there instead; and
 * it never touches EMI's tree when there is none (EMI itself would crash on that).
 */
@Mixin(value = ResolutionButtonWidget.class, remap = false)
public abstract class ResolutionButtonPickMixin {

    @WrapOperation(method = "lambda$new$0", at = @At(value = "INVOKE",
            target = "Ldev/emi/emi/bom/MaterialTree;addResolution(Ldev/emi/emi/api/stack/EmiIngredient;Ldev/emi/emi/api/recipe/EmiRecipe;)V"))
    private static void roll_mod$keepAsBaseResource(MaterialTree tree, EmiIngredient stack, EmiRecipe recipe,
                                                    Operation<Void> original) {
        if (RecipeTreePicker.offerNone()) return;
        if (tree != null) original.call(tree, stack, recipe);
    }
}
