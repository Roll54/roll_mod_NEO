package com.roll_54.roll_mod_client.mixin.theme;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.minestar.theme.GuiRecolor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.sugar.Local;

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
    private void roll_mod$recolor(Supplier<ShaderInstance> shader, Operation<Void> original,
                                  @Local(argsOnly = true) ResourceLocation texture) {
        original.call(GuiRecolor.keepsOwnColours(texture) ? shader : GuiRecolor.pick(shader, false));
    }

    @WrapOperation(
            method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"),
            require = 0)
    private void roll_mod$recolorTinted(Supplier<ShaderInstance> shader, Operation<Void> original,
                                        @Local(argsOnly = true) ResourceLocation texture) {
        original.call(GuiRecolor.keepsOwnColours(texture) ? shader : GuiRecolor.pick(shader, true));
    }

    /**
     * The furnace's fire (furnace, blast furnace, smoker: {@code …/lit_progress}) keeps vanilla's colours: its greys
     * and whites are part of the flame, and recoloured they turned it into a smear of theme tones.
     */
    @WrapMethod(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIIIIII)V", require = 0)
    private void roll_mod$fireAsIs(ResourceLocation sprite, int texW, int texH, int u, int v, int x, int y, int w, int h,
                                   Operation<Void> original) {
        if (sprite.getPath().endsWith("lit_progress")) {
            GuiRecolor.withoutRecolor(() -> original.call(sprite, texW, texH, u, v, x, y, w, h));
        } else {
            original.call(sprite, texW, texH, u, v, x, y, w, h);
        }
    }

    @WrapMethod(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V", require = 0)
    private void roll_mod$fireAsIsWhole(ResourceLocation sprite, int x, int y, int w, int h, Operation<Void> original) {
        if (sprite.getPath().endsWith("lit_progress")) {
            GuiRecolor.withoutRecolor(() -> original.call(sprite, x, y, w, h));
        } else {
            original.call(sprite, x, y, w, h);
        }
    }

    /** The lists' blue hovered buttons in their grey form, so they take the theme's colours: {@link GuiRecolor#sprite}. */
    @ModifyVariable(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIIII)V",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private ResourceLocation roll_mod$plainSprite(ResourceLocation sprite) {
        return GuiRecolor.sprite(sprite);
    }

    /**
     * TEI's recipe buttons show their "on" states (bookmarked, default recipe, tree goal) in green, which the recolour (greys only) left as it was on every
     * theme. While the recolour is on, TEI's button sheet is drawn from roll_mod's copy, the same art with an amber
     * outline there instead ({@link GuiRecolor#swapTexture}).
     */
    @ModifyVariable(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFF)V",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private ResourceLocation roll_mod$themedSheet(ResourceLocation texture) {
        return GuiRecolor.swapTexture(texture);
    }

    @ModifyVariable(method = "innerBlit(Lnet/minecraft/resources/ResourceLocation;IIIIIFFFFFFFF)V",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private ResourceLocation roll_mod$themedSheetTinted(ResourceLocation texture) {
        return GuiRecolor.swapTexture(texture);
    }
}
