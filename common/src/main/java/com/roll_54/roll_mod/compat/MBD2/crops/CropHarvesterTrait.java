package com.roll_54.roll_mod.compat.MBD2.crops;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.plant.AgriWeed;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.lowdragmc.lowdraglib2.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.Trait;
import com.lowdragmc.lowdraglib2.misc.FluidStorage;
import com.lowdragmc.mbd2.common.trait.fluid.FluidTankCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.roll_54.roll_mod.compat.MBD2.energy.MIEnergyStore;
import com.roll_54.roll_mod.compat.MBD2.energy.MIEnergyTrait;
import com.roll_54.roll_mod.compat.argicraft.AgriCropNutrition;
import com.roll_54.roll_mod.compat.argicraft.AgriHerbicide;
import com.roll_54.roll_mod.compat.argicraft.CropBreedingSettings;
import com.roll_54.roll_mod.compat.argicraft.HerbicideHelper;
import com.roll_54.roll_mod.data.datamap.ModDataMaps;
import com.roll_54.roll_mod.data.datamap.NutrientValue;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The live half of {@link CropHarvesterTraitDefinition}: scans the configured box behind the machine
 * once every {@code tickInterval} ticks, de-weeds crops with Herbicide and harvests the fully grown
 * ones, paying the configured energy cost per operation.
 *
 * <p>Ported from the deprecated {@code CropManagerBlockEntity.processCropsInRange}, with three
 * deliberate changes: the area follows the machine's facing instead of being a fixed box centred on
 * the block, items move through MBD2 item-slot traits instead of a hand-rolled {@code NonNullList},
 * and energy is reserved before the work happens rather than deducted after it.
 *
 * <p>Herbicide also comes as a fluid (1000 mB = one item of its tier). Where the definition enables it,
 * the machine feeds crops too (crops wiki §11.7): MV and HV turn {@code roll_mod:npk} items and fluids
 * into their own N/P/K buffer and top each crop's pools up from it; HV also tops crops up with mutagen.
 */
public class CropHarvesterTrait extends Trait {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(CropHarvesterTrait.class);

    /** How long the machine keeps its "working" look after the last operation, so the 8-frame front
     *  animation is actually visible instead of flickering on and off. */
    private static final int ACTIVE_HOLD_TICKS = 20;
    /** Liquid herbicide per application: a bucket, the worth of one item of its tier. */
    private static final int FLUID_PER_HERBICIDE_DOSE = 1000;
    /** Caps how many N/P/K items one scan may eat, so a full buffer is reached gradually. */
    private static final int MAX_NUTRIENT_ITEMS_PER_SCAN = 16;

    // Per-machine settings, flipped from the GUI's settings panel. @Persisted puts them in the
    // machine's NBT (namespaced by trait name, handled by MBDMachine.loadAdditionalTraits) and
    // @DescSynced pushes them to clients -- which is what lets the box renderer read them.
    @Persisted
    @DescSynced
    private boolean collectCrops = true;
    @Persisted
    @DescSynced
    private boolean applyHerbicide = true;
    @Persisted
    @DescSynced
    private boolean voidBiomass = false;
    @Persisted
    @DescSynced
    private boolean renderBoundingBox = false;
    @Persisted
    @DescSynced
    private boolean feedNutrients = true;
    @Persisted
    @DescSynced
    private boolean applyMutagen = true;

    /** N/P/K already taken from the inputs, waiting to be handed out to crops. */
    @Persisted
    private int nitrogenBuffer;
    @Persisted
    private int phosphorusBuffer;
    @Persisted
    private int potassiumBuffer;

    /** Transient on purpose: a freshly loaded machine should look idle until it does something. */
    private int activeHoldTicks;

    public CropHarvesterTrait(MBDMachine machine, CropHarvesterTraitDefinition definition) {
        super(machine, definition);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public CropHarvesterTraitDefinition getDefinition() {
        return (CropHarvesterTraitDefinition) super.getDefinition();
    }

    @Override
    public void serverTick() {
        CropHarvesterTraitDefinition definition = getDefinition();
        MBDMachine machine = getMachine();

        if (!(machine.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (activeHoldTicks > 0) {
            activeHoldTicks--;
        }
        machine.setMachineState(activeHoldTicks > 0 ? "working" : "base");

        if (machine.getOffsetTimer() % Math.max(1, definition.getTickInterval()) != 0) {
            return;
        }
        boolean feeding = definition.isNutrientFeeding() && feedNutrients;
        boolean mutating = definition.isMutagenFeeding() && applyMutagen;
        // Nothing enabled: skip the area scan entirely rather than walking 25 positions to do nothing.
        if (!collectCrops && !applyHerbicide && !feeding && !mutating) {
            return;
        }

        Direction front = machine.getFrontFacing().orElse(null);
        if (front == null || front.getAxis().isVertical()) {
            return;
        }

        MIEnergyStore energy = energyStore();
        ItemSlotCapabilityTrait output = slotTrait(definition.getOutputSlotName());
        if (energy == null || output == null) {
            return;
        }
        ItemSlotCapabilityTrait herbicide = slotTrait(definition.getHerbicideSlotName());
        FluidStorage herbicideTank = tank(definition.getHerbicideTankName());
        FluidStorage mutagenTank = mutating ? tank(definition.getMutagenTankName()) : null;
        if (feeding) {
            refillNutrientBuffer(slotTrait(definition.getNutrientSlotName()), tank(definition.getNutrientTankName()));
        }

        Direction back = front.getOpposite();
        Direction right = front.getClockWise();
        // One block behind the machine: the scanned box never includes the machine's own column.
        BlockPos origin = machine.getPos().relative(back);
        int halfWidth = definition.getAreaWidth() / 2;

        for (int depth = 0; depth < definition.getAreaDepth(); depth++) {
            for (int offset = -halfWidth; offset <= halfWidth; offset++) {
                for (int up = 0; up < definition.getAreaHeight(); up++) {
                    BlockPos target = origin.relative(back, depth).relative(right, offset).above(up);
                    // Never pull an unloaded chunk in just to look for a crop stick.
                    if (!level.isLoaded(target)) {
                        continue;
                    }
                    AgriApi.getCrop(level, target).ifPresent(crop -> {
                        if (crop.getLevel() == null) {
                            return;
                        }
                        if (applyHerbicide) {
                            tryRemoveWeeds(crop, level, target, energy, herbicide, herbicideTank, output);
                        }
                        // Fed before harvesting, so the harvest's N/P/K upkeep finds full pools.
                        if (feeding) {
                            tryFeedNutrients(crop, energy);
                        }
                        if (mutagenTank != null) {
                            tryApplyMutagen(crop, energy, mutagenTank);
                        }
                        if (collectCrops) {
                            tryHarvest(crop, level, target, energy, output);
                        }
                    });
                }
            }
        }
    }

    // ── Operations ───────────────────────────────────────────────────────

    /**
     * Applies one Herbicide item — or its worth of liquid herbicide, 1000 mB, when the slot is empty —
     * to a weeded crop and banks the rake drops plus a unit of Biomass. Mirrors
     * {@code CropManagerBlockEntity:88-122}, including the "already saturated" early-out that stops the
     * machine feeding herbicide into a crop that cannot absorb it yet.
     */
    private void tryRemoveWeeds(AgriCrop crop, ServerLevel level, BlockPos pos, MIEnergyStore energy,
                                @Nullable ItemSlotCapabilityTrait herbicide, @Nullable FluidStorage herbicideTank,
                                ItemSlotCapabilityTrait output) {
        if (!crop.hasWeeds() || HerbicideHelper.isWeedResistant(crop)) {
            return;
        }
        if (!(crop instanceof CropBlockEntity cropEntity) || !(crop instanceof AgriHerbicide agriHerbicide)) {
            return;
        }
        ItemStackTransfer herbicideStorage = herbicide == null ? null : herbicide.storage;
        ItemStack herbicideStack = herbicideStorage == null || herbicideStorage.getSlots() == 0
                ? ItemStack.EMPTY : herbicideStorage.getStackInSlot(0);
        int perItem = herbicideStack.isEmpty() ? 0 : herbicideStack.getOrDefault(ComponentsRegistry.HERBICIDE.get(), 0);
        boolean fromItem = perItem > 0;

        FluidStack fluid = herbicideTank == null ? FluidStack.EMPTY : herbicideTank.getFluid();
        double perMb = fluid.isEmpty() ? 0 : HerbicideHelper.herbicidePerMb(fluid.getFluid());
        if (!fromItem && perMb <= 0) {
            return;
        }
        long available = fromItem ? (long) perItem * herbicideStack.getCount() : (long) (perMb * fluid.getAmount());
        if (agriHerbicide.roll_mod$getHerbicideAmount() > available * 2) {
            return;
        }
        // Rake drops still need somewhere to go. With biomass voided the grid may legitimately stay
        // full for a while, so only the drops -- not the discarded biomass -- justify stalling here.
        if (!hasFreeSlot(output.storage)) {
            return;
        }
        int cost = getDefinition().getEnergyPerWeedRemoval();
        if (!canAfford(energy, cost)) {
            return;
        }
        AgriWeed weed = crop.getWeed();
        AgriGrowthStage stage = crop.getWeedGrowthStage();
        if (fromItem) {
            if (!HerbicideHelper.applyHerbicide(herbicideStack, cropEntity)) {
                return;
            }
            herbicideStorage.extractItem(0, 1, false);
        } else {
            FluidStack drained = herbicideTank.drain(FLUID_PER_HERBICIDE_DOSE, IFluidHandler.FluidAction.EXECUTE);
            int amount = (int) (perMb * drained.getAmount());
            if (amount <= 0) {
                return;
            }
            agriHerbicide.roll_mod$addHerbicide(amount);
        }

        crop.removeWeeds();
        energy.extractEnergy(cost, false);
        markActive();

        List<ItemStack> drops = new ArrayList<>();
        weed.onRake(stage, drops::add, crop.getLevel().getRandom(), null);
        for (ItemStack drop : drops) {
            bank(drop, output.storage, level, pos);
        }
        if (!voidBiomass) {
            // Flat 1, exactly as the old machine did. Raking by hand still scales with the weed's
            // growth stage via HerbicideHelper.biomassForStage; the machine deliberately does not.
            bank(new ItemStack(ItemRegistry.BIOMASS.get(), 1), output.storage, level, pos);
        }
    }

    /**
     * Turns {@code roll_mod:npk} inputs into the machine's N/P/K buffer: whole items while all their
     * nutrients fit, and up to a bucket of fluid per scan, as much of it as fits.
     */
    private void refillNutrientBuffer(@Nullable ItemSlotCapabilityTrait slot, @Nullable FluidStorage tank) {
        int cap = CropBreedingSettings.NUTRIENT_MANAGER_BUFFER;
        if (slot != null && slot.storage.getSlots() > 0) {
            for (int taken = 0; taken < MAX_NUTRIENT_ITEMS_PER_SCAN; taken++) {
                ItemStack stack = slot.storage.getStackInSlot(0);
                NutrientValue value = ModDataMaps.nutrients(stack);
                if (value == null || value.isEmpty()
                        || nitrogenBuffer + value.nitrogen() > cap
                        || phosphorusBuffer + value.phosphorus() > cap
                        || potassiumBuffer + value.potassium() > cap) {
                    break;
                }
                slot.storage.extractItem(0, 1, false);
                addToBuffer(value.nitrogen(), value.phosphorus(), value.potassium());
            }
        }
        if (tank != null && !tank.getFluid().isEmpty()) {
            NutrientValue value = ModDataMaps.nutrients(tank.getFluid().getFluid());
            if (value == null || value.isEmpty()) {
                return;
            }
            // Values are per bucket: drain only as many mB as the fullest nutrient still has room for.
            int mb = Math.min(tank.getFluidAmount(), 1000);
            mb = Math.min(mb, roomInMb(cap - nitrogenBuffer, value.nitrogen()));
            mb = Math.min(mb, roomInMb(cap - phosphorusBuffer, value.phosphorus()));
            mb = Math.min(mb, roomInMb(cap - potassiumBuffer, value.potassium()));
            if (mb <= 0) {
                return;
            }
            int drained = tank.drain(mb, IFluidHandler.FluidAction.EXECUTE).getAmount();
            addToBuffer(value.nitrogen() * drained / 1000, value.phosphorus() * drained / 1000,
                    value.potassium() * drained / 1000);
        }
    }

    private static int roomInMb(int room, int perBucket) {
        return perBucket <= 0 ? Integer.MAX_VALUE : (int) Math.min(Integer.MAX_VALUE, (long) room * 1000 / perBucket);
    }

    private void addToBuffer(int nitrogen, int phosphorus, int potassium) {
        nitrogenBuffer += nitrogen;
        phosphorusBuffer += phosphorus;
        potassiumBuffer += potassium;
    }

    /** Tops a planted crop's N, P and K pools up to {@link CropBreedingSettings#NUTRIENT_CROP_CAP} from the buffer. */
    private void tryFeedNutrients(AgriCrop crop, MIEnergyStore energy) {
        if (!crop.hasPlant() || !(crop instanceof AgriCropNutrition nutrition)) {
            return;
        }
        int cap = CropBreedingSettings.NUTRIENT_CROP_CAP;
        int nitrogen = Math.min(nitrogenBuffer, Math.max(0, cap - nutrition.roll_mod$getNitrogen()));
        int phosphorus = Math.min(phosphorusBuffer, Math.max(0, cap - nutrition.roll_mod$getPhosphorus()));
        int potassium = Math.min(potassiumBuffer, Math.max(0, cap - nutrition.roll_mod$getPotassium()));
        if (nitrogen + phosphorus + potassium <= 0) {
            return;
        }
        int cost = getDefinition().getEnergyPerFeed();
        if (!canAfford(energy, cost)) {
            return;
        }
        nutrition.roll_mod$addNutrients(nitrogen, phosphorus, potassium);
        addToBuffer(-nitrogen, -phosphorus, -potassium);
        energy.extractEnergy(cost, false);
        markActive();
    }

    /** Tops a planted crop up to {@link CropBreedingSettings#MUTAGEN_MANAGER_CAP} mutagen, 1 mB per unit. */
    private void tryApplyMutagen(AgriCrop crop, MIEnergyStore energy, FluidStorage tank) {
        if (!crop.hasPlant() || !(crop instanceof AgriCropNutrition nutrition) || tank.getFluidAmount() <= 0) {
            return;
        }
        int missing = CropBreedingSettings.MUTAGEN_MANAGER_CAP - nutrition.roll_mod$getMutagen();
        if (missing <= 0) {
            return;
        }
        int cost = getDefinition().getEnergyPerFeed();
        if (!canAfford(energy, cost)) {
            return;
        }
        int drained = tank.drain(missing, IFluidHandler.FluidAction.EXECUTE).getAmount();
        if (drained <= 0) {
            return;
        }
        nutrition.roll_mod$addMutagen(drained);
        energy.extractEnergy(cost, false);
        markActive();
    }

    private void tryHarvest(AgriCrop crop, ServerLevel level, BlockPos pos, MIEnergyStore energy, ItemSlotCapabilityTrait output) {
        if (!crop.hasPlant() || !crop.isFullyGrown()) {
            return;
        }
        if (!hasFreeSlot(output.storage)) {
            return;
        }
        int cost = getDefinition().getEnergyPerHarvest();
        if (!canAfford(energy, cost)) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>();
        if (!crop.harvest(drops::add, null)) {
            return;
        }
        energy.extractEnergy(cost, false);
        markActive();
        for (ItemStack drop : drops) {
            bank(drop, output.storage, level, pos);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /** Reserves nothing; just asks whether the whole cost is available, so work is never done free. */
    private static boolean canAfford(MIEnergyStore energy, int cost) {
        return cost <= 0 || energy.extractEnergy(cost, true) >= cost;
    }

    /**
     * An output stack is only ever taken on if a slot is completely free, which is what makes "grid
     * full" mean "idle" instead of "void the harvest".
     */
    private static boolean hasFreeSlot(ItemStackTransfer storage) {
        for (int slot = 0; slot < storage.getSlots(); slot++) {
            if (storage.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Puts {@code stack} in the grid. A remainder can only appear when a single operation yields more
     * than the free slots can hold; it is popped into the world rather than voided.
     */
    private static void bank(ItemStack stack, ItemStackTransfer storage, ServerLevel level, BlockPos pos) {
        ItemStack remainder = stack.copy();
        for (int slot = 0; slot < storage.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = storage.insertItem(slot, remainder, false);
        }
        if (!remainder.isEmpty()) {
            Block.popResource(level, pos, remainder);
        }
    }

    /** Keeps the machine in its "working" visual state for a moment after each operation. */
    private void markActive() {
        activeHoldTicks = ACTIVE_HOLD_TICKS;
    }

    // ── Settings (bound to the GUI's settings panel) ──────────────────────

    public boolean isCollectCrops() {
        return collectCrops;
    }

    public void setCollectCrops(boolean collectCrops) {
        this.collectCrops = collectCrops;
    }

    public boolean isApplyHerbicide() {
        return applyHerbicide;
    }

    public void setApplyHerbicide(boolean applyHerbicide) {
        this.applyHerbicide = applyHerbicide;
    }

    public boolean isVoidBiomass() {
        return voidBiomass;
    }

    public void setVoidBiomass(boolean voidBiomass) {
        this.voidBiomass = voidBiomass;
    }

    public boolean isRenderBoundingBox() {
        return renderBoundingBox;
    }

    public void setRenderBoundingBox(boolean renderBoundingBox) {
        if (this.renderBoundingBox == renderBoundingBox) {
            return;
        }
        this.renderBoundingBox = renderBoundingBox;
        // Nudge the chunk so the change shows immediately. The renderer no longer gates
        // hasBlockEntityRenderer on this flag, so a rebuild is not strictly required any more -- but
        // sections compiled before that fix, or before the machine was first toggled on, still need
        // one to pick the block entity up.
        getMachine().scheduleRenderUpdate();
    }

    public boolean isFeedNutrients() {
        return feedNutrients;
    }

    public void setFeedNutrients(boolean feedNutrients) {
        this.feedNutrients = feedNutrients;
    }

    public boolean isApplyMutagen() {
        return applyMutagen;
    }

    public void setApplyMutagen(boolean applyMutagen) {
        this.applyMutagen = applyMutagen;
    }

    @Nullable
    private MIEnergyStore energyStore() {
        MIEnergyTrait trait = getMachine().getTraitByName(MIEnergyTrait.class, getDefinition().getEnergyTraitName());
        return trait == null ? null : trait.getStorage();
    }

    @Nullable
    private ItemSlotCapabilityTrait slotTrait(String name) {
        return getMachine().getTraitByName(ItemSlotCapabilityTrait.class, name);
    }

    /** The first tank of the named fluid trait, or null when this tier has none. */
    @Nullable
    private FluidStorage tank(String name) {
        FluidTankCapabilityTrait trait = getMachine().getTraitByName(FluidTankCapabilityTrait.class, name);
        return trait == null || trait.storages.length == 0 ? null : trait.storages[0];
    }
}
