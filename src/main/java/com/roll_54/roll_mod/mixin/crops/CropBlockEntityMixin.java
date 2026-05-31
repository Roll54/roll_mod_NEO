package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.plant.AgriWeed;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.roll_54.roll_mod.compat.argicraft.AgriHerbicide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Unique;
import java.util.List;

@Mixin(CropBlockEntity.class)
public abstract class CropBlockEntityMixin implements AgriHerbicide {

    @Shadow
    private String weedId;

    @Shadow
    private AgriWeed weed;

    @Shadow
    private AgriGrowthStage weedGrowthStage;

    @Unique
    private int roll_mod$herbicideAmount = 0;

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void roll_mod$clearWeedsWhenTagSaysNo(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {
        if (!tag.getBoolean("hasWeeds")) {
            this.weedId = "";
            this.weed = null;
            this.weedGrowthStage = new AgriGrowthStage(0, 8);
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
        if (this.roll_mod$herbicideAmount > 0) {
            ci.cancel();
        }
    }

    @Inject(method = "spreadWeeds", at = @At("HEAD"), cancellable = true)
    private void roll_mod$blockWeedSpreadWhenHerbicidePresent(CallbackInfo ci) {
        if (this.roll_mod$herbicideAmount > 0) {
            ci.cancel();
        }
    }
}