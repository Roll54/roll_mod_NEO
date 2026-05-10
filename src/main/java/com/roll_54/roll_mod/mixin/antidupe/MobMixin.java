package com.roll_54.roll_mod.mixin.antidupe;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {

    protected MobMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
            method = "removeAfterChangingDimensions()V", // Mojang аналог removeFromDimension
            at = @At("RETURN")
    )
    protected void stopGeneralItemDupes(CallbackInfo ci) {

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.getItemBySlot(slot).setCount(0);
        }
    }
}