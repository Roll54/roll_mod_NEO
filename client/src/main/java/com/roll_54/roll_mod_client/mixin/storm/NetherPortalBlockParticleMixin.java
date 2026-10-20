package com.roll_54.roll_mod_client.mixin.storm;

import com.roll_54.roll_mod_client.client.storm.StormPortalClient;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brackets the portal's ambient particle spawning, so {@link PortalParticleColorMixin} can tell a
 * nether portal's particles apart from the ender pearl / chorus fruit ones sharing the same type.
 */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockParticleMixin {

    @Inject(method = "animateTick", at = @At("HEAD"))
    private void roll_mod$beginPortalParticles(BlockState state, Level level, BlockPos pos, RandomSource random,
                                               CallbackInfo ci) {
        StormPortalClient.spawningPortalParticles = true;
    }

    @Inject(method = "animateTick", at = @At("RETURN"))
    private void roll_mod$endPortalParticles(BlockState state, Level level, BlockPos pos, RandomSource random,
                                             CallbackInfo ci) {
        StormPortalClient.spawningPortalParticles = false;
    }
}
