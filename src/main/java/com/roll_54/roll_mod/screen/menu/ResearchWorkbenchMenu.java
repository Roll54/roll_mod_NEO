package com.roll_54.roll_mod.screen.menu;

import com.roll_54.roll_mod.blocks.entity.ResearchWorkbenchBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

import static com.roll_54.roll_mod.blocks.entity.ResearchWorkbenchBlockEntity.*;
import static com.roll_54.roll_mod.init.BlockRegistry.RESEARCH_WORKBENCH;
import static com.roll_54.roll_mod.init.MenuTypes.RESEARCH_WORKBENCH_MENU;

public class ResearchWorkbenchMenu extends AbstractContainerMenu {

    public final ResearchWorkbenchBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    /** Client-side constructor (called from FriendlyByteBuf factory) */
    public ResearchWorkbenchMenu(int containerId, Inventory inv, FriendlyByteBuf extra) {
        this(containerId, inv, inv.player.level().getBlockEntity(extra.readBlockPos()), new SimpleContainerData(2));
    }

    /** Server-side constructor */
    public ResearchWorkbenchMenu(int containerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(RESEARCH_WORKBENCH_MENU.get(), containerId);
        this.blockEntity = (ResearchWorkbenchBlockEntity) entity;
        this.level = inv.player.level();
        this.data  = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // TE slots: main input (left), catalyst (middle), output (right)
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, MAIN_INPUT_SLOT, 9, 34));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, CATALYST_SLOT,   27, 34));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, OUTPUT_SLOT,    116, 34));

        addDataSlots(data);
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress    = data.get(0);
        int maxProgress = data.get(1);
        int barWidth    = 68;
        return (maxProgress != 0 && progress != 0) ? progress * barWidth / maxProgress : 0;
    }

    // ── Slot layout constants ─────────────────────────────────────────────────
    private static final int HOTBAR_SLOT_COUNT          = 9;
    private static final int PLAYER_INV_ROW_COUNT       = 3;
    private static final int PLAYER_INV_COL_COUNT       = 9;
    private static final int PLAYER_INV_SLOT_COUNT      = PLAYER_INV_ROW_COUNT * PLAYER_INV_COL_COUNT;
    private static final int VANILLA_SLOT_COUNT         = HOTBAR_SLOT_COUNT + PLAYER_INV_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX   = 0;
    private static final int TE_INVENTORY_FIRST_SLOT    = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT    = 3;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot source = slots.get(index);
        if (source == null || !source.hasItem()) return ItemStack.EMPTY;
        ItemStack srcStack  = source.getItem();
        ItemStack srcCopy   = srcStack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(srcStack, TE_INVENTORY_FIRST_SLOT,
                    TE_INVENTORY_FIRST_SLOT + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(srcStack, VANILLA_FIRST_SLOT_INDEX,
                    VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (srcStack.getCount() == 0) source.set(ItemStack.EMPTY);
        else source.setChanged();

        source.onTake(player, srcStack);
        return srcCopy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, RESEARCH_WORKBENCH.get());
    }

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
        }
    }
}
