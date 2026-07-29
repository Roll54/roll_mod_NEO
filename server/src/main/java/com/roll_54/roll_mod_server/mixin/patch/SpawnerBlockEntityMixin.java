package com.roll_54.roll_mod_server.mixin.patch;


import com.roll_54.roll_mod_server.util.SpawnerChestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnerBlockEntity.class)
public class SpawnerBlockEntityMixin {

    // A mob spawner is inert unless a chest sits directly above it: with no chest, cancel the tick
    // entirely so it never spawns mobs (BaseSpawnerMixin turns the chest-present case into a drop
    // generator).
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$onlyRunWithChestAbove(Level level, BlockPos pos, BlockState state,
                                                       SpawnerBlockEntity blockEntity, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && !SpawnerChestHelper.hasContainerAbove(serverLevel, pos)) {
            ci.cancel();
        }
    }
}
