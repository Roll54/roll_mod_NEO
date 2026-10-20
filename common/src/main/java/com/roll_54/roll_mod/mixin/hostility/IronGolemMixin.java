package com.roll_54.roll_mod.mixin.hostility;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.roll_54.roll_mod.compat.createcybernetics.ConfigurableHostility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * Iron golems hunt every {@code Enemy} on sight and on bump. A {@link ConfigurableHostility} entity
 * configured passive is neither; retaliation ({@code HurtByTargetGoal}) is untouched.
 */
@Mixin(IronGolem.class)
public abstract class IronGolemMixin {

    /**
     * Both 6-arg target goals in {@code registerGoals} pass through here — the player one too, which
     * a passive check cannot affect. The wrapped predicate is evaluated per scan, so it stays live.
     */
    @ModifyArg(
            method = "registerGoals",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/goal/target/NearestAttackableTargetGoal;<init>(Lnet/minecraft/world/entity/Mob;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V"
            ),
            index = 5
    )
    private Predicate<LivingEntity> roll_mod$skipPassive(Predicate<LivingEntity> original) {
        return original == null ? null : e -> !ConfigurableHostility.isPassive(e) && original.test(e);
    }

    @WrapWithCondition(
            method = "doPush",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/IronGolem;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"
            )
    )
    private boolean roll_mod$noBumpAggroOnPassive(IronGolem golem, LivingEntity target, Entity pushed) {
        return !ConfigurableHostility.isPassive(pushed);
    }
}
