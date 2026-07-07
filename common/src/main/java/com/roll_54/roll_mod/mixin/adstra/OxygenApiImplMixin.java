package com.roll_54.roll_mod.mixin.adstra;

import com.roll_54.roll_mod.util.NanoSuitUtils;
import earth.terrarium.adastra.common.systems.OxygenApiImpl;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops Ad Astra from dealing oxygen/suffocation damage to a wearer of a full Extended
 * Industrialization nano suit. Ad Astra's {@code entityTick} only recognizes its own
 * {@code SpaceSuitItem}/armor tags, so it hurts the nano wearer in no-oxygen dimensions.
 *
 * <p>Protection mirrors a real space suit: it applies only while the chestplate's oxygen tank still
 * holds oxygen. When the tank runs dry the wearer takes damage again (must refill). Oxygen is drained
 * by {@code NanoArmorMixin#armorTick}, so this mixin only cancels the damage — it does not consume.
 */
@Mixin(OxygenApiImpl.class)
public abstract class OxygenApiImplMixin {

    @Inject(method = "entityTick", at = @At("HEAD"), cancellable = true)
    private void roll_mod$nanoSuitPreventsOxygenDamage(ServerLevel level, LivingEntity entity, CallbackInfo ci) {
        if (NanoSuitUtils.hasFullNanoSet(entity) && NanoSuitUtils.chestHasOxygen(entity)) {
            ci.cancel();
        }
    }
}
