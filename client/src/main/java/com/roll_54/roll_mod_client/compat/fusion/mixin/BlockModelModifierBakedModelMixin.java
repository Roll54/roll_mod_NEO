package com.roll_54.roll_mod_client.compat.fusion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes Fusion block model modifiers on blocks whose model depends on block entity data,
 * such as Modern Industrialization machines.
 */
@Mixin(value = BlockModelModifierBakedModel.class, remap = false)
public abstract class BlockModelModifierBakedModelMixin {

    @Shadow
    @Final
    private BakedModel original;
    @Shadow
    @Final
    private TextureAtlasSprite particleSprite;

    /**
     * Fusion asks the wrapped model for its particle icon while models are still being baked.
     * Models that look up other baked models (MI machines look up their casing model) throw at that point,
     * which fails the whole resource reload. Defer the lookup instead.
     */
    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/BakedModel;getParticleIcon()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
        )
    )
    private TextureAtlasSprite roll_mod$deferParticleIcon(BakedModel model, Operation<TextureAtlasSprite> operation){
        try{
            return operation.call(model);
        }catch(RuntimeException e){
            return null;
        }
    }

    @Inject(
        method = "getParticleIcon()Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void roll_mod$resolveParticleIcon(CallbackInfoReturnable<TextureAtlasSprite> cir){
        if(this.particleSprite == null)
            cir.setReturnValue(this.original.getParticleIcon());
    }

    /**
     * Fusion's Sodium integration passes {@code null} model data, so the wrapped model never receives its
     * block entity's model data (MI machines then always face north). Fetch it from the block entity instead.
     */
    @Inject(
        method = "collectByOffset",
        at = @At("HEAD"),
        cancellable = true
    )
    private void roll_mod$supplyModelData(ModelsByRandomOffset output, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable ModelData modelData, CallbackInfo ci){
        if(modelData != null || level == null || pos == null || state == null)
            return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        ModelData blockEntityData = blockEntity == null ? ModelData.EMPTY : blockEntity.getModelData();
        BlockModelModifierBakedModel self = (BlockModelModifierBakedModel)(Object)this;
        self.collectByOffset(output, level, pos, state, self.getModelData(level, pos, state, blockEntityData));
        ci.cancel();
    }
}
