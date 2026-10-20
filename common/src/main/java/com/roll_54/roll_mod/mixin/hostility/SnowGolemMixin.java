package com.roll_54.roll_mod.mixin.hostility;

import com.roll_54.roll_mod.compat.createcybernetics.ConfigurableHostility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/** Snow golems pelt every {@code Enemy}; not a {@link ConfigurableHostility} one configured passive. */
@Mixin(SnowGolem.class)
public abstract class SnowGolemMixin {

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
}
