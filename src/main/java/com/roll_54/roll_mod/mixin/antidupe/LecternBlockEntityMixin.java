package com.roll_54.roll_mod.mixin.antidupe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin extends BlockEntity {

    public LecternBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(
            method = "onBookItemRemove()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/LecternBlock;resetBookState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void onBookRemovedCheckIfBlockStillThere(CallbackInfo ci) {


        if (!this.isRemoved() && this.level != null) {
            BlockState state = this.level.getBlockState(this.getBlockPos());
            if (state.is(Blocks.LECTERN)) {
                LecternBlock.resetBookState(null, this.level, this.getBlockPos(), state, false);
            }
        }
        ci.cancel();
    }
}