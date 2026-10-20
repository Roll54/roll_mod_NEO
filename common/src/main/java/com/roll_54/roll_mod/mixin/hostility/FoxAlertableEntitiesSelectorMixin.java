package com.roll_54.roll_mod.mixin.hostility;

import com.roll_54.roll_mod.compat.createcybernetics.ConfigurableHostility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sleeping foxes wake for any {@code Monster} nearby; not a {@link ConfigurableHostility} one configured passive. */
@Mixin(Fox.FoxAlertableEntitiesSelector.class)
public abstract class FoxAlertableEntitiesSelectorMixin {

    @Inject(method = "test(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void roll_mod$ignorePassive(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ConfigurableHostility.isPassive(entity)) {
            cir.setReturnValue(false);
        }
    }
}
