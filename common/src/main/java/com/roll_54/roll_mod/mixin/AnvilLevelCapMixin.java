package com.roll_54.roll_mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.roll_54.roll_mod.registry.ModConfigs;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Removes the vanilla anvil's 40-level "Too Expensive!" cap and replaces the hard block with an
 * exponential cost curve, so expensive work is possible but increasingly punishing.
 *
 * <p>Two precise edits inside {@link AnvilMenu#createResult()}:
 * <ol>
 *   <li><b>Exponential scaling.</b> The total cost is produced by
 *       {@code int k2 = (int) Mth.clamp(j + (long) i, 0L, 2147483647L)} — the only
 *       {@code Mth.clamp(long, long, long)} call in the method. Once that raw cost reaches the
 *       configured threshold (40 by default) we replace it with
 *       {@code threshold * expGrowth^(cost - threshold)}, clamped to {@link Integer#MAX_VALUE}.
 *       Because this feeds {@code this.cost}, it scales both the displayed level cost and the XP
 *       actually charged on take. Costs below the threshold, and pure renames (kept at 39 by
 *       vanilla), are unaffected.</li>
 *   <li><b>Lift the hard block.</b> Vanilla then does
 *       {@code if (this.cost.get() >= 40 && !instabuild) itemstack1 = ItemStack.EMPTY;}. That is the
 *       second (ordinal 1) {@code DataSlot.get()} read in the method — the first is the rename cap
 *       check just above it. Forcing this read to report {@code 0} makes the comparison fail, so the
 *       result item is kept no matter how high the (now exponential) cost climbs. The real cost value
 *       shown to the player is untouched, since this return only feeds that one comparison. The player
 *       still needs enough XP levels to take the result ({@code mayPickup}), so the exponential curve
 *       is a real cost, not a free bypass.</li>
 * </ol>
 *
 * @author roll_54
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilLevelCapMixin {

    @ModifyExpressionValue(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;clamp(JJJ)J"
            )
    )
    private long roll_mod$scaleCostExponentially(long rawCost) {
        if (!ModConfigs.MAIN.anvil.removeLevelCap.get()) {
            return rawCost;
        }

        int threshold = ModConfigs.MAIN.anvil.expThreshold.get();
        if (rawCost < threshold) {
            return rawCost;
        }

        double growth = ModConfigs.MAIN.anvil.expGrowth.get();
        double scaled =
                //threshold *
                        Math.pow(growth, rawCost - threshold);
        return (long) Math.min(scaled, Integer.MAX_VALUE);
    }

    @ModifyExpressionValue(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/DataSlot;get()I",
                    ordinal = 1
            )
    )
    private int roll_mod$dropTooExpensiveBlock(int cost) {
        if (!ModConfigs.MAIN.anvil.removeLevelCap.get()) {
            return cost;
        }
        // Report 0 to the "cost >= 40 -> empty the result" check only, so the result is never blocked
        // purely for being expensive. The displayed cost (this.cost) is unchanged.
        return 0;
    }
}
