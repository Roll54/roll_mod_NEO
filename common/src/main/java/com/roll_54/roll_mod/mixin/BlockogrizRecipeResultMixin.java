package com.roll_54.roll_mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.roll_54.roll_mod.items.BlockogrizItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives a crafted blockogriz its tier's pre-enchant ({@link BlockogrizItem#withPreEnchant}).
 * Enchantments are a dynamic registry, so they cannot be baked into the item's default
 * components; the recipe is the first place that has registry access. Hooking {@code assemble}
 * covers the crafting table preview, crafters and autocrafting (AE2 assembles the recipe too);
 * {@code getResultItem} makes JEI/EMI show the enchanted output. The latter returns a copy, since
 * the recipe's own result stack is shared.
 */
@Mixin({ShapedRecipe.class, ShapelessRecipe.class, SmithingTransformRecipe.class})
public abstract class BlockogrizRecipeResultMixin {

    @ModifyReturnValue(method = "assemble", at = @At("RETURN"))
    private ItemStack roll_mod$preEnchantAssembled(ItemStack result,
                                                   @Local(argsOnly = true) HolderLookup.Provider registries) {
        return BlockogrizItem.withPreEnchant(result, registries);
    }

    @ModifyReturnValue(method = "getResultItem", at = @At("RETURN"))
    private ItemStack roll_mod$preEnchantResult(ItemStack result,
                                                @Local(argsOnly = true) HolderLookup.Provider registries) {
        return result.getItem() instanceof BlockogrizItem
                ? BlockogrizItem.withPreEnchant(result.copy(), registries)
                : result;
    }
}
