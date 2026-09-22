package com.roll_54.roll_mod.hydroponics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One merged group of Hydroponic Garden Beds: the shared settings, the shared buffers and the three
 * shared input slots that every member of the group sees.
 *
 * <p>Buffer capacity grows with the group — {@code base * (1 + 0.1 * (members - 1))}, so a full 5x5
 * field holds 3.4x what a lone bed does. The acidity reservoir is the exception: it is a fixed
 * -10000..+10000 range no matter how many beds are merged.
 *
 * <p>Consumption is <b>not</b> pooled: every member debits this node for itself every tick, so a
 * 25-bed node really does burn 25 x 2500 EU/t. That also means a bed in an unloaded chunk costs
 * nothing, because it is not ticking.
 */
public class HydroponicNode {

    public static final int MAX_MEMBERS = 25;

    public static final int ACIDITY_RANGE = 10_000;
    public static final int BASE_WATER_CAPACITY = 10_000;       // 10 buckets
    public static final long BASE_ENERGY_CAPACITY = 1_000_000L; // 1M EU
    public static final int BASE_FERTILIZER_CAPACITY = 5_000;   // 5 buckets

    public static final int SLOT_ACIDITY = 0;
    public static final int SLOT_WATER = 1;
    public static final int SLOT_FERTILIZER = 2;

    private final int id;
    private final Runnable onChanged;

    private HydroponicSettings settings = HydroponicSettings.DEFAULT;
    private final Set<BlockPos> members = new LinkedHashSet<>();

    private int acidity;
    private int water;
    private long energy;
    private int fertilizer;

    /** Last fluid accepted into the fertilizer buffer, kept only so the GUI and pipes can name it. */
    private String fertilizerFluidId = "";

    /** Guards the once-per-node-per-tick input slot pass; not persisted. */
    private long lastInputTick = Long.MIN_VALUE;

    public HydroponicNode(int id, Runnable onChanged) {
        this.id = id;
        this.onChanged = onChanged;
    }

    private final ItemStackHandler inputs = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            markChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return HydroponicReagents.accepts(kindOf(slot), stack);
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            // A drained container is handed straight back to the slot it came from, which is only
            // safe if the slot held exactly one. Without this a hopper could stack 16 buckets here
            // and 15 of them would vanish the moment the first was emptied.
            return HydroponicReagents.fluidHandlerOf(stack) != null ? 1 : super.getStackLimit(slot, stack);
        }
    };

    private static HydroponicReagents.Kind kindOf(int slot) {
        return switch (slot) {
            case SLOT_WATER -> HydroponicReagents.Kind.WATER;
            case SLOT_FERTILIZER -> HydroponicReagents.Kind.FERTILIZER;
            default -> HydroponicReagents.Kind.ACIDITY;
        };
    }

    public int id() {
        return id;
    }

    public ItemStackHandler inputs() {
        return inputs;
    }

    public Set<BlockPos> members() {
        return members;
    }

    public int size() {
        return Math.max(1, members.size());
    }

    public void markChanged() {
        onChanged.run();
    }

    // ---------------------------------------------------------------- settings

    public HydroponicSettings settings() {
        return settings;
    }

    public void setSettings(HydroponicSettings value) {
        if (!settings.equals(value)) {
            settings = value;
            markChanged();
        }
    }

    // ---------------------------------------------------------------- capacities

    private int scaled(int base) {
        return (int) Math.round(base * (1.0D + 0.1D * (size() - 1)));
    }

    private long scaled(long base) {
        return Math.round(base * (1.0D + 0.1D * (size() - 1)));
    }

    public int waterCapacity() {
        return scaled(BASE_WATER_CAPACITY);
    }

    public long energyCapacity() {
        return scaled(BASE_ENERGY_CAPACITY);
    }

    public int fertilizerCapacity() {
        return scaled(BASE_FERTILIZER_CAPACITY);
    }

    /** Re-clamps every buffer after the member count changed. */
    public void clampBuffers() {
        acidity = Math.clamp(acidity, -ACIDITY_RANGE, ACIDITY_RANGE);
        water = Math.clamp(water, 0, waterCapacity());
        energy = Math.clamp(energy, 0L, energyCapacity());
        fertilizer = Math.clamp(fertilizer, 0, fertilizerCapacity());
    }

    // ---------------------------------------------------------------- buffers

    public int acidity() {
        return acidity;
    }

    public int water() {
        return water;
    }

    public long energy() {
        return energy;
    }

    public int fertilizer() {
        return fertilizer;
    }

    public String fertilizerFluidId() {
        return fertilizerFluidId;
    }

    /**
     * Adds signed acidity units, clamped to the fixed range.
     *
     * @return how much was actually accepted
     */
    public int addAcidity(int amount, boolean simulate) {
        int target = Math.clamp((long) acidity + amount, -ACIDITY_RANGE, ACIDITY_RANGE);
        int accepted = target - acidity;
        if (!simulate && accepted != 0) {
            acidity = target;
            markChanged();
        }
        return accepted;
    }

    public int addWater(int amount, boolean simulate) {
        int accepted = Math.min(amount, waterCapacity() - water);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            water += accepted;
            markChanged();
        }
        return accepted;
    }

    public long addEnergy(long amount, boolean simulate) {
        long accepted = Math.min(amount, energyCapacity() - energy);
        if (accepted <= 0) {
            return 0L;
        }
        if (!simulate) {
            energy += accepted;
            markChanged();
        }
        return accepted;
    }

    public int addFertilizer(int amount, String fluidId, boolean simulate) {
        int accepted = Math.min(amount, fertilizerCapacity() - fertilizer);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            fertilizer += accepted;
            if (fluidId != null && !fluidId.isEmpty()) {
                fertilizerFluidId = fluidId;
            }
            markChanged();
        }
        return accepted;
    }

    public long extractEnergy(long amount, boolean simulate) {
        long taken = Math.min(amount, energy);
        if (taken <= 0) {
            return 0L;
        }
        if (!simulate) {
            energy -= taken;
            markChanged();
        }
        return taken;
    }

    /**
     * Runs one bed's worth of upkeep against this node.
     *
     * <p>All four resources are checked before anything is spent, so a bed that cannot afford its
     * tick pays nothing at all and simply reports the inert soil instead.
     *
     * @return {@code true} if the bed is fully supplied this tick
     */
    public boolean consumeOneBed() {
        if (!settings.enabled()) {
            return false;
        }
        int waterCost = settings.waterCost();
        int fertilizerCost = settings.fertilizerCost();
        int acidityDelta = settings.acidityDelta();

        if (energy < HydroponicSettings.ENERGY_COST) {
            return false;
        }
        if (water < waterCost) {
            return false;
        }
        if (fertilizer < fertilizerCost) {
            return false;
        }
        // An acidic setting eats stored acid, an alkaline one eats stored base; either way the
        // buffer may not cross zero, so the reservoir must hold at least the whole step.
        if (acidityDelta < 0 && acidity < -acidityDelta) {
            return false;
        }
        if (acidityDelta > 0 && acidity > -acidityDelta) {
            return false;
        }

        energy -= HydroponicSettings.ENERGY_COST;
        water -= waterCost;
        fertilizer -= fertilizerCost;
        acidity += acidityDelta;
        markChanged();
        return true;
    }

    /** True once per game tick, for the node-wide work that must not run once per member. */
    public boolean claimInputTick(long gameTime) {
        if (lastInputTick == gameTime) {
            return false;
        }
        lastInputTick = gameTime;
        return true;
    }

    // ---------------------------------------------------------------- merging / splitting

    /** Folds {@code other}'s buffers and slot contents into this node. Members are not touched. */
    public void absorbBuffers(HydroponicNode other) {
        acidity = Math.clamp((long) acidity + other.acidity, -ACIDITY_RANGE, ACIDITY_RANGE);
        water += other.water;
        energy += other.energy;
        fertilizer += other.fertilizer;
        if (fertilizerFluidId.isEmpty()) {
            fertilizerFluidId = other.fertilizerFluidId;
        }
        markChanged();
    }

    /** Overwrites this node's buffers wholesale — used when a broken bed splits a node apart. */
    public void setBuffers(int acidity, int water, long energy, int fertilizer, String fertilizerFluidId) {
        this.acidity = Math.clamp(acidity, -ACIDITY_RANGE, ACIDITY_RANGE);
        this.water = water;
        this.energy = energy;
        this.fertilizer = fertilizer;
        this.fertilizerFluidId = fertilizerFluidId == null ? "" : fertilizerFluidId;
        markChanged();
    }

    // ---------------------------------------------------------------- persistence

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("id", id);
        tag.put("settings", settings.save());
        tag.putInt("acidity", acidity);
        tag.putInt("water", water);
        tag.putLong("energy", energy);
        tag.putInt("fertilizer", fertilizer);
        tag.putString("fertilizer_fluid", fertilizerFluidId);
        tag.put("inputs", inputs.serializeNBT(registries));
        long[] packed = new long[members.size()];
        int i = 0;
        for (BlockPos pos : members) {
            packed[i++] = pos.asLong();
        }
        tag.putLongArray("members", packed);
        return tag;
    }

    public static HydroponicNode load(CompoundTag tag, HolderLookup.Provider registries, Runnable onChanged) {
        HydroponicNode node = new HydroponicNode(tag.getInt("id"), onChanged);
        node.settings = HydroponicSettings.load(tag.getCompound("settings"));
        node.acidity = tag.getInt("acidity");
        node.water = tag.getInt("water");
        node.energy = tag.getLong("energy");
        node.fertilizer = tag.getInt("fertilizer");
        node.fertilizerFluidId = tag.getString("fertilizer_fluid");
        node.inputs.deserializeNBT(registries, tag.getCompound("inputs"));
        for (long packed : tag.getLongArray("members")) {
            node.members.add(BlockPos.of(packed));
        }
        return node;
    }
}
