package com.roll_54.roll_mod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskEvents;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds the daily-task SMELT hook at the moment an item is produced, rather than when a player takes
 * it — so output pulled out by hoppers and pipes counts as well.
 *
 * <p>In {@code serverTick}, {@code setRecipeUsed} runs only right after {@code burn} has returned
 * {@code true}, i.e. exactly once per smelted item, for furnaces, smokers and blast furnaces alike.
 * Credit goes to the furnace's owner — see {@link DailyTaskEvents#progressForOwner}.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Inject(
            method = "serverTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;setRecipeUsed(Lnet/minecraft/world/item/crafting/RecipeHolder;)V"
            )
    )
    private static void roll_mod$countSmelted(Level level, BlockPos pos, BlockState state,
                                              AbstractFurnaceBlockEntity furnace, CallbackInfo ci,
                                              @Local RecipeHolder<?> recipe) {
        if (recipe == null) return;
        DailyTaskEvents.progressForOwner(furnace, DailyTaskHook.SMELT,
                recipe.value().getResultItem(level.registryAccess()));
    }
}
