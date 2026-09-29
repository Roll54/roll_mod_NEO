package com.roll_54.roll_mod.blocks.entity;

import com.roll_54.roll_mod.items.modulardrill.ModularTool;
import com.roll_54.roll_mod.registry.BlockEntites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Module Installation Table's inside: a single slot for the drill being worked on.
 *
 * <p>Only the drill lives here. The modules never do — they exist inside the drill stack's own
 * component, and the GUI's module slots are windows into that component, so a drill picked up
 * mid-session takes its modules with it and nothing is left behind to dupe or lose.
 */
public class ModuleInstallationTableBlockEntity extends BlockEntity {

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ModularTool;
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            drillChanged();
        }
    };

    /** Spun by the renderer, the way the pedestal turns its item. Client-side only state. */
    private float rotation;

    public ModuleInstallationTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntites.MODULE_INSTALLATION_TABLE_BE.get(), pos, blockState);
    }

    /** The drill on the table, or {@link ItemStack#EMPTY}. */
    public ItemStack drill() {
        return inventory.getStackInSlot(0);
    }

    /**
     * Saves and pushes the drill to clients. Called by the slot handler as well as the inventory:
     * installing a module rewrites the drill's component <em>in place</em>, which the handler's
     * own change hook never sees — and the GUI's labels and the spinning render both read the
     * client copy of this block entity, so a silent change would leave them stale.
     */
    public void drillChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public float getRenderingRotation() {
        rotation += 0.5f;
        if (rotation >= 360) {
            rotation = 0;
        }
        return rotation;
    }

    public void drops() {
        SimpleContainer inv = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            inv.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inv);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
