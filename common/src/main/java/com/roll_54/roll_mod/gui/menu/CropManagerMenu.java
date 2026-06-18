package com.roll_54.roll_mod.gui.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.NonNull;

import static com.roll_54.roll_mod.registry.MenuTypes.CROP_MANAGER_MENU;

public class CropManagerMenu extends AbstractContainerMenu {
    private final Container container;
    private final BlockEntity blockEntity;
    private final Level level;

    public CropManagerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public CropManagerMenu(int pContainerId, Inventory inv, BlockEntity entity) {
        super(CROP_MANAGER_MENU.get(), pContainerId);
        if (!(entity instanceof Container)) {
            throw new IllegalStateException("BlockEntity is not a Container: " + entity);
        }
        this.blockEntity = entity;
        this.container = (Container) entity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // Harvested crops (0-7)
        this.addSlot(new Slot(this.container, 0, 8, 20));
        this.addSlot(new Slot(this.container, 1, 26, 20));
        this.addSlot(new Slot(this.container, 2, 8, 38));
        this.addSlot(new Slot(this.container, 3, 26, 38));
        this.addSlot(new Slot(this.container, 4, 44, 20));
        this.addSlot(new Slot(this.container, 5, 62, 20));
        this.addSlot(new Slot(this.container, 6, 44, 38));
        this.addSlot(new Slot(this.container, 7, 62, 38));

        // Herbicide (8)
        this.addSlot(new Slot(this.container, 8, 116, 20));

        // Biomass (9)
        this.addSlot(new Slot(this.container, 9, 116, 37));
    }

    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = 36;
    private static final int TE_INVENTORY_SLOT_COUNT = 10;

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX) { // Vanilla slot
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) { // TE slot
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(@NonNull Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
