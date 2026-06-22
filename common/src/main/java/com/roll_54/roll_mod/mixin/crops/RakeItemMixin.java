package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.plant.AgriWeed;
import com.agricraft.agricraft.common.block.CropBlock;
import com.agricraft.agricraft.common.item.RakeItem;
import com.agricraft.agricraft.common.registry.ModItems;
import com.roll_54.roll_mod.compat.argicraft.HerbicideHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Makes the AgriCraft hand rake (iron/wooden) remove weeds authoritatively on the server.
 *
 * <p>By default AgriCraft's {@link RakeItem#useOn} removes the weed on the client (interaction
 * prediction) but the removal does not stick on the server, so the weed reappears on the next block
 * sync. This mixin takes full ownership of "rake a weedy crop" on both physical sides: it runs the
 * exact same rake logic AgriCraft would (iron rake clears the weed; wooden rake steps it down one
 * growth stage, clearing it once it bottoms out), spawns the rake drops server-side, and returns
 * early so AgriCraft's own (desyncing) path never runs — keeping client and server in lock-step.
 *
 * <p>Only intercepts when the held item is a rake <em>and</em> the targeted crop currently has weeds;
 * every other interaction (raking a clean crop, non-crop blocks) falls through to AgriCraft untouched.
 */
@Mixin(RakeItem.class)
public abstract class RakeItemMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void roll_mod$rakeAuthoritatively(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        Optional<AgriCrop> optional = AgriApi.getCrop(level, pos);
        if (optional.isEmpty()) {
            return; // not a crop — let AgriCraft's useOn run
        }

        boolean iron = (Object) this == ModItems.IRON_RAKE.get();
        boolean wooden = (Object) this == ModItems.WOODEN_RAKE.get();
        if (!iron && !wooden) {
            return; // unknown rake variant — don't take over
        }

        AgriCrop crop = optional.get();
        if (!crop.hasWeeds()) {
            return; // nothing to rake — let AgriCraft handle the interaction normally
        }
        AgriWeed weed = crop.getWeed();
        AgriGrowthStage stage = crop.getWeedGrowthStage();
        if (weed == null || stage == null) {
            return;
        }

        RandomSource random = level.getRandom();

        // Iron rake clears the weed outright; wooden rake knocks it down one stage at a time.
        boolean removed;
        if (iron) {
            crop.removeWeeds();
            removed = true;
        } else {
            AgriGrowthStage previous = stage.getPrevious(crop, random);
            if (previous == stage) { // already at the lowest stage (getPrevious returns the same instance)
                crop.removeWeeds();
                removed = true;
            } else {
                crop.setWeedGrowthStage(previous);
                removed = false;
            }
        }

        // Drops are server-authoritative; the client only mirrors the visual weed change above.
        if (!level.isClientSide) {
            Player player = context.getPlayer();
            ArrayList<ItemStack> drops = new ArrayList<>();
            weed.onRake(stage, drops::add, random, player);
            for (ItemStack drop : drops) {
                CropBlock.spawnItem(level, pos, drop);
            }
            // Removing the weed yields biomass scaled by how grown it was at removal time.
            if (removed) {
                HerbicideHelper.dropBiomass(level, pos, stage);
            }
        }

        // Take over this interaction entirely so AgriCraft's desyncing rake path doesn't also run.
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
