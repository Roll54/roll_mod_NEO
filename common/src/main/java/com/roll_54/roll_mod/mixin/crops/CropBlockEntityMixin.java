package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.fertilizer.AgriFertilizer;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.api.plant.AgriWeed;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.roll_54.roll_mod.compat.argicraft.AgriHerbicide;
import com.roll_54.roll_mod.compat.argicraft.WeedResistant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Unique;
import java.util.List;

@Mixin(CropBlockEntity.class)
public abstract class CropBlockEntityMixin implements AgriHerbicide {

    @Shadow
    public abstract AgriGrowthStage getGrowthStage();

    @Shadow
    public abstract boolean isCrossCropSticks();

    @Unique
    private int roll_mod$herbicideAmount = 0;

    /**
     * Null-safe override of AgriCraft's {@code AgriCrop.isFullyGrown()} default
     * ({@code getGrowthStage().isFinal()}), which NPEs on crops with no growth stage (e.g. bare
     * sticks). FTB Ultimine's single-crop-harvesting calls this on every crop right-click, so the
     * unguarded default crashes the {@code ServerboundUseItemOnPacket} handler and aborts the
     * interaction (crops can't be collected, client gets kicked). An absent growth stage is, by
     * definition, not fully grown.
     */
    public boolean isFullyGrown() {
        AgriGrowthStage stage = this.getGrowthStage();
        return stage != null && stage.isFinal();
    }

    /**
     * Fertilizer (bone meal included) must not make cross crop sticks spread the neighbouring crops.
     * Upstream accepts fertilizer on cross sticks ({@code allow_fertilizer_mutation} + the fertilizer's
     * {@code trigger_mutation}) and runs a cross-breed tick per potency step, so a player could force
     * spreading on demand. Rejecting it here keeps the item unused on cross sticks.
     */
    @Inject(method = "acceptsFertilizer", at = @At("HEAD"), cancellable = true)
    private void roll_mod$rejectFertilizerOnCrossSticks(AgriFertilizer fertilizer, CallbackInfoReturnable<Boolean> cir) {
        if (this.isCrossCropSticks()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Same rule for callers that apply fertilizer without asking {@code acceptsFertilizer} first (other
     * mods' fertilizing machines): the fertilizer-only growth tick does nothing on cross sticks. Natural
     * cross-breeding runs through {@code applyGrowthTick} and is unaffected.
     */
    @Inject(method = "applyFertilizerGrowthTick", at = @At("HEAD"), cancellable = true)
    private void roll_mod$noFertilizerCrossBreeding(CallbackInfo ci) {
        if (this.isCrossCropSticks()) {
            ci.cancel();
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void roll_mod$loadHerbicideAmount(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {
        this.roll_mod$herbicideAmount = Math.max(0, tag.getInt("herbicideAmount"));
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void roll_mod$saveHerbicideAmount(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {
        tag.putInt("herbicideAmount", Math.max(0, this.roll_mod$herbicideAmount));
    }

    @Inject(method = "executeWeedsGrowthTick", at = @At("HEAD"), cancellable = true)
    private void roll_mod$consumeHerbicideBeforeWeedsGrow(CallbackInfo ci) {
        if (this.roll_mod$herbicideAmount <= 0) {
            return;
        }
        CropBlockEntity self = (CropBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }
        int resistance = 0;
        AgriGenome genome = self.getGenome();
        if (genome != null) {
            resistance = (Integer) genome.getStatGene(AgriStatRegistry.getInstance().resistanceStat()).getTrait();
        }
        int multiplier = Math.max(1, 11 - resistance);
        int removal = (level.getRandom().nextInt(5) + 1) * multiplier;
        this.roll_mod$herbicideAmount = Math.max(0, this.roll_mod$herbicideAmount - removal);
        self.setChanged();
        if (!level.isClientSide) {
            BlockPos pos = self.getBlockPos();
            level.sendBlockUpdated(pos, self.getBlockState(), self.getBlockState(), 3);
        }
        ci.cancel();
    }

    @Inject(method = "addMagnifyingTooltip", at = @At("TAIL"))
    private void roll_mod$addHerbicideTooltip(List<Component> tooltip, boolean isPlayerSneaking, CallbackInfo ci) {
        if (this.roll_mod$herbicideAmount > 0) {
            tooltip.add(Component.literal("  ").append(
                    Component.translatable("message.roll_mod.magnifying.herbicide", this.roll_mod$herbicideAmount)
            ));
        }
    }

    @Override
    public int roll_mod$getHerbicideAmount() {
        return this.roll_mod$herbicideAmount;
    }

    @Override
    public void roll_mod$setHerbicideAmount(int amount) {
        this.roll_mod$herbicideAmount = Math.max(0, amount);
    }

    @Override
    public void roll_mod$addHerbicide(int amount) {
        if (amount <= 0) {
            return;
        }
        CropBlockEntity self = (CropBlockEntity) (Object) this;
        this.roll_mod$herbicideAmount = Math.max(0, this.roll_mod$herbicideAmount + amount);
        self.setChanged();
        Level level = self.getLevel();
        if (level != null && !level.isClientSide) {
            BlockPos pos = self.getBlockPos();
            level.sendBlockUpdated(pos, self.getBlockState(), self.getBlockState(), 3);
        }
    }

    @Inject(method = "setWeed", at = @At("HEAD"), cancellable = true)
    private void roll_mod$blockWeedSetWhenHerbicidePresent(String weedId, AgriWeed weed, CallbackInfo ci) {
        if (this.roll_mod$herbicideAmount > 0 || this.roll_mod$isWeedsResistantPlant()) {
            ci.cancel();
        }
    }

    @Inject(method = "spreadWeeds", at = @At("HEAD"), cancellable = true)
    private void roll_mod$blockWeedSpreadWhenHerbicidePresent(CallbackInfo ci) {
        if (this.roll_mod$herbicideAmount > 0 || this.roll_mod$isWeedsResistantPlant()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean roll_mod$isWeedsResistantPlant() {
        AgriPlant plant = ((CropBlockEntity) (Object) this).getPlant();
        return plant instanceof WeedResistant resistant && resistant.roll_mod$isWeedsResistant();
    }
}