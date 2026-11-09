package com.roll_54.roll_mod.compat.mi.machines.me;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import aztech.modern_industrialization.inventory.AbstractConfigurableStack;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
import aztech.modern_industrialization.inventory.ConfigurableItemStack;
import aztech.modern_industrialization.inventory.MIInventory;
import aztech.modern_industrialization.inventory.SlotPositions;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.multiblocks.HatchType;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ME input hatch: {@link MeHatches#INPUT_SLOTS} slots, each kept stocked from ME like a slot of an ME
 * interface. Every {@link MeHatches#INPUT_INTERVAL} ticks, for each slot, the same plan as AE2's
 * {@code InterfaceLogic.updatePlan}:
 * <ul>
 *   <li>stock that is not what the slot is set to (or the slot is unset) goes back to ME;</li>
 *   <li>stock above the set amount: the extra goes back;</li>
 *   <li>stock below it: the difference is requested.</li>
 * </ul>
 * The multiblock consumes from the stock like from any MI input hatch. Only ME fills the stock: its MI slots
 * take nothing from pipes or players.
 *
 * <p>The config is AE2's own {@link ConfigInventory} with overstacking on, so an item slot clamps to
 * {@link MeHatches#ITEM_INPUT_CAPACITY} items whatever the item's stack size, and a fluid slot to
 * {@link MeHatches#FLUID_INPUT_CAPACITY} mB. A machine needing more puts the same item in several slots.
 */
public class MeInputHatchBlockEntity extends MeHatchBlockEntity {

    private static final String TAG_CONFIG = "me_config";

    private final boolean fluid;
    private final MIInventory inventory;
    private final ConfigInventory config;

    public MeInputHatchBlockEntity(BEP bep, ResourceLocation id, boolean fluid) {
        super(bep, id);
        this.fluid = fluid;
        // Slot positions only matter to MI's own screen, which these hatches do not use.
        SlotPositions positions = new SlotPositions.Builder().addSlots(8, 18, 8, 2).build();
        if (fluid) {
            List<ConfigurableFluidStack> tanks = new ArrayList<>(MeHatches.INPUT_SLOTS);
            for (int i = 0; i < MeHatches.INPUT_SLOTS; i++) {
                // No insert or extract flags: pipes and players cannot touch the stock.
                tanks.add(new ConfigurableFluidStack(MeHatches.FLUID_INPUT_CAPACITY));
            }
            this.inventory = new MIInventory(Collections.emptyList(), tanks, SlotPositions.empty(), positions);
        } else {
            List<ConfigurableItemStack> slots = new ArrayList<>(MeHatches.INPUT_SLOTS);
            for (int i = 0; i < MeHatches.INPUT_SLOTS; i++) {
                slots.add(new ConfigurableItemStack());
            }
            this.inventory = new MIInventory(slots, Collections.emptyList(), positions, SlotPositions.empty());
        }
        this.config = ConfigInventory.configStacks(MeHatches.INPUT_SLOTS)
                .supportedType(fluid ? AEKeyType.fluids() : AEKeyType.items())
                .changeListener(this::onConfigChanged)
                // Without it AE2 clamps an item to its stack size, ignoring the capacity below.
                .allowOverstacking(true)
                .build();
        if (fluid) {
            config.setCapacity(AEKeyType.fluids(), MeHatches.FLUID_INPUT_CAPACITY);
        } else {
            config.setCapacity(AEKeyType.items(), MeHatches.ITEM_INPUT_CAPACITY);
        }
        registerComponents(inventory);
    }

    public boolean isFluid() {
        return fluid;
    }

    /** What each slot keeps stocked; set through {@link MeHatchConfigPacket}. */
    public ConfigInventory getConfig() {
        return config;
    }

    /** The stock of slot {@code i}, for the screen; {@code null} when empty. */
    public @Nullable GenericStack getStock(int slot) {
        AbstractConfigurableStack<?, ?> stack = stack(slot);
        if (stack.getAmount() <= 0) return null;
        AEKey key = MeKeys.of(stack.getResource());
        return key == null ? null : new GenericStack(key, stack.getAmount());
    }

    private void onConfigChanged() {
        setChanged();
        markClientDirty();
    }

    // Always read the live list: MI swaps the stack objects when it loads NBT.
    private AbstractConfigurableStack<?, ?> stack(int slot) {
        return fluid ? inventory.getFluidStacks().get(slot) : inventory.getItemStacks().get(slot);
    }

    /* ------------------------------------------------------ ME ------------------------------------------------------ */

    @Override
    protected void tickMe(long gameTime) {
        if (!every(gameTime, MeHatches.INPUT_INTERVAL)) return;
        IGrid grid = activeGrid();
        if (grid == null) return;
        for (int i = 0; i < MeHatches.INPUT_SLOTS; i++) {
            restock(grid, i);
        }
    }

    private void restock(IGrid grid, int slot) {
        AbstractConfigurableStack<?, ?> stack = stack(slot);
        GenericStack want = config.getStack(slot);
        AEKey wantKey = want == null ? null : want.what();
        long have = stack.getAmount();
        AEKey haveKey = have > 0 ? MeKeys.of(stack.getResource()) : null;

        if (haveKey != null && !haveKey.equals(wantKey)) {
            // Not what the slot is set to: back to ME, all of it. Restocking waits until the slot is empty.
            remove(stack, pushToMe(grid, haveKey, have));
            return;
        }
        if (wantKey == null) return;
        long target = Math.min(want.amount(), config.getMaxAmount(wantKey));
        if (have > target) {
            remove(stack, pushToMe(grid, wantKey, have - target));
        } else if (have < target) {
            long got = pullFromMe(grid, wantKey, target - have);
            if (got <= 0) return;
            if (have == 0) setKey(slot, wantKey);
            stack.increment(got);
        }
    }

    /** Every change notifies MI's listeners and so restarts the controller's recipe search: skip empty ones. */
    private static void remove(AbstractConfigurableStack<?, ?> stack, long amount) {
        if (amount > 0) stack.decrement(amount);
    }

    private void setKey(int slot, AEKey key) {
        if (key instanceof AEFluidKey fluidKey) {
            inventory.getFluidStacks().get(slot).setKey(MeKeys.fluid(fluidKey));
        } else if (key instanceof AEItemKey itemKey) {
            inventory.getItemStacks().get(slot).setKey(MeKeys.item(itemKey));
        }
    }

    /**
     * Broken: the stock goes back to ME first. What ME would not take is dropped here, split into stacks:
     * MI would drop each slot as one {@code ItemStack} of its whole amount, which above a stack is an item
     * entity that cannot be saved. MI calls this before dropping the slots, so they are empty by then.
     */
    @Override
    public List<ItemStack> dropExtra() {
        IGrid grid = activeGrid();
        if (grid != null) {
            for (int i = 0; i < MeHatches.INPUT_SLOTS; i++) {
                AbstractConfigurableStack<?, ?> stack = stack(i);
                AEKey key = stack.getAmount() > 0 ? MeKeys.of(stack.getResource()) : null;
                if (key != null) remove(stack, pushToMe(grid, key, stack.getAmount()));
            }
        }
        List<ItemStack> drops = new ArrayList<>(super.dropExtra());
        for (ConfigurableItemStack stack : inventory.getItemStacks()) {
            long left = stack.getAmount();
            if (left <= 0) continue;
            int max = Math.max(1, stack.getResource().getMaxStackSize());
            while (left > 0) {
                int count = (int) Math.min(left, max);
                drops.add(stack.getResource().toStack(count));
                left -= count;
            }
            stack.setAmount(0);
        }
        return drops;
    }

    /* ------------------------------------------------------ MI ------------------------------------------------------ */

    @Override
    public HatchType getHatchType() {
        return fluid ? HatchTypes.FLUID_INPUT : HatchTypes.ITEM_INPUT;
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
    public void appendItemInputs(List<ConfigurableItemStack> list) {
        list.addAll(inventory.getItemStacks());
    }

    @Override
    public void appendFluidInputs(List<ConfigurableFluidStack> list) {
        list.addAll(inventory.getFluidStacks());
    }

    @Override
    public List<Component> getTooltips() {
        return List.of(
                Component.translatable(fluid ? "tooltip.roll_mod.me_hatch.input.fluid" : "tooltip.roll_mod.me_hatch.input.item",
                        MeHatches.INPUT_SLOTS).withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.roll_mod.me_hatch.channel").withStyle(ChatFormatting.GRAY));
    }

    /* ------------------------------------------------ saving / screen ----------------------------------------------- */

    @Override
    protected void writeMeData(CompoundTag tag, HolderLookup.Provider registries) {
        config.writeToChildTag(tag, TAG_CONFIG, registries);
    }

    @Override
    protected void readMeData(CompoundTag tag, HolderLookup.Provider registries) {
        config.readFromChildTag(tag, TAG_CONFIG, registries);
    }

    @Override
    protected void writeClientMeData(CompoundTag tag, HolderLookup.Provider registries) {
        config.writeToChildTag(tag, TAG_CONFIG, registries);
    }

    @Override
    protected void readClientMeData(CompoundTag tag, HolderLookup.Provider registries) {
        config.readFromChildTag(tag, TAG_CONFIG, registries);
    }

    @Override
    public ModularUI createUI(Player player) {
        return MeHatchUI.input(this, player);
    }
}
