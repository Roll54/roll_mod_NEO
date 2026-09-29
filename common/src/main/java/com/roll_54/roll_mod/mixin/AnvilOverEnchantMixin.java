package com.roll_54.roll_mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.roll_54.roll_mod.registry.ModConfigs;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets an over-max enchantment on an anvil input carry into the result ("overchanting" an item with
 * an overchanted book), without letting the anvil create new over-max levels.
 *
 * <p>Vanilla {@link AnvilMenu#createResult()} merges each enchantment of the right input into the
 * left one and clamps the combined level down to {@link Enchantment#getMaxLevel()}:
 * <pre>
 *     int i2 = itemenchantments$mutable.getLevel(holder);   // left level
 *     int j2 = entry.getIntValue();                          // right level
 *     j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2);
 *     ...
 *     if (j2 &gt; enchantment.getMaxLevel()) {
 *         j2 = enchantment.getMaxLevel();
 *     }
 * </pre>
 * That clamp even pulls an already over-max level back down. Here both {@code getMaxLevel()} reads
 * of the clamp — the only ones in the method — are raised to the highest level on either input, so:
 * <ul>
 *   <li>Sharpness V sword + Sharpness VII book → Sharpness VII sword;</li>
 *   <li>Sharpness VII sword + Sharpness I book → stays Sharpness VII;</li>
 *   <li>Sharpness V book + Sharpness V book → Sharpness V book, not VI — the {@code +1} of two equal
 *       levels still stops at the enchantment max, so books can't be overchanted by combining.</li>
 * </ul>
 * The anvil level cost still scales with the level via {@code i += anvilCost * j2}, which pairs with
 * {@link AnvilLevelCapMixin}'s exponential cost scaling.
 *
 * @author roll_54
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilOverEnchantMixin {

    /** Level of the enchantment being merged on the left input; set just before the clamp reads it. */
    @Unique
    private int roll_mod$leftLevel;

    /** Level of the enchantment being merged on the right input; set just before the clamp reads it. */
    @Unique
    private int roll_mod$rightLevel;

    @ModifyExpressionValue(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/ItemEnchantments$Mutable;getLevel(Lnet/minecraft/core/Holder;)I"
            )
    )
    private int roll_mod$captureLeftLevel(int level) {
        roll_mod$leftLevel = level;
        return level;
    }

    @ModifyExpressionValue(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/Object2IntMap$Entry;getIntValue()I"
            )
    )
    private int roll_mod$captureRightLevel(int level) {
        roll_mod$rightLevel = level;
        return level;
    }

    @ModifyExpressionValue(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"
            )
    )
    private int roll_mod$liftMaxLevelToInputs(int originalMaxLevel) {
        if (!ModConfigs.MAIN.anvil.overEnchant.get()) {
            return originalMaxLevel;
        }
        return Math.max(originalMaxLevel, Math.max(roll_mod$leftLevel, roll_mod$rightLevel));
    }
}
