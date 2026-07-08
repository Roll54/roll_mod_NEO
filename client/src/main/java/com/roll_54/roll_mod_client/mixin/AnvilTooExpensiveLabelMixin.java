package com.roll_54.roll_mod_client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.roll_54.roll_mod.registry.ModConfigs;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides the red "Too Expensive!" label past the vanilla 40-level cap, matching {@link
 * com.roll_54.roll_mod.mixin.AnvilLevelCapMixin}'s removal of the server-side hard block.
 *
 * <p>Client-only {@link AnvilScreen#renderLabels} decides the label with:
 * <pre>
 *     if (i &gt;= 40 &amp;&amp; !this.minecraft.player.getAbilities().instabuild) {
 *         component = TOO_EXPENSIVE_TEXT;   // red "Too Expensive!"
 *     } else if (!this.menu.getSlot(2).hasItem()) {
 *         component = null;
 *     } else {
 *         component = Component.translatable("container.repair.cost", i);  // real cost number
 *         if (!this.menu.getSlot(2).mayPickup(this.player)) j = red;
 *     }
 * </pre>
 * The {@code instabuild} field read is the only one in the method. When {@code removeLevelCap} is on
 * we report it as {@code true}, so {@code !instabuild} is false and the "Too Expensive!" branch is
 * skipped. The label then falls through to the normal cost display, which shows the actual level
 * cost — coloured red by vanilla's own {@code mayPickup} check when the player can't afford it. This
 * is purely cosmetic: it changes only how the cost is labelled, not whether the result can be taken.
 *
 * @author roll_54
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilTooExpensiveLabelMixin {

    @ModifyExpressionValue(
            method = "renderLabels",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Abilities;instabuild:Z",
                    opcode = org.objectweb.asm.Opcodes.GETFIELD
            )
    )
    private boolean roll_mod$hideTooExpensiveLabel(boolean instabuild) {
        if (ModConfigs.MAIN.anvil.removeLevelCap.get()) {
            // Skip the "Too Expensive!" branch so the real cost is shown instead. Vanilla still
            // colours it red via mayPickup when the exponential cost is unaffordable.
            return true;
        }
        return instabuild;
    }
}
