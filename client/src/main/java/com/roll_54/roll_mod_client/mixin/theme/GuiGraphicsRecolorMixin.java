package com.roll_54.roll_mod_client.mixin.theme;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.minestar.theme.GuiRecolor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

/**
 * Every GUI texture blit — container backgrounds, slots, buttons, nine-sliced sprites — ends in one
 * of {@code GuiGraphics.innerBlit}'s two overloads, which pick vanilla's {@code position_tex} or
 * {@code position_tex_color} shader. While "Use the hub theme everywhere" is on, {@link GuiRecolor}
 * hands back its recolouring copy instead; otherwise the vanilla shader goes through unchanged.
 *
 * <p>{@code @WrapOperation} rather than a redirect, so another mod wrapping the same call still runs.
 * {@code require = 0}: the feature is experimental, and a target that ever stops matching should
 * only switch the recolour off, never stop the game from starting.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsRecolorMixin {

    @WrapOperation(
            method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"),
            require = 0)
    private void roll_mod$recolor(Supplier<ShaderInstance> shader, Operation<Void> original) {
        original.call(GuiRecolor.pick(shader, false));
    }

    @WrapOperation(
            method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"),
            require = 0)
    private void roll_mod$recolorTinted(Supplier<ShaderInstance> shader, Operation<Void> original) {
        original.call(GuiRecolor.pick(shader, true));
    }
}
