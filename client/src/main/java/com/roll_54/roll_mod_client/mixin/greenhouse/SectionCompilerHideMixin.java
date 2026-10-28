package com.roll_54.roll_mod_client.mixin.greenhouse;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.roll_54.roll_mod.compat.MBD2.greenhouse.HiddenStructureBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Leaves the blocks of a formed EV Greenhouse out of the chunk mesh ({@link HiddenStructureBlocks}),
 * so only the controller's model shows. The blocks stay in the world untouched; only their drawing is
 * skipped, and their section is rebuilt whenever the greenhouse forms or breaks.
 */
@Mixin(SectionCompiler.class)
public class SectionCompilerHideMixin {

    @WrapOperation(
            method = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"))
    private void roll_mod$skipHiddenGreenhouseBlocks(BlockRenderDispatcher dispatcher, BlockState state, BlockPos pos,
                                                     BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer,
                                                     boolean checkSides, RandomSource random, ModelData modelData,
                                                     RenderType renderType, Operation<Void> original) {
        if (HiddenStructureBlocks.isHidden(pos)) return;
        original.call(dispatcher, state, pos, level, poseStack, consumer, checkSides, random, modelData, renderType);
    }
}
