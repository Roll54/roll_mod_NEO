package com.roll_54.roll_mod.blocks.entity;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.crop.AgriGrowthStage;
import com.agricraft.agricraft.api.plant.AgriWeed;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.roll_54.roll_mod.compat.argicraft.AgriHerbicide;
import com.roll_54.roll_mod.compat.argicraft.HerbicideHelper;
import com.roll_54.roll_mod.registry.BlockEntites;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import java.util.ArrayList;

public class CropManagerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {

    private static final long CAPACITY = 100000L;
    private static final long MAX_TRANSFER = 1000L;
    private static final long COST_PER_OPERATION = 500L;

    private final EnergyStorage energyStorage = new EnergyStorage((int) CAPACITY, (int) MAX_TRANSFER, (int) MAX_TRANSFER, 0);
    private int tickCounter = 0;
    
    // 0-7: Harvested crops
    // 8: Herbicide
    // 9: Biomass
    private final NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);

    public CropManagerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntites.CROP_MANAGER_BE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CropManagerBlockEntity blockEntity) {
        if (level == null || level.isClientSide) return;

        blockEntity.tickCounter++;
        // Check for crops to remove weeds every 10 ticks
        if (blockEntity.tickCounter >= 1) {
            blockEntity.tickCounter = 0;
            blockEntity.processCropsInRange(level);
        }
    }

    /**
     * Detect and process all CropBlockEntity instances in the detection range
     */
    private void processCropsInRange(Level level) {
        BlockPos center = getBlockPos();

        int startX = center.getX() - 5;
        int endX = center.getX() + 5;
        int startZ = center.getZ() - 5;
        int endZ = center.getZ() + 5;
        int startY = center.getY();
        int endY = center.getY();

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                for (int y = startY; y <= endY; y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    AgriApi.getCrop(level, checkPos).ifPresent(crop -> {
                        if (crop.getLevel() == null) return;

                        boolean hasBiomassSpace = hasSpace(9, 10);
                        boolean hasCropSpace = hasSpace(0, 8);

                        if (canOperate() && crop.hasWeeds() && hasBiomassSpace && hasCropSpace
                                && !HerbicideHelper.isWeedResistant(crop)) {
                            ItemStack herbicideStack = this.items.get(8);
                            if (herbicideStack.getCount() > 0
                                    && herbicideStack.has(ComponentsRegistry.HERBICIDE.get())
                                    && crop instanceof CropBlockEntity cropEntity
                                    && cropEntity instanceof AgriHerbicide herbicide
                            ) {
                                int perItem = herbicideStack.getOrDefault(ComponentsRegistry.HERBICIDE.get(), 0);
                                if (perItem <= 0) {
                                    return;
                                }
                                int available = perItem * herbicideStack.getCount();
                                if (herbicide.roll_mod$getHerbicideAmount() > available * 2) {
                                    return;
                                }
                                if (HerbicideHelper.applyHerbicide(herbicideStack, cropEntity)) {
                                    AgriWeed weed = crop.getWeed();
                                    AgriGrowthStage stage = crop.getWeedGrowthStage();

                                    crop.removeWeeds();
                                    herbicideStack.shrink(1);
                                    energyStorage.extractEnergy((int) COST_PER_OPERATION, false);

                                    ArrayList<ItemStack> drops = new ArrayList<>();
                                    weed.onRake(stage, drops::add, crop.getLevel().getRandom(), null);
                                    for (ItemStack stack : drops) {
                                        insertItemIntoSlots(stack, 0, 8);
                                    }

                                    ItemStack biomassStack = new ItemStack(ItemRegistry.BIOMASS.get(), 1);
                                    insertItemIntoSlots(biomassStack, 9, 10);
                                }
                            }
                        }
                        if (canOperate() && crop.hasPlant() && crop.isFullyGrown() && hasCropSpace) {
                            crop.harvest((stack) -> {
                                insertItemIntoSlots(stack, 0, 8);
                            }, null);
                            energyStorage.extractEnergy((int) COST_PER_OPERATION, false);
                        }
                    });
                }
            }
        }
    }

    private boolean hasSpace(int start, int end) {
        for (int i = start; i < end; i++) {
            ItemStack stack = this.items.get(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if we have enough energy to perform the operation
     */
    private boolean canOperate() {
        return energyStorage.getEnergyStored() >= COST_PER_OPERATION;
    }

    private void insertItemIntoSlots(ItemStack stack, int start, int end) {
        for (int i = start; i < end; i++) {
            if (stack.isEmpty()) break;
            ItemStack slotStack = this.items.get(i);
            if (slotStack.isEmpty()) {
                this.items.set(i, stack.copy());
                stack.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                int toAdd = Math.min(space, stack.getCount());
                if (toAdd > 0) {
                    slotStack.grow(toAdd);
                    stack.shrink(toAdd);
                }
            }
        }
    }

    // ── Energy Storage Implementation ─────────────────────────────────────

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // ── NBT Serialization ────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("energy", energyStorage.getEnergyStored());
        ContainerHelper.saveAllItems(tag, this.items, registries);
        super.saveAdditional(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        long energy = tag.getLong("energy");
        energyStorage.receiveEnergy((int) Math.min(energy, Integer.MAX_VALUE), false);
        this.items.clear();
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    // ── Network Synchronization ──────────────────────────────────────────

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.roll_mod.crop_manager");
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.UP) {
            return new int[]{8};
        } else if (direction == Direction.DOWN) {
            return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 9};
        } else {
            return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 9};
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @org.jspecify.annotations.Nullable Direction direction) {
        return i == 8 && itemStack.has(ComponentsRegistry.HERBICIDE.get());
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return i != 8;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int i) {
        return this.items.get(i);
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        return ContainerHelper.removeItem(this.items, i, i1);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ContainerHelper.takeItem(this.items, i);
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        this.items.set(i, itemStack);
        if (itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public @org.jspecify.annotations.Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new com.roll_54.roll_mod.gui.menu.CropManagerMenu(i, inventory, this);
    }
}
