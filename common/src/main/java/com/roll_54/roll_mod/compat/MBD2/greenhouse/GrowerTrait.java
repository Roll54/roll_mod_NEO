package com.roll_54.roll_mod.compat.MBD2.greenhouse;

import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.machines.components.UpgradeComponent;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.MBDMultiblockMachine;
import com.lowdragmc.mbd2.common.trait.Trait;
import com.lowdragmc.mbd2.common.trait.item.ItemSlotCapabilityTrait;
import com.roll_54.roll_mod.data.datamap.FertilizerValue;
import com.roll_54.roll_mod.data.datamap.ModDataMaps;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The EV Greenhouse's work: grows the seed in its seed slot through AgriCraft's stages with no crop
 * in the world (timing in {@link GreenhouseGrowth}), paying water and fertilizer from the structure's
 * MI fluid input hatches when each stage starts and EU from its MI energy input hatches while it grows,
 * and puts the harvest in its MI item output hatches ({@link GreenhouseHatches}). The seed is never used up.
 *
 * <p>EU is spent the way an MI machine spends it on a recipe: a stage costs
 * {@link GreenhouseSettings#RECIPE_EU} × its expected ticks, and the machine may draw more per tick
 * than that, up to {@link GreenhouseSettings#BASE_MAX_EU} plus its MI upgrades, finishing sooner.
 * Like MI's {@code CrafterComponent}, it only gets there gradually: each harvest adds an efficiency
 * tick, each tick it is starved or idle takes one away, and the per-tick cap is
 * {@code RECIPE_EU + efficiency × cycleEu / 600}, never more than the max nor a whole cycle in one tick.
 */
public class GrowerTrait extends Trait {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(GrowerTrait.class);

    /** What the machine is doing, for the GUI. Lang key {@code gui.roll_mod.ev_greenhouse.status.<name>}. */
    public enum Status {
        NOT_FORMED, NO_SEED, NOT_A_SEED, CANNOT_GROW, NO_WATER, NO_FERTILIZER, NO_ENERGY, NO_OUTPUT,
        OUTPUT_FULL, GROWING;

        public Component text() {
            return Component.translatable("gui.roll_mod.ev_greenhouse.status." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** Plant id of what is growing; empty when nothing is. Read by the crop renderer. */
    @Persisted
    @DescSynced
    private String plantId = "";
    @Persisted
    @DescSynced
    private int stage;
    @Persisted
    @DescSynced
    private int stageTotal;
    /** EU spent on the current stage, and what it costs in all; 0 cost means its water and fertilizer are not paid yet. */
    @Persisted
    @DescSynced
    private long stageEuUsed;
    @Persisted
    @DescSynced
    private long stageEuCost;
    @Persisted
    private int efficiencyTicks;
    /** MI upgrade items, inserted by right-click (see {@code GreenhouseMachines#onUseItem}). */
    @Persisted
    @DescSynced
    private ItemStack upgrades = ItemStack.EMPTY;

    @DescSynced
    private Status status = Status.NOT_FORMED;
    @DescSynced
    private long euPerTick;

    public GrowerTrait(MBDMachine machine, GrowerTraitDefinition definition) {
        super(machine, definition);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public GrowerTraitDefinition getDefinition() {
        return (GrowerTraitDefinition) super.getDefinition();
    }

    @Override
    public void serverTick() {
        MBDMachine machine = getMachine();
        if (!(machine.getLevel() instanceof ServerLevel level)) return;
        if (!(machine instanceof MBDMultiblockMachine multiblock) || !multiblock.isFormed() || !multiblock.isFormedValid()) {
            stop(Status.NOT_FORMED);
            return;
        }

        Direction front = machine.getFrontFacing().orElse(Direction.NORTH);
        GreenhouseHatches hatches = GreenhouseHatches.collect(level, GreenhouseStructure.blocks(machine.getPos(), front));

        ItemStack seed = seedStack();
        AgriGenome genome = GreenhouseGrowth.genome(seed);
        AgriPlant plant = genome == null ? null : GreenhouseGrowth.plant(genome);
        if (plant == null) {
            plantId = "";
            stop(seed.isEmpty() ? Status.NO_SEED : Status.NOT_A_SEED);
            return;
        }

        // A different seed starts over from its first stage.
        String id = genome.getSpeciesGene().getTrait();
        int total = plant.getInitialGrowthStage().total();
        if (!id.equals(plantId) || total != stageTotal) {
            plantId = id;
            stageTotal = total;
            restartStage(plant.getInitialGrowthStage().index());
        }

        AgriGrowthStage current = new AgriGrowthStage(stage, stageTotal);
        if (current.isMature()) {
            if (harvest(level, hatches, plant, genome, current)) {
                restartStage(plant.getGrowthStageAfterHarvest().index());
                efficiencyTicks = Math.min(efficiencyTicks + 1, maxEfficiencyTicks(plant, genome));
            }
            return;
        }

        long ticks = GreenhouseGrowth.stageTicks(plant, current, genome.getGrowth());
        if (ticks < 0) {
            stop(Status.CANNOT_GROW);
            return;
        }
        if (stageEuCost == 0) {
            Status missing = payReagents(hatches);
            if (missing != null) {
                stop(missing);
                return;
            }
            stageEuCost = GreenhouseSettings.RECIPE_EU * ticks;
        }

        long wanted = Math.min(euCap(plant, genome), stageEuCost - stageEuUsed);
        long spent = hatches.consumeEu(wanted);
        euPerTick = spent;
        if (spent < wanted) {
            efficiencyTicks = Math.max(0, efficiencyTicks - 1);
        }
        if (spent == 0) {
            status = Status.NO_ENERGY;
            machine.setMachineState("formed");
            return;
        }
        status = Status.GROWING;
        machine.setMachineState("working");
        stageEuUsed += spent;
        if (stageEuUsed >= stageEuCost) {
            restartStage(stage + 1);
        }
    }

    private void restartStage(int index) {
        stage = index;
        stageEuUsed = 0;
        stageEuCost = 0;
    }

    /** Idle with a reason; idling loses efficiency, as an MI machine between recipes does. */
    private void stop(Status reason) {
        status = reason;
        euPerTick = 0;
        efficiencyTicks = Math.max(0, efficiencyTicks - 1);
        if (getMachine() instanceof MBDMultiblockMachine multiblock && multiblock.isFormed()) {
            getMachine().setMachineState("formed");
        }
    }

    // ── EU and overclocking (MI's CrafterComponent curve) ──────────────────

    /** The most EU/t allowed: the EV base plus whatever the upgrades add. */
    public long maxEuPerTick() {
        long extra = upgrades.isEmpty() ? 0 : upgrades.getCount() * UpgradeComponent.getExtraEu(upgrades.getItem());
        return GreenhouseSettings.BASE_MAX_EU + extra;
    }

    /** One whole regrowth's EU, the "recipe" the overclock ramps against. */
    private static long cycleEu(AgriPlant plant, AgriGenome genome) {
        long ticks = GreenhouseGrowth.cycleTicks(plant, genome.getGrowth());
        return GreenhouseSettings.RECIPE_EU * Math.max(1, ticks);
    }

    private long euCap(AgriPlant plant, AgriGenome genome) {
        long cycleEu = cycleEu(plant, genome);
        long overclocked = GreenhouseSettings.RECIPE_EU + efficiencyTicks * cycleEu / (20 * 30);
        return Math.min(cycleEu, Math.min(overclocked, maxEuPerTick()));
    }

    /** The efficiency at which {@link #euCap} stops growing, as MI's {@code getRecipeMaxEfficiencyTicks}. */
    private int maxEfficiencyTicks(AgriPlant plant, AgriGenome genome) {
        long cycleEu = cycleEu(plant, genome);
        long target = Math.min(maxEuPerTick(), cycleEu) - GreenhouseSettings.RECIPE_EU;
        if (target <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (target * (20 * 30) + cycleEu - 1) / cycleEu);
    }


    // ── Water and fertilizer ────────────────────────────────────────────────

    /** Pays the current stage's water and fertilizer from the fluid input hatches, all or nothing. Returns what is missing, or null. */
    @Nullable
    private Status payReagents(GreenhouseHatches hatches) {
        long water = 0;
        long fertilizer = 0;
        for (ConfigurableFluidStack tank : hatches.fluidInputs) {
            if (isWater(tank)) water += tank.getAmount();
            fertilizer += tank.getAmount() * fertilizerValue(tank) / 1000;
        }
        if (water < GreenhouseSettings.WATER_PER_STAGE) return Status.NO_WATER;
        if (fertilizer < GreenhouseSettings.FERTILIZER_PER_STAGE) return Status.NO_FERTILIZER;

        long waterLeft = GreenhouseSettings.WATER_PER_STAGE;
        long unitsLeft = GreenhouseSettings.FERTILIZER_PER_STAGE;
        for (ConfigurableFluidStack tank : hatches.fluidInputs) {
            if (waterLeft > 0 && isWater(tank)) {
                long drained = Math.min(waterLeft, tank.getAmount());
                tank.decrement(drained);
                waterLeft -= drained;
                continue;
            }
            int perBucket = fertilizerValue(tank);
            if (unitsLeft > 0 && perBucket > 0) {
                // Rounded up, so a stage is never paid with less than it costs.
                long drained = Math.min(tank.getAmount(), (unitsLeft * 1000 + perBucket - 1) / perBucket);
                tank.decrement(drained);
                unitsLeft -= drained * perBucket / 1000;
            }
        }
        return null;
    }

    private static boolean isWater(ConfigurableFluidStack tank) {
        return tank.getAmount() > 0 && tank.getResource().getFluid().defaultFluidState().is(Tags.Fluids.WATER);
    }

    /** Fertilizer per bucket of the tank's fluid ({@code roll_mod:fertilizer} data map), 0 if it is not one. */
    private static int fertilizerValue(ConfigurableFluidStack tank) {
        if (tank.getAmount() <= 0) return 0;
        FertilizerValue value = ModDataMaps.fertilizer(tank.getResource().getFluid());
        return value == null ? 0 : value.amount();
    }

    // ── Harvest ─────────────────────────────────────────────────────────────

    /**
     * Rolls AgriCraft's harvest into the item output hatches. If it does not all fit, nothing is put
     * anywhere and it is rolled again next tick, so a full hatch pauses the machine instead of voiding crops.
     */
    private boolean harvest(ServerLevel level, GreenhouseHatches hatches, AgriPlant plant, AgriGenome genome,
                            AgriGrowthStage stage) {
        if (hatches.itemOutputs.isEmpty()) {
            stop(Status.NO_OUTPUT);
            return false;
        }
        List<ItemStack> drops = new ArrayList<>();
        int rolls = GreenhouseGrowth.harvestRolls(genome.getGain());
        for (int roll = 0; roll < rolls; roll++) {
            plant.getHarvestProducts(drops::add, stage, genome, level.getRandom());
        }
        if (!hatches.fits(drops)) {
            stop(Status.OUTPUT_FULL);
            return false;
        }
        hatches.put(drops);
        status = Status.GROWING;
        return true;
    }

    // ── Structure ───────────────────────────────────────────────────────────

    private ItemStack seedStack() {
        ItemSlotCapabilityTrait slots = getMachine().getTraitByName(ItemSlotCapabilityTrait.class, GreenhouseMachines.SEED_TRAIT);
        return slots == null || slots.storage.getSlots() == 0 ? ItemStack.EMPTY : slots.storage.getStackInSlot(0);
    }

    /** Client: while formed, the structure's blocks are not drawn; the controller's model replaces them. */
    @Override
    public void clientTick() {
        MBDMachine machine = getMachine();
        Direction front = machine.getFrontFacing().orElse(null);
        if (machine instanceof MBDMultiblockMachine multiblock && multiblock.isFormed()
                && front != null && !front.getAxis().isVertical()) {
            HiddenStructureBlocks.set(machine.getPos(), GreenhouseStructure.blocks(machine.getPos(), front));
        } else {
            HiddenStructureBlocks.clear(machine.getPos());
        }
    }

    @Override
    public void onMachineUnLoad() {
        if (getMachine().getLevel() != null && getMachine().getLevel().isClientSide()) {
            HiddenStructureBlocks.clear(getMachine().getPos());
        }
    }

    @Override
    public void onMachineRemoved() {
        onMachineUnLoad();
    }

    // ── Upgrades ────────────────────────────────────────────────────────────

    /** Takes MI upgrade items from {@code held}, as {@code UpgradeComponent.onUse}. True if any were taken. */
    public boolean insertUpgrades(ItemStack held, boolean creative) {
        if (held.isEmpty() || UpgradeComponent.getExtraEu(held.getItem()) <= 0) return false;
        if (upgrades.isEmpty()) {
            upgrades = held.copy();
            if (!creative) held.setCount(0);
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(upgrades, held)) return false;
        int added = Math.min(held.getCount(), upgrades.getMaxStackSize() - upgrades.getCount());
        if (added <= 0) return false;
        upgrades = upgrades.copyWithCount(upgrades.getCount() + added);
        if (!creative) held.shrink(added);
        return true;
    }

    /** Hands the upgrades back. */
    public ItemStack takeUpgrades() {
        ItemStack taken = upgrades;
        upgrades = ItemStack.EMPTY;
        return taken;
    }

    @Override
    public void onMachineDrop(Entity entity, List<ItemStack> drops) {
        if (!upgrades.isEmpty()) drops.add(upgrades.copy());
    }

    // ── Read by the GUI and the renderer ─────────────────────────────────────

    public String getPlantId() {
        return plantId;
    }

    public int getStage() {
        return stage;
    }

    public int getStageTotal() {
        return stageTotal;
    }

    public float stageProgress() {
        return stageEuCost <= 0 ? 0f : (float) Math.min(1.0, (double) stageEuUsed / stageEuCost);
    }

    public Status getStatus() {
        return status;
    }

    public long getEuPerTick() {
        return euPerTick;
    }

    public ItemStack getUpgrades() {
        return upgrades;
    }
}
