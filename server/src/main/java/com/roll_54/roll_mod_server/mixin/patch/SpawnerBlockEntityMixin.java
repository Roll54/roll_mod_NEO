package com.roll_54.roll_mod_server.mixin.patch;


import com.roll_54.roll_mod_server.util.SpawnerChestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpawnerBlockEntity.class)
public class SpawnerBlockEntityMixin {

    // Vanilla's spawner tick never runs for a block spawner. With no chest above it is inert; with a
    // chest above, SpawnerChestHelper.tick replaces it with a drop generator, so none of vanilla's
    // spawn gates (player range, light, liquid, obstruction, crowd cap, spawn events) apply.
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$replaceTick(Level level, BlockPos pos, BlockState state,
                                             SpawnerBlockEntity blockEntity, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        IItemHandler dest = SpawnerChestHelper.containerAbove(serverLevel, pos);
        if (dest != null) {
            SpawnerChestHelper.tick(serverLevel, pos, blockEntity.getSpawner(), dest);
        }
        ci.cancel();
    }
}
