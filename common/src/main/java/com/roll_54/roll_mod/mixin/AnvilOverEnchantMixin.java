package com.roll_54.roll_mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.roll_54.roll_mod.registry.ModConfigs;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets the anvil combine enchantments past their natural maximum level ("over-enchanting").
 *
 * <p>Vanilla {@link AnvilMenu#createResult()} clamps the combined level of every enchantment down to
 * {@link Enchantment#getMaxLevel()}:
 * <pre>
 *     if (j2 &gt; enchantment.getMaxLevel()) {
 *         j2 = enchantment.getMaxLevel();
 *     }
 * </pre>
 * Both reads of {@code getMaxLevel()} in that clamp are the only ones in the method, so lifting the
 * returned value to {@link Integer#MAX_VALUE} removes the clamp entirely: two Sharpness V books yield
 * Sharpness VI, that book plus another Sharpness V yields VII, and so on. The natural combined level
 * (computed as {@code i2 == j2 ? j2 + 1 : Math.max(j2, i2)}) is untouched — we only stop it being
 * pulled back down. The anvil level cost still scales with the level via {@code i += anvilCost * j2},
 * which pairs with {@link AnvilLevelCapMixin}'s exponential cost scaling.
 *
 * @author roll_54
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilOverEnchantMixin {

    @ModifyExpressionValue(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"
            )
    )
    private int roll_mod$liftEnchantmentMaxLevel(int originalMaxLevel) {
        if (!ModConfigs.MAIN.anvil.overEnchant.get()) {
            return originalMaxLevel;
        }
        // Disable the "clamp combined level to the enchantment max" step without changing the
        // naturally computed combined level.
        return Integer.MAX_VALUE;
    }
}
