package com.roll_54.roll_mod.compat.farmersdelight.mixin;

import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskEvents;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

/**
 * COOK hook, cooking pot: counts a dish the moment it finishes cooking, credited to the pot's owner
 * (whoever placed it or last opened it — see {@link DailyTaskEvents#progressForOwner}).
 *
 * <p>Counting when a player takes the dish out missed too much: a pot left to cook, output pulled by
 * hoppers, meals that sit in the display slot. In {@code processCooking}, {@code setRecipeUsed} runs
 * only after the finished dish has been put into the meal slot, so it marks exactly one cooked batch.
 * (The bowls or buckets a pot throws out at that moment are ingredient leftovers, which Farmer's
 * Delight ejects on purpose.)
 */
@Mixin(CookingPotBlockEntity.class)
public abstract class CookingPotBlockEntityMixin {

    @Inject(
            method = "processCooking",
            at = @At(
                    value = "INVOKE",
                    target = "Lvectorwing/farmersdelight/common/block/entity/CookingPotBlockEntity;setRecipeUsed(Lnet/minecraft/world/item/crafting/RecipeHolder;)V"
            )
    )
    private void roll_mod$countCooked(RecipeHolder<CookingPotRecipe> recipe, CookingPotBlockEntity pot,
                                      CallbackInfoReturnable<Boolean> cir) {
        Level level = pot.getLevel();
        if (recipe == null || level == null || level.isClientSide()) return;
        DailyTaskEvents.progressForOwner(pot, DailyTaskHook.COOK,
                recipe.value().getResultItem(level.registryAccess()));
    }
}
