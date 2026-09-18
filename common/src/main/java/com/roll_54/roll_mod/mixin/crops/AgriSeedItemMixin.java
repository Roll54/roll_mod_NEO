package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.common.block.entity.SeedAnalyzerBlockEntity;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops genome-less AgriCraft seeds from being eaten by an accidental use-on.
 *
 * <p>Upstream bug: when {@link AgriSeedItem#useOn} falls through to the "plant on soil" branch it
 * places crop sticks at the position above and then calls {@code plantSeed}, which runs
 * {@code stack.shrink(1)} <em>outside</em> the {@code has(DataComponents.CUSTOM_DATA)} guard that
 * actually assigns the genome. A seed without CUSTOM_DATA therefore gets consumed while planting
 * nothing, leaving a plantless ("empty") crop stick block behind.
 *
 * <p>The interaction normally arrives via the off-hand: the player tills dirt with a hoe, the hoe
 * returns PASS on the freshly created farmland (which <em>is</em> a registered AgriSoil, unlike the
 * dirt it came from), and vanilla's {@code Minecraft.startUseItem} then offers the same click to the
 * off-hand seed. So a routine tilling pass silently burns off-hand seeds and litters the field.
 *
 * <p>Only intercepts the case that can never do anything useful — a seed with no genome clicked on
 * something that is neither existing crop sticks nor a seed analyzer. Planting into crop sticks and
 * inserting into an analyzer are left completely untouched, as is every seed that <em>has</em> a
 * genome. AgriCraft's own {@code useOn} bails out with PASS on the client before doing anything, so
 * this returns PASS there too simply by matching the same conditions — no side-specific handling.
 */
@Mixin(AgriSeedItem.class)
public abstract class AgriSeedItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void roll_mod$dontConsumeGenomelessSeeds(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = context.getItemInHand();
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            return; // has a genome — a legitimate seed, let AgriCraft plant it as usual
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (AgriApi.getCrop(level, pos).isPresent()) {
            return; // clicking existing crop sticks — AgriCraft's own PASS/plant handling still applies
        }
        if (level.getBlockEntity(pos) instanceof SeedAnalyzerBlockEntity) {
            return; // inserting into a seed analyzer is a valid use for a genome-less seed
        }

        // Soil (or anything else): AgriCraft would place crop sticks above and shrink the stack for
        // no plant. Refuse the interaction outright so the seed survives and nothing is placed.
        cir.setReturnValue(InteractionResult.PASS);
    }
}
