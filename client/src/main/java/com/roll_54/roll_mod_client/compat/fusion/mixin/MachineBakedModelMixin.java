package com.roll_54.roll_mod_client.compat.fusion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Lets MI machines render a casing model that depends on the world, such as a Fusion connecting model,
 * so machine casings connect to the casing blocks around them.
 */
@Mixin(targets = "aztech.modern_industrialization.client.machines.models.MachineBakedModel", remap = false)
public abstract class MachineBakedModelMixin {

    /**
     * MI replaces its own model data by whatever the casing model returns. Fusion returns fresh model data,
     * which drops the machine's facing, casing and active state. Keep both.
     */
    @WrapOperation(
        method = "getModelData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/BakedModel;getModelData(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/neoforged/neoforge/client/model/data/ModelData;)Lnet/neoforged/neoforge/client/model/data/ModelData;"
        )
    )
    private ModelData roll_mod$keepMachineData(BakedModel casingModel, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData machineData, Operation<ModelData> operation){
        ModelData casingData = operation.call(casingModel, level, pos, state, machineData);
        if(casingData == machineData)
            return machineData;
        ModelData.Builder merged = machineData.derive();
        for(ModelProperty<?> property : casingData.getProperties())
            roll_mod$copy(merged, casingData, property);
        return merged.build();
    }

    private static <T> void roll_mod$copy(ModelData.Builder builder, ModelData from, ModelProperty<T> property){
        builder.with(property, from.get(property));
    }

    /**
     * MI asks the casing model for its quads in MI's own render type (cutout mipped), while Fusion sorts quads
     * by the block's default render type and returns nothing for others. Ask for all quads instead; MI draws
     * them in its own render type anyway.
     */
    @ModifyArg(
        method = "getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/BakedModel;getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;"
        ),
        index = 4
    )
    private RenderType roll_mod$allCasingQuads(RenderType renderType){
        return null;
    }
}
