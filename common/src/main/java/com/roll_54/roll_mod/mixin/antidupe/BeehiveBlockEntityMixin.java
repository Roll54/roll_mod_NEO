package com.roll_54.roll_mod.mixin.antidupe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin extends BlockEntity {

    public BeehiveBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(
            method = "addOccupant",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tryEnterHiveIfLoaded(Entity entity, CallbackInfo ci) {

        if (!entity.level().hasChunk(
                SectionPos.blockToSectionCoord(this.getBlockPos().getX()),
                SectionPos.blockToSectionCoord(this.getBlockPos().getZ())
        )) {
            ci.cancel();
        }
    }
}