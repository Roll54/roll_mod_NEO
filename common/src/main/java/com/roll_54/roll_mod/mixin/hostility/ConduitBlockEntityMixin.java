package com.roll_54.roll_mod.mixin.hostility;

import com.roll_54.roll_mod.compat.createcybernetics.ConfigurableHostility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/** A full conduit zaps any wet {@code Enemy} nearby; not a {@link ConfigurableHostility} one configured passive. */
@Mixin(ConduitBlockEntity.class)
public abstract class ConduitBlockEntityMixin {

    @ModifyArg(
            method = "updateDestroyTarget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            index = 2
    )
    private static Predicate<Entity> roll_mod$skipPassive(Predicate<Entity> original) {
        return e -> !ConfigurableHostility.isPassive(e) && original.test(e);
    }
}
