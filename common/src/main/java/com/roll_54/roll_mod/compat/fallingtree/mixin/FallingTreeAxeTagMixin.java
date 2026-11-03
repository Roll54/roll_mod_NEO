package com.roll_54.roll_mod.compat.fallingtree.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import fr.rakambda.fallingtree.common.FallingTreeCommon;
import fr.rakambda.fallingtree.common.wrapper.IItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets tools tagged {@code #minecraft:axes} fell whole trees with FallingTree.
 *
 * <p>FallingTree takes a tool as an axe only when it is an {@code AxeItem} or answers the
 * {@code axe_dig} ability. Modern Industrialization's and Extended Industrialization's chainsaws are
 * neither: they are plain items that MI's {@code DynamicToolItem} turns into axes by the
 * {@code #minecraft:axes} tag (EI's electric one answers {@code axe_dig} only in some modes), so they
 * chopped one log at a time. The tag is added to that last check, so FallingTree's own deny list
 * still wins.
 */
@Mixin(value = FallingTreeCommon.class, remap = false)
public abstract class FallingTreeAxeTagMixin {

    @ModifyExpressionValue(method = "isValidTool",
            at = @At(value = "INVOKE", target = "Lfr/rakambda/fallingtree/common/wrapper/IItemStack;canPerformAxeAction()Z"))
    private boolean roll_mod$axeTag(boolean canPerformAxeAction, @Local(argsOnly = true) IItemStack heldItemStack) {
        return canPerformAxeAction
                || heldItemStack.getRaw() instanceof ItemStack stack && stack.is(ItemTags.AXES);
    }
}
