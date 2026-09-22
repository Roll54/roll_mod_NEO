package com.roll_54.roll_mod.blocks.entity;

import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import com.roll_54.roll_mod.hydroponics.HydroponicEnergyPort;
import com.roll_54.roll_mod.hydroponics.HydroponicFluidPort;
import com.roll_54.roll_mod.hydroponics.HydroponicNode;
import com.roll_54.roll_mod.hydroponics.HydroponicNodeData;
import com.roll_54.roll_mod.hydroponics.HydroponicReagents;
import com.roll_54.roll_mod.hydroponics.HydroponicSettings;
import com.roll_54.roll_mod.hydroponics.NodeItemHandlerView;
import com.roll_54.roll_mod.registry.BlockEntites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * One Hydroponic Garden Bed.
 *
 * <p>The block entity owns almost nothing: settings, buffers and the three input slots live on the
 * shared {@link HydroponicNode} in level saved data, and this class is the terminal that reaches
 * them. What it does own is a small snapshot — the settings plus whether this particular bed was
 * supplied last tick — which is synced to the client, because {@code AgriApiSoilMixin} has to answer
 * {@code getSoil} on both sides.
 *
 * <p>Every loaded bed ticks and pays its own upkeep, which is what makes "2500 EU/t per bed" true
 * and lets beds in unloaded chunks cost nothing.
 *
 * <p>The snapshot rides LdLib2's managed storage: {@code @Persisted} writes it to the bed's NBT and
 * {@code @DescSynced} pushes it to tracking clients, both through the mixins LdLib2 puts on vanilla
 * {@code BlockEntity}. That is why there is no update packet, no update tag and no dirty-flag
 * bookkeeping here — an assignment to a managed field is the whole of it.
 */
public class HydroponicGardenBedBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {

    // HydroponicSettings is a record and LdLib2 has no record accessor, so the snapshot is stored
    // decomposed and rebuilt on read. The record stays the type everything else speaks.
    @Persisted
    @DescSynced
    private int ph = HydroponicSettings.DEFAULT.ph();
    @Persisted
    @DescSynced
    private int water = HydroponicSettings.DEFAULT.water();
    @Persisted
    @DescSynced
    private int fertility = HydroponicSettings.DEFAULT.fertility();
    @Persisted
    @DescSynced
    private boolean enabled = HydroponicSettings.DEFAULT.enabled();

    @Persisted
    @DescSynced
    private boolean supplied;

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    /**
     * Client-side stand-in for the node's shared slots. Still needed with the LdLib2 UI: its item
     * slots are real container slots, so vanilla slot sync writes the server's stacks back through
     * {@code Slot#set}, and on a client — where {@link #node()} is always null — this is what it
     * writes into.
     */
    private final ItemStackHandler clientInputs = new ItemStackHandler(3);

    /**
     * Resolved lazily on every call: merging or splitting replaces the node object, and an open
     * menu holding the old slot handler would silently swallow whatever the player put in it.
     */
    private final NodeItemHandlerView inputsView = new NodeItemHandlerView(this::resolveInputs);

    private final HydroponicEnergyPort energyPort = new HydroponicEnergyPort(this::node);
    private final HydroponicFluidPort fluidPort = new HydroponicFluidPort(this::node);

    public HydroponicGardenBedBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntites.HYDROPONIC_GARDEN_BED_BE.get(), pos, state);
    }

    // ---------------------------------------------------------------- node access

    @Nullable
    public HydroponicNode node() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return HydroponicNodeData.get(serverLevel).nodeAt(worldPosition);
    }

    /**
     * Copies the node's shared state into this bed's client-visible snapshot.
     *
     * <p>The equality guard is kept: writing a managed field marks it dirty whether or not the value
     * moved, and a 25-bed node would otherwise re-sync four fields per bed per tick for nothing.
     */
    public void adoptNode(HydroponicNode node) {
        HydroponicSettings incoming = node.settings();
        if (settings().equals(incoming)) {
            return;
        }
        ph = incoming.ph();
        water = incoming.water();
        fertility = incoming.fertility();
        enabled = incoming.enabled();
    }

    public HydroponicSettings settings() {
        return new HydroponicSettings(ph, water, fertility, enabled);
    }

    public boolean isSupplied() {
        return supplied;
    }

    public IItemHandler inputs() {
        return inputsView;
    }

    private IItemHandler resolveInputs() {
        HydroponicNode node = node();
        return node == null ? clientInputs : node.inputs();
    }

    public HydroponicEnergyPort energyPort() {
        return energyPort;
    }

    public HydroponicFluidPort fluidPort() {
        return fluidPort;
    }

    // ---------------------------------------------------------------- tick

    public static void tick(Level level, BlockPos pos, BlockState state, HydroponicGardenBedBlockEntity bed) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        HydroponicNodeData data = HydroponicNodeData.get(serverLevel);
        HydroponicNode node = data.nodeAt(pos);
        if (node == null) {
            // Covers /setblock, structure placement and worlds from before this block existed.
            bed.adoptNode(data.attach(serverLevel, pos));
            return;
        }
        bed.adoptNode(node);

        // Draining the shared input slots is node-level work: it must happen once per tick, not
        // once per member, or a 25-bed node would empty a bucket 25x as fast.
        if (node.claimInputTick(level.getGameTime())) {
            for (HydroponicReagents.Kind kind : HydroponicReagents.Kind.values()) {
                HydroponicReagents.processSlot(node, kind);
            }
        }

        // Guarded for the same reason adoptNode is: the flag can flap every tick on a marginal
        // buffer, and only a real change should reach the wire.
        boolean nowSupplied = node.consumeOneBed();
        if (bed.supplied != nowSupplied) {
            bed.supplied = nowSupplied;
        }
    }

    // ---------------------------------------------------------------- persistence

    // Nothing is written by hand any more: super.saveAdditional is where LdLib2's mixin stores the
    // managed fields, and the same mixin supplies getUpdateTag and getUpdatePacket.

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // One-time migration off the hand-rolled format. Beds saved before the move to managed
        // storage carry their snapshot under "settings"/"supplied"; those keys are no longer
        // written, so this reads them once and the next save drops them.
        if (tag.contains("settings")) {
            HydroponicSettings legacy = HydroponicSettings.load(tag.getCompound("settings"));
            ph = legacy.ph();
            water = legacy.water();
            fertility = legacy.fertility();
            enabled = legacy.enabled();
            supplied = tag.getBoolean("supplied");
        }
    }

    /** LdLib2's managed storage. The one method with no default in {@code ISyncPersistRPCBlockEntity}. */
    @Override
    public IManagedStorage getSyncStorage() {
        return syncStorage;
    }
}
