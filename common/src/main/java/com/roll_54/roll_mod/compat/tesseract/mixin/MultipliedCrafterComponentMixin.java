package com.roll_54.roll_mod.compat.tesseract.mixin;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.AbstractModularCrafterComponent;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.ModularCrafterAccessBehavior;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.MultipliedCrafterComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Tesseract's multiplied crafters (Extended Industrialization's processing array and other "multiplied" multiblocks)
 * computed the recipe multiplier for every candidate recipe before checking whether the recipe could start at all.
 * The multiplier is a binary search over simulated output inserts plus input counting (1.2% of the live server thread,
 * 1.6% in lag ticks), and most candidates fail on missing inputs anyway.
 * <p>
 * A recipe that cannot start at x1 cannot start at any multiplier (inputs and outputs scale with it, conditions do
 * not depend on it), so x1 is checked first and the multiplier is only computed for recipes that pass. The result is
 * the same; failing candidates skip the search.
 */
@Mixin(value = MultipliedCrafterComponent.class, remap = false)
public abstract class MultipliedCrafterComponentMixin extends AbstractModularCrafterComponent<RecipeHolder<MachineRecipe>> {
    @Shadow
    private int tryRecipeMultiplier;

    private MultipliedCrafterComponentMixin(MachineBlockEntity blockEntity, CrafterComponent.Inventory inventory, ModularCrafterAccessBehavior behavior) {
        super(blockEntity, inventory, behavior);
    }

    @WrapMethod(method = "canStartRecipe(Lnet/minecraft/world/item/crafting/RecipeHolder;Z)Z")
    private boolean roll_mod$checkSingleFirst(RecipeHolder<MachineRecipe> recipe, boolean ignoreConditions, Operation<Boolean> original) {
        boolean single;
        this.tryRecipeMultiplier = 1;
        try {
            single = super.canStartRecipe(recipe, ignoreConditions);
        } finally {
            this.tryRecipeMultiplier = 0;
        }
        return single && original.call(recipe, ignoreConditions);
    }
}
