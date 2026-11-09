package com.roll_54.roll_mod.compat.mi.machines.me;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ME item output hatch: {@link MeHatches#OUTPUT_SLOTS} buffer slots, each holding one item type in any
 * amount, pushed into ME every {@link MeHatches#FLUSH_INTERVAL} ticks.
 *
 * <p>MI caps an item slot at one stack per insert, and its crafters write outputs as
 * {@code maxStackSize - amount}, so a slot claiming more room would break them. MI therefore puts its output
 * into {@link MeHatches#ITEM_RECEIVING_SLOTS} ordinary output slots, which are emptied into the buffer every
 * tick, whatever the state of ME. Emptying a slot keeps MI's machine lock (the output reservation it takes
 * when a recipe starts), and only the crafter fills these slots, so a reserved output always fits.
 *
 * <p>The multiblock never waits on this hatch, except when all three buffer slots hold other types that have
 * not flushed yet (ME full or offline): then a fourth type stays in its receiving slot until one empties.
 *
 * <p>Broken, the buffer first goes to ME; what ME will not take travels on the dropped hatch item
 * ({@link ComponentsRegistry#ME_HATCH_BUFFER}, copied on by {@link MeHatchBlock}) and comes back when it is
 * placed again, rather than spilling as item entities.
 */
public class MeItemOutputHatchBlockEntity extends MeHatchBlockEntity {

    private static final String TAG_BUFFER = "me_buffer";
    private static final String TAG_BLOCKED = "me_blocked";

    private final MIInventory inventory;
    private final AEKey[] bufferKeys = new AEKey[MeHatches.OUTPUT_SLOTS];
    private final long[] bufferAmounts = new long[MeHatches.OUTPUT_SLOTS];
    /** The last flush left something behind: ME is full or offline. Shown on the screen. */
    private boolean blocked;
    /** Game time the buffer was last copied onto a drop; that tick, breaking must not also flush it. */
    private long handedOutAt = Long.MIN_VALUE;

    public MeItemOutputHatchBlockEntity(BEP bep, ResourceLocation id) {
        super(bep, id);
        List<ConfigurableItemStack> slots = new ArrayList<>(MeHatches.ITEM_RECEIVING_SLOTS);
        for (int i = 0; i < MeHatches.ITEM_RECEIVING_SLOTS; i++) {
            slots.add(ConfigurableItemStack.standardOutputSlot());
        }
        // Slot positions only matter to MI's own screen, which these hatches do not use.
        this.inventory = new MIInventory(slots, Collections.emptyList(),
                new SlotPositions.Builder().addSlots(8, 18, 9, 3).build(), SlotPositions.empty());
        registerComponents(inventory);
    }

    /** Buffer slot {@code i}, for the screen; {@code null} when empty. */
    public @Nullable GenericStack getBuffer(int slot) {
        return bufferKeys[slot] == null ? null : new GenericStack(bufferKeys[slot], bufferAmounts[slot]);
    }

    public boolean isBlocked() {
        return blocked;
    }

    /* ------------------------------------------------------ ME ------------------------------------------------------ */

    @Override
    protected void tickMe(long gameTime) {
        drainReceivingSlots();
        if (every(gameTime, MeHatches.FLUSH_INTERVAL)) flush();
    }

    /** Moves whatever MI output into the buffer slot of the same item, else into an empty one. */
    private void drainReceivingSlots() {
        for (ConfigurableItemStack stack : inventory.getItemStacks()) {
            long amount = stack.getAmount();
            if (amount <= 0) continue;
            AEKey key = MeKeys.of(stack.getResource());
            if (key == null) continue;
            int slot = bufferSlotFor(key);
            if (slot < 0) continue; // three other types are waiting
            bufferKeys[slot] = key;
            bufferAmounts[slot] = saturatedAdd(bufferAmounts[slot], amount);
            // Blanks the key, keeps the machine lock.
            stack.setAmount(0);
        }
    }

    private int bufferSlotFor(AEKey key) {
        int empty = -1;
        for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) {
            if (key.equals(bufferKeys[i])) return i;
            if (bufferKeys[i] == null && empty < 0) empty = i;
        }
        return empty;
    }

    /** Pushes every buffer slot into ME; what does not fit stays for the next flush. */
    private void flush() {
        IGrid grid = activeGrid();
        boolean left = false;
        for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) {
            if (bufferKeys[i] == null) continue;
            if (grid != null) bufferAmounts[i] -= pushToMe(grid, bufferKeys[i], bufferAmounts[i]);
            if (bufferAmounts[i] <= 0) {
                bufferKeys[i] = null;
                bufferAmounts[i] = 0;
            } else {
                left = true;
            }
        }
        if (left != blocked) {
            blocked = left;
            markClientDirty();
        }
    }

    private static long saturatedAdd(long a, long b) {
        long sum = a + b;
        return sum < 0 ? Long.MAX_VALUE : sum;
    }

    /* ---------------------------------------------- breaking / placing ---------------------------------------------- */

    /**
     * Broken: flush what ME takes now; the rest stays for {@link MeHatchBlock} to copy onto the drop.
     *
     * <p>A player's break runs this before the drops are worked out, but {@code Level.destroyBlock} works
     * them out first. If the buffer was already copied onto a drop this tick, it is not flushed as well,
     * or it would exist twice.
     */
    @Override
    public List<ItemStack> dropExtra() {
        boolean handedOut = level != null && handedOutAt == level.getGameTime();
        if (!handedOut && activeGrid() != null) flush();
        return super.dropExtra();
    }

    /**
     * Copies the buffer onto the hatch item among {@code drops}. A copy, not a move: something may ask for a
     * block's drops without breaking it, and the buffer must survive that. When the block does go, so does
     * this block entity and its buffer with it. Nothing happens when the buffer is empty, so a clean hatch
     * drops a plain, stackable item.
     */
    void copyBufferTo(List<ItemStack> drops, Item hatchItem) {
        List<GenericStack> contents = new ArrayList<>();
        for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) {
            if (bufferKeys[i] != null) contents.add(new GenericStack(bufferKeys[i], bufferAmounts[i]));
        }
        if (contents.isEmpty()) return;
        for (ItemStack drop : drops) {
            if (drop.is(hatchItem) && drop.getCount() == 1) {
                drop.set(ComponentsRegistry.ME_HATCH_BUFFER.get(), List.copyOf(contents));
                if (level != null) handedOutAt = level.getGameTime();
                return;
            }
        }
    }

    @Override
    public void onPlaced(@Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(placer, itemStack);
        List<GenericStack> stored = itemStack.get(ComponentsRegistry.ME_HATCH_BUFFER.get());
        if (stored == null) return;
        for (GenericStack stack : stored) {
            if (stack == null || stack.amount() <= 0) continue;
            int slot = bufferSlotFor(stack.what());
            if (slot < 0) continue;
            bufferKeys[slot] = stack.what();
            bufferAmounts[slot] = saturatedAdd(bufferAmounts[slot], stack.amount());
        }
        setChanged();
    }

    /* ------------------------------------------------------ MI ------------------------------------------------------ */

    @Override
    public HatchType getHatchType() {
        return HatchTypes.ITEM_OUTPUT;
    }

    @Override
    public boolean upgradesToSteel() {
        return false;
    }

    @Override
    public MIInventory getInventory() {
        return inventory;
    }

    @Override
    public void appendItemOutputs(List<ConfigurableItemStack> list) {
        list.addAll(inventory.getItemStacks());
    }

    @Override
    public List<Component> getTooltips() {
        return List.of(
                Component.translatable("tooltip.roll_mod.me_hatch.output.item", MeHatches.OUTPUT_SLOTS)
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.roll_mod.me_hatch.output.flush").withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.roll_mod.me_hatch.channel").withStyle(ChatFormatting.GRAY));
    }

    /* ------------------------------------------------ saving / screen ----------------------------------------------- */

    @Override
    protected void writeMeData(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) {
            list.add(GenericStack.writeTag(registries, getBuffer(i)));
        }
        tag.put(TAG_BUFFER, list);
    }

    @Override
    protected void readMeData(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = tag.getList(TAG_BUFFER, Tag.TAG_COMPOUND);
        for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) {
            GenericStack stack = i < list.size() ? GenericStack.readTag(registries, list.getCompound(i)) : null;
            bufferKeys[i] = stack == null || stack.amount() <= 0 ? null : stack.what();
            bufferAmounts[i] = bufferKeys[i] == null ? 0 : stack.amount();
        }
    }

    @Override
    protected void writeClientMeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(TAG_BLOCKED, blocked);
    }

    @Override
    protected void readClientMeData(CompoundTag tag, HolderLookup.Provider registries) {
        blocked = tag.getBoolean(TAG_BLOCKED);
    }

    @Override
    public ModularUI createUI(Player player) {
        return MeHatchUI.output(this, player, this::getBuffer, this::isBlocked);
    }
}
