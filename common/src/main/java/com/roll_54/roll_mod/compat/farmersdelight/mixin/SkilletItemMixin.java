package com.roll_54.roll_mod.compat.farmersdelight.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vectorwing.farmersdelight.common.item.SkilletItem;

/**
 * COOK hook, hand-held skillet. When the skillet finishes, {@code finishUsingItem} hands the
 * campfire recipe to a lambda that assembles the cooked item and gives it to the player; the
 * assembled stack is observed there, untouched.
 *
 * <p>Targets the synthetic {@code lambda$finishUsingItem$0} of Farmer's Delight 1.3.2. The config is
 * {@code required = false}, so if a later FD renames it this one hook is skipped, not the game.
 */
@Mixin(SkilletItem.class)
public abstract class SkilletItemMixin {

    @ModifyExpressionValue(
            method = "lambda$finishUsingItem$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/CampfireCookingRecipe;assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private static ItemStack roll_mod$countSkilletCooked(ItemStack cooked, @Local(argsOnly = true) Player player) {
        if (!cooked.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            DailyTaskManager.progress(serverPlayer, DailyTaskHook.COOK, cooked, cooked.getCount());
        }
        return cooked;
    }
}
