package com.roll_54.roll_mod_client.compat.fusion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionDirection;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.roll_54.roll_mod_client.compat.fusion.ConnectionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes the neighbour's position available to connection predicates (see {@link ConnectionContext}).
 */
@Mixin(targets = "com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureType", remap = false)
public abstract class ConnectingTextureTypeMixin {

    @WrapOperation(
        method = "shouldConnect",
        at = @At(
            value = "INVOKE",
            target = "Lcom/supermartijn642/fusion/api/texture/types/connecting/predicates/ConnectionPredicate;shouldConnect(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lcom/supermartijn642/fusion/api/texture/types/connecting/predicates/ConnectionDirection;)Z"
        )
    )
    private static boolean roll_mod$withNeighborPos(ConnectionPredicate predicate, BlockGetter level, BlockPos pos, Direction side, BlockState ownState, BlockState otherState, BlockState blockInFront, ConnectionDirection direction, Operation<Boolean> operation, @Local(argsOnly = true) BlockPos.MutableBlockPos neighborPos){
        BlockPos previous = ConnectionContext.set(neighborPos);
        try{
            return operation.call(predicate, level, pos, side, ownState, otherState, blockInFront, direction);
        }finally{
            ConnectionContext.set(previous);
        }
    }
}
