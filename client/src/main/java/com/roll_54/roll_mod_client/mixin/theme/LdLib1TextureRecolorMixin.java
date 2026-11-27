package com.roll_54.roll_mod_client.mixin.theme;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.minestar.theme.GuiRecolor;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

/**
 * LDLib 1's textures (the ore vein pages' slots in the recipe viewer, other LDLib 1 widgets) are drawn with their own
 * vertex buffer, not through {@code GuiGraphics.innerBlit}, so the theme recolour ({@code GuiGraphicsRecolorMixin})
 * never reached them: their slots stayed vanilla grey with a white bevel beside the themed ones. Their shader is picked
 * the same way here.
 */
@Mixin(targets = "com.lowdragmc.lowdraglib.gui.texture.ResourceTexture", remap = false)
public abstract class LdLib1TextureRecolorMixin {

    @Shadow
    public ResourceLocation imageLocation;

    @WrapOperation(method = "drawSubArea",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V"),
            require = 0)
    private void roll_mod$recolor(Supplier<ShaderInstance> shader, Operation<Void> original) {
        original.call(GuiRecolor.keepsOwnColours(imageLocation) ? shader : GuiRecolor.pick(shader, true));
    }
}
