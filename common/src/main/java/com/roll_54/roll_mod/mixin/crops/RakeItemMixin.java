package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.common.item.RakeItem;
import com.roll_54.roll_mod.compat.argicraft.HerbicideHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * roll_mod's rake rules on top of AgriCraft's (4.0.24+) {@link RakeItem#rakeCrop}, which does the raking itself:
 * iron rake clears the weed, wooden rake steps it down one stage, and the weed's rake drops spawn.
 *
 * <ul>
 *     <li>Fully removing a weed also drops {@code roll_mod:biomass} scaled by how grown it was
 *     ({@link HerbicideHelper#dropBiomass}), on the server only. Hooked on {@code rakeCrop}, the one method
 *     every rake path goes through: {@code CropBlock}'s own main-hand handling and {@link RakeItem#useOn}.</li>
 *     <li>{@code useOn} (sneaking, off hand, automation) still rakes on both sides upstream, so the client's
 *     predicted removal desyncs and the weed comes back. Here it rakes on the server only and lets the block
 *     sync carry the result, like {@code CropBlock} does.</li>
 * </ul>
 */
@Mixin(RakeItem.class)
public abstract class RakeItemMixin {

    /** The weed's stage before this {@code rakeCrop} call, for the biomass amount; null when nothing to drop. */
    @Unique
    private static final ThreadLocal<AgriGrowthStage> roll_mod$rakedStage = new ThreadLocal<>();

    @Shadow
    public abstract void rakeCrop(AgriCrop crop, ItemStack stack, @Nullable Player player);

    @Inject(method = "rakeCrop", at = @At("HEAD"))
    private void roll_mod$rememberWeedStage(AgriCrop crop, ItemStack stack, @Nullable Player player, CallbackInfo ci) {
        Level level = crop.getLevel();
        boolean server = level != null && !level.isClientSide;
        roll_mod$rakedStage.set(server && crop.hasWeeds() ? crop.getWeedGrowthStage() : null);
    }

    @Inject(method = "rakeCrop", at = @At("RETURN"))
    private void roll_mod$dropBiomassWhenRemoved(AgriCrop crop, ItemStack stack, @Nullable Player player, CallbackInfo ci) {
        AgriGrowthStage stage = roll_mod$rakedStage.get();
        roll_mod$rakedStage.remove();
        if (stage != null && !crop.hasWeeds()) {
            HerbicideHelper.dropBiomass(crop.getLevel(), crop.getBlockPos(), stage);
        }
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void roll_mod$rakeOnServerOnly(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        Optional<AgriCrop> crop = AgriApi.getCrop(level, context.getClickedPos());
        if (crop.isEmpty() || !crop.get().hasWeeds()) {
            return; // nothing to rake: AgriCraft's own result
        }
        if (!level.isClientSide) {
            this.rakeCrop(crop.get(), context.getItemInHand(), context.getPlayer());
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
    }
}
