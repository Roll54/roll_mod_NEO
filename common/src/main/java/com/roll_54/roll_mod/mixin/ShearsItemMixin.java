package com.roll_54.roll_mod.mixin;

import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feeds the daily-task SHEAR hook. NeoForge posts no shearing event; it routes every mob shearing
 * through {@link ShearsItem#interactLivingEntity}, which calls {@code IShearable.onSheared} only once
 * {@code isShearable} has agreed — so that call is the proof a shear happened.
 *
 * <p>A mooshroom has already been swapped for a cow by the time {@code onSheared} returns, but
 * {@code entity} is still the old mooshroom instance, so its type is reported correctly. Dispensers
 * shear through a different path with no player, and are deliberately not counted.
 */
@Mixin(ShearsItem.class)
public abstract class ShearsItemMixin {

    @Inject(
            method = "interactLivingEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/IShearable;onSheared(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Ljava/util/List;",
                    shift = At.Shift.AFTER
            )
    )
    private void roll_mod$countShear(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer serverPlayer) {
            DailyTaskManager.progress(serverPlayer, DailyTaskHook.SHEAR, entity, 1);
        }
    }
}
