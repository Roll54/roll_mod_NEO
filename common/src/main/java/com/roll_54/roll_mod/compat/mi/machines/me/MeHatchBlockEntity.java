package com.roll_54.roll_mod.compat.mi.machines.me;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.api.util.AECableType;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.MachineComponent;
import aztech.modern_industrialization.machines.components.OrientationComponent;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.machines.multiblocks.HatchBlockEntity;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * An MI hatch that is also an AE2 grid node, the shared half of every ME hatch ({@link MeHatches}).
 *
 * <p>The node follows the vending block's: an in-world node that needs a channel, created on the first
 * tick and destroyed on removal or chunk unload, saved by this hatch's own {@link MachineComponent}. All
 * ME work is done from MI's hatch tick ({@link #tickTransfer}), spread over the ticks by position so a
 * wall of hatches does not all move on the same one.
 *
 * <p>Right-click opens an LDLib2 screen ({@link MeHatchUI}) rather than MI's.
 */
public abstract class MeHatchBlockEntity extends HatchBlockEntity implements IInWorldGridNodeHost, IActionHost {

    public static final int ME_OFFLINE = 0;
    public static final int ME_NO_CHANNEL = 1;
    public static final int ME_ONLINE = 2;

    private static final String TAG_STATUS = "me_status";

    private static final IGridNodeListener<MeHatchBlockEntity> NODE_LISTENER = new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(MeHatchBlockEntity hatch, IGridNode node) {
            hatch.setChanged();
        }

        @Override
        public void onStateChanged(MeHatchBlockEntity hatch, IGridNode node, State state) {
            hatch.updateMeStatus();
        }
    };

    protected final IManagedGridNode mainNode;
    protected final IActionSource actionSource;
    private int meStatus = ME_OFFLINE;
    /** Something the client shows changed; synced on the next server tick, never mid-load. */
    private boolean clientDirty;

    protected MeHatchBlockEntity(BEP bep, ResourceLocation id) {
        // Has an output side, so the output overlay shows and the wrench turns it; nothing auto-extracts.
        super(bep, new MachineGuiParameters.Builder(id, false).build(), OrientationComponent.Params.noFacing(false, false));
        this.mainNode = GridHelper.createManagedNode(this, NODE_LISTENER)
                .setInWorldNode(true)
                .setTagName("proxy")
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setVisualRepresentation(bep.state().getBlock());
        this.actionSource = IActionSource.ofMachine(this);
        registerComponents(new MeComponent());
    }

    /* ------------------------------------------------ subclass hooks ------------------------------------------------ */

    /** ME work, every server tick. Use {@link #every} to run on an interval. */
    protected abstract void tickMe(long gameTime);

    /** Saved data of the hatch itself (config, buffer). */
    protected abstract void writeMeData(CompoundTag tag, HolderLookup.Provider registries);

    protected abstract void readMeData(CompoundTag tag, HolderLookup.Provider registries);

    /** Data the client screen reads from the block entity (not stock, which goes through the menu). */
    protected void writeClientMeData(CompoundTag tag, HolderLookup.Provider registries) {}

    protected void readClientMeData(CompoundTag tag, HolderLookup.Provider registries) {}

    /** The screen, built on both sides. */
    public abstract ModularUI createUI(Player player);

    /* ---------------------------------------------------- ME side --------------------------------------------------- */

    /** The grid, when this hatch may use it: powered, with a channel. */
    protected @Nullable IGrid activeGrid() {
        return mainNode.isActive() ? mainNode.getGrid() : null;
    }

    /** Puts up to {@code amount} into the network, paying AE energy; returns how much went in. */
    protected long pushToMe(IGrid grid, AEKey key, long amount) {
        if (amount <= 0) return 0;
        return StorageHelper.poweredInsert(grid.getEnergyService(), grid.getStorageService().getInventory(),
                key, amount, actionSource);
    }

    /** Takes up to {@code amount} from the network, paying AE energy; returns how much came out. */
    protected long pullFromMe(IGrid grid, AEKey key, long amount) {
        if (amount <= 0) return 0;
        return StorageHelper.poweredExtraction(grid.getEnergyService(), grid.getStorageService().getInventory(),
                key, amount, actionSource);
    }

    /** True once every {@code interval} ticks, at a phase of its own for this position. */
    protected boolean every(long gameTime, int interval) {
        return Math.floorMod(gameTime + worldPosition.hashCode(), interval) == 0;
    }

    /** {@link #ME_OFFLINE}, {@link #ME_NO_CHANNEL} or {@link #ME_ONLINE}; synced to the client. */
    public int getMeStatus() {
        return meStatus;
    }

    private void updateMeStatus() {
        int status;
        if (mainNode.isActive()) status = ME_ONLINE;
        else if (mainNode.isPowered()) status = ME_NO_CHANNEL;
        else status = ME_OFFLINE;
        if (status != meStatus) {
            meStatus = status;
            markClientDirty();
        }
    }

    /** Asks for a client sync on the next server tick. */
    protected void markClientDirty() {
        clientDirty = true;
    }

    public static Component statusText(int status) {
        return switch (status) {
            case ME_ONLINE -> Component.translatable("menu.roll_mod.me_hatch.online").withStyle(ChatFormatting.DARK_GREEN);
            case ME_NO_CHANNEL -> Component.translatable("menu.roll_mod.me_hatch.no_channel").withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("menu.roll_mod.me_hatch.offline").withStyle(ChatFormatting.RED);
        };
    }

    @Override
    public @Nullable IGridNode getGridNode(Direction dir) {
        return mainNode.getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    @Override
    public @Nullable IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    /* --------------------------------------------------- lifecycle -------------------------------------------------- */

    @Override
    protected void tickTransfer() {
        tickMe(level.getGameTime());
        if (clientDirty) {
            clientDirty = false;
            // No remesh: nothing in the model changed.
            sync(false);
        }
    }

    @Override
    public void onPlaced(@Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(placer, itemStack);
        if (placer instanceof Player player) mainNode.setOwningPlayer(player);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        // Client side this does nothing; server side the node joins on the first tick.
        GridHelper.onFirstTick(this, hatch -> hatch.mainNode.create(hatch.getLevel(), hatch.getBlockPos()));
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        mainNode.destroy();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        mainNode.destroy();
    }

    @Override
    public void openMenu(ServerPlayer player) {
        BlockUIMenuType.openUI(player, getBlockPos());
    }

    /** The node, this hatch's data and, for the client, the ME status. */
    private final class MeComponent implements MachineComponent {
        @Override
        public void writeNbt(CompoundTag tag, HolderLookup.Provider registries) {
            mainNode.saveToNBT(tag);
            writeMeData(tag, registries);
        }

        @Override
        public void readNbt(CompoundTag tag, HolderLookup.Provider registries, boolean isUpgradingMachine) {
            mainNode.loadFromNBT(tag);
            readMeData(tag, registries);
        }

        @Override
        public void writeClientNbt(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt(TAG_STATUS, meStatus);
            writeClientMeData(tag, registries);
        }

        @Override
        public void readClientNbt(CompoundTag tag, HolderLookup.Provider registries) {
            meStatus = tag.getInt(TAG_STATUS);
            readClientMeData(tag, registries);
        }
    }
}
