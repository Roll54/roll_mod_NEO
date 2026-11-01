package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.fertilizer.AgriFertilizer;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.api.plant.AgriWeed;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.stat.AgriStatRegistry;
import com.agricraft.agricraft.api.config.CoreConfig;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.roll_54.roll_mod.blocks.HydroponicGardenBedBlock;
import com.roll_54.roll_mod.compat.argicraft.AgriCropNutrition;
import com.roll_54.roll_mod.compat.argicraft.AgriHerbicide;
import com.roll_54.roll_mod.compat.argicraft.CropBreeding;
import com.roll_54.roll_mod.compat.argicraft.CropBreedingSettings;
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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Unique;
import java.util.Comparator;
import java.util.List;

@Mixin(CropBlockEntity.class)
public abstract class CropBlockEntityMixin implements AgriHerbicide, AgriCropNutrition {

    @Shadow
    public abstract AgriGrowthStage getGrowthStage();

    @Shadow
    public abstract boolean isCrossCropSticks();

    @Unique
    private int roll_mod$herbicideAmount = 0;

    /** Mutagen as of {@link #roll_mod$mutagenTick}; it bleeds from there (see {@link #roll_mod$getMutagen}). */
    @Unique
    private int roll_mod$mutagenAmount = 0;
    @Unique
    private long roll_mod$mutagenTick = 0L;
    @Unique
    private int roll_mod$nitrogen = 0;
    @Unique
    private int roll_mod$phosphorus = 0;
    @Unique
    private int roll_mod$potassium = 0;

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
        this.roll_mod$mutagenAmount = Math.max(0, tag.getInt("mutagenAmount"));
        this.roll_mod$mutagenTick = tag.getLong("mutagenTick");
        this.roll_mod$nitrogen = Math.max(0, tag.getInt("npkN"));
        this.roll_mod$phosphorus = Math.max(0, tag.getInt("npkP"));
        this.roll_mod$potassium = Math.max(0, tag.getInt("npkK"));
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void roll_mod$saveHerbicideAmount(
            CompoundTag tag,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {
        tag.putInt("herbicideAmount", Math.max(0, this.roll_mod$herbicideAmount));
        tag.putInt("mutagenAmount", this.roll_mod$mutagenAmount);
        tag.putLong("mutagenTick", this.roll_mod$mutagenTick);
        tag.putInt("npkN", this.roll_mod$nitrogen);
        tag.putInt("npkP", this.roll_mod$phosphorus);
        tag.putInt("npkK", this.roll_mod$potassium);
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

    /**
     * Everything roll_mod adds to a crop, each line coloured by what it means — green when it is
     * fine, red when something is missing — and AgriCraft's own lines coloured too
     * ({@link #roll_mod$colourAgriCraftLines}). Since 4.0.24 the overlay re-wraps lines as plain text
     * and drops their colours; {@code MagnifyingGlassOverlayColourMixin} (client) keeps them. On cross
     * sticks it shows what the two neighbours that would be picked as parents would give a child.
     */
    @Inject(method = "addMagnifyingTooltip", at = @At("TAIL"))
    private void roll_mod$addCropTooltip(List<Component> tooltip, boolean isPlayerSneaking, CallbackInfo ci) {
        roll_mod$colourAgriCraftLines(tooltip);
        CropBlockEntity self = (CropBlockEntity) (Object) this;
        if (this.roll_mod$herbicideAmount > 0 || self.hasPlant() || this.isCrossCropSticks()) {
            roll_mod$line(tooltip, Component.translatable("message.roll_mod.magnifying.herbicide", this.roll_mod$herbicideAmount),
                    ChatFormatting.DARK_GREEN);
        }
        if (this.isCrossCropSticks()) {
            roll_mod$addBreedingPreview(self, tooltip);
            return;
        }
        if (!self.hasPlant()) {
            return;
        }
        int mutagen = this.roll_mod$getMutagen();
        roll_mod$line(tooltip, mutagen > 0
                ? Component.translatable("message.roll_mod.magnifying.mutagen.draining", mutagen,
                        CropBreedingSettings.MUTAGEN_BLEED_PER_SECOND)
                : Component.translatable("message.roll_mod.magnifying.mutagen", 0), ChatFormatting.LIGHT_PURPLE);

        boolean supplied = this.roll_mod$isNutrientSupplied();
        roll_mod$line(tooltip, Component.translatable("message.roll_mod.magnifying.nutrients",
                this.roll_mod$nitrogen, this.roll_mod$phosphorus, this.roll_mod$potassium), ChatFormatting.GOLD);
        AgriGenome genome = self.getGenome();
        boolean needsUpkeep = genome != null
                && CropBreeding.highestAllele(genome) > CropBreedingSettings.CEILING_UNSUPPLIED;
        roll_mod$line(tooltip, supplied
                ? Component.translatable("message.roll_mod.magnifying.nutrients.supplied",
                        CropBreedingSettings.NUTRIENT_COST_PER_HARVEST)
                : needsUpkeep
                        ? Component.translatable("message.roll_mod.magnifying.nutrients.regress",
                                CropBreedingSettings.CEILING_UNSUPPLIED)
                        : Component.translatable("message.roll_mod.magnifying.nutrients.unsupplied",
                                CropBreedingSettings.NUTRIENT_COST_PER_HARVEST),
                supplied ? ChatFormatting.GREEN : ChatFormatting.RED);

        roll_mod$line(tooltip, !supplied
                ? Component.translatable("message.roll_mod.magnifying.growth_boost.no_nutrients")
                : this.roll_mod$onHydroponicBed()
                        ? Component.translatable("message.roll_mod.magnifying.growth_boost.hydroponic")
                        : Component.translatable("message.roll_mod.magnifying.growth_boost.active",
                                roll_mod$multiplier()),
                supplied && !this.roll_mod$onHydroponicBed() ? ChatFormatting.GREEN : ChatFormatting.GRAY);

        roll_mod$line(tooltip, Component.translatable("message.roll_mod.magnifying.ceiling",
                CropBreeding.parentCeiling(self)), ChatFormatting.AQUA);
        Component next = CropBreeding.nextCeilingRequirement(self);
        if (next != null) {
            roll_mod$line(tooltip, Component.literal("  ").append(next), ChatFormatting.YELLOW);
        }
        roll_mod$line(tooltip, mutagen >= CropBreedingSettings.MUTAGEN_GATE
                ? Component.translatable("message.roll_mod.magnifying.mutation.ready")
                : Component.translatable("message.roll_mod.magnifying.mutation.blocked", CropBreedingSettings.MUTAGEN_GATE),
                mutagen >= CropBreedingSettings.MUTAGEN_GATE ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    /**
     * AgriCraft's own lines, which come with no colour: each gets one from its translation key —
     * the heading gold, the species green, the stats aqua, the growth stage yellow, weeds dark green,
     * "fertile" green, and anything that stops the plant growing red. A line that already has a
     * colour keeps it.
     */
    @Unique
    private static void roll_mod$colourAgriCraftLines(List<Component> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (roll_mod$hasColour(line)) continue;
            String key = roll_mod$firstKey(line);
            if (key == null || !key.startsWith("agricraft.")) continue;
            tooltip.set(i, line.copy().withStyle(roll_mod$colourFor(key)));
        }
    }

    @Unique
    private static ChatFormatting roll_mod$colourFor(String key) {
        if (key.equals("agricraft.tooltip.magnifying.crop") || key.equals("agricraft.tooltip.magnifying.soil")) {
            return ChatFormatting.GOLD;
        }
        if (key.equals("agricraft.tooltip.magnifying.species")) return ChatFormatting.GREEN;
        if (key.startsWith("agricraft.tooltip.magnifying.stat.")) return ChatFormatting.AQUA;
        if (key.equals("agricraft.tooltip.magnifying.growth")) return ChatFormatting.YELLOW;
        if (key.equals("agricraft.tooltip.magnifying.weeds")) return ChatFormatting.DARK_GREEN;
        if (key.equals("agricraft.tooltip.magnifying.requirement.fertile")) return ChatFormatting.GREEN;
        if (key.endsWith(".not_fertile") || key.endsWith(".lethal") || key.startsWith("agricraft.tooltip.condition.")) {
            return ChatFormatting.RED;
        }
        return ChatFormatting.GRAY;
    }

    @Unique
    private static boolean roll_mod$hasColour(Component component) {
        if (component.getStyle().getColor() != null) return true;
        for (Component sibling : component.getSiblings()) {
            if (roll_mod$hasColour(sibling)) return true;
        }
        return false;
    }

    /** The first translation key in the line (its own, then its children's), or null. */
    @Unique
    private static String roll_mod$firstKey(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable) return translatable.getKey();
        for (Component sibling : component.getSiblings()) {
            String key = roll_mod$firstKey(sibling);
            if (key != null) return key;
        }
        return null;
    }

    /**
     * Mirrors AgriCraft's parent pick without its fertility roll: mature planted neighbours, most
     * fertile first. One parent clones, so only a pair gets a ceiling and mutation line.
     */
    @Unique
    private static void roll_mod$addBreedingPreview(CropBlockEntity self, List<Component> tooltip) {
        List<AgriCrop> parents = self.streamNeighbours()
                .filter(AgriCrop::hasPlant)
                .filter(AgriCrop::canBeHarvested)
                .sorted(Comparator.comparingInt((AgriCrop crop) -> crop.getGenome().getFertility()).reversed())
                .limit(2)
                .toList();
        if (parents.size() < 2) {
            roll_mod$line(tooltip, Component.translatable("message.roll_mod.magnifying.breeding.no_pair"), ChatFormatting.GRAY);
            return;
        }
        AgriCrop a = parents.get(0);
        AgriCrop b = parents.get(1);
        roll_mod$line(tooltip, Component.translatable("message.roll_mod.magnifying.breeding.ceiling",
                CropBreeding.pairCeiling(a, b)), ChatFormatting.AQUA);
        boolean mutates = CropBreeding.canMutate(a, b);
        roll_mod$line(tooltip, mutates
                ? Component.translatable("message.roll_mod.magnifying.breeding.mutation_ready")
                : Component.translatable("message.roll_mod.magnifying.breeding.mutation_blocked",
                        CropBreedingSettings.MUTAGEN_GATE), mutates ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Unique
    private static void roll_mod$line(List<Component> tooltip, Component line, ChatFormatting colour) {
        tooltip.add(Component.literal("  ").append(line).withStyle(colour));
    }

    @Unique
    private static String roll_mod$multiplier() {
        double multiplier = CropBreedingSettings.NUTRIENT_GROWTH_MULTIPLIER;
        return multiplier == Math.rint(multiplier) ? String.valueOf((long) multiplier) : String.valueOf(multiplier);
    }

    /**
     * Resistance keeps its 1–10 meaning with stats stored up to 20 (crops wiki §1): AgriCraft's
     * default rolls {@code nextInt(max) >= (max + resistance) / 2}, so it is recomputed here with
     * {@value CropBreedingSettings#STAT_REFERENCE_MAX} as the max. The crop-stick material factor that
     * {@code CropBlockEntity} applies afterwards is untouched; empty sticks keep the original roll.
     */
    @ModifyExpressionValue(
            method = "shouldWeedsActivate",
            at = @At(value = "INVOKE", target = "Lcom/agricraft/agricraft/api/crop/AgriCrop;shouldWeedsActivate()Z")
    )
    private boolean roll_mod$resistanceReferenceMax(boolean original) {
        CropBlockEntity self = (CropBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (CoreConfig.disableWeeds || level == null) {
            return false;
        }
        if (!self.hasPlant() || self.getGenome() == null) {
            return original;
        }
        int max = CropBreedingSettings.STAT_REFERENCE_MAX;
        return level.getRandom().nextInt(max) >= (max + self.getGenome().getResistance()) / 2;
    }

    /** N/P/K-fed crops grow faster, except on a Hydroponic Garden Bed, whose own boost doesn't stack. */
    @ModifyReturnValue(method = "calculateGrowthRate", at = @At("RETURN"))
    private double roll_mod$nutrientGrowthBoost(double rate) {
        if (this.roll_mod$isNutrientSupplied() && !this.roll_mod$onHydroponicBed()) {
            return rate * CropBreedingSettings.NUTRIENT_GROWTH_MULTIPLIER;
        }
        return rate;
    }

    @Unique
    private boolean roll_mod$onHydroponicBed() {
        CropBlockEntity self = (CropBlockEntity) (Object) this;
        Level level = self.getLevel();
        return level != null
                && level.getBlockState(self.getBlockPos().below()).getBlock() instanceof HydroponicGardenBedBlock;
    }

    @Unique
    private void roll_mod$sync() {
        CropBlockEntity self = (CropBlockEntity) (Object) this;
        self.setChanged();
        Level level = self.getLevel();
        if (level != null && !level.isClientSide) {
            BlockPos pos = self.getBlockPos();
            level.sendBlockUpdated(pos, self.getBlockState(), self.getBlockState(), 3);
        }
    }

    // ── AgriCropNutrition ───────────────────────────────────────────────

    /**
     * Crops don't tick, so the bleed is worked out from game time:
     * {@code stored − bleed × elapsed seconds}, never below zero.
     */
    @Override
    public int roll_mod$getMutagen() {
        Level level = ((CropBlockEntity) (Object) this).getLevel();
        if (level == null || this.roll_mod$mutagenAmount <= 0) {
            return Math.max(0, this.roll_mod$mutagenAmount);
        }
        long elapsed = Math.max(0L, level.getGameTime() - this.roll_mod$mutagenTick);
        long bled = elapsed * CropBreedingSettings.MUTAGEN_BLEED_PER_SECOND / 20L;
        return (int) Math.max(0L, this.roll_mod$mutagenAmount - bled);
    }

    @Override
    public void roll_mod$addMutagen(int amount) {
        if (amount <= 0) {
            return;
        }
        Level level = ((CropBlockEntity) (Object) this).getLevel();
        int current = this.roll_mod$getMutagen();
        this.roll_mod$mutagenAmount = (int) Math.min(Integer.MAX_VALUE, (long) current + amount);
        this.roll_mod$mutagenTick = level != null ? level.getGameTime() : 0L;
        this.roll_mod$sync();
    }

    @Override
    public int roll_mod$getNitrogen() {
        return this.roll_mod$nitrogen;
    }

    @Override
    public int roll_mod$getPhosphorus() {
        return this.roll_mod$phosphorus;
    }

    @Override
    public int roll_mod$getPotassium() {
        return this.roll_mod$potassium;
    }

    @Override
    public void roll_mod$addNutrients(int nitrogen, int phosphorus, int potassium) {
        if (nitrogen <= 0 && phosphorus <= 0 && potassium <= 0) {
            return;
        }
        this.roll_mod$nitrogen += Math.max(0, nitrogen);
        this.roll_mod$phosphorus += Math.max(0, phosphorus);
        this.roll_mod$potassium += Math.max(0, potassium);
        this.roll_mod$sync();
    }

    @Override
    public boolean roll_mod$isNutrientSupplied() {
        int cost = CropBreedingSettings.NUTRIENT_COST_PER_HARVEST;
        return this.roll_mod$nitrogen >= cost && this.roll_mod$phosphorus >= cost && this.roll_mod$potassium >= cost;
    }

    @Override
    public boolean roll_mod$consumeNutrients() {
        if (!this.roll_mod$isNutrientSupplied()) {
            return false;
        }
        int cost = CropBreedingSettings.NUTRIENT_COST_PER_HARVEST;
        this.roll_mod$nitrogen -= cost;
        this.roll_mod$phosphorus -= cost;
        this.roll_mod$potassium -= cost;
        this.roll_mod$sync();
        return true;
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