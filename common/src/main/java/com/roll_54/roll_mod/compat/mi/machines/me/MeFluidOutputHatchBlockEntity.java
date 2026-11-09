package com.roll_54.roll_mod.compat.mi.machines.me;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import aztech.modern_industrialization.inventory.ConfigurableFluidStack;
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
 * An ME fluid output hatch: {@link MeHatches#OUTPUT_SLOTS} tanks of unlimited size
 * ({@link MeHatches#FLUID_OUTPUT_CAPACITY}), pushed into ME every {@link MeHatches#FLUSH_INTERVAL} ticks.
 *
 * <p>Unlike item slots, MI fluid tanks may be any size, so MI fills these directly and there is no separate
 * buffer. They keep filling while ME is full or offline and flush once it takes fluid again; a fourth fluid
 * waits while three others fill the tanks. Fluid still inside a broken hatch is lost, as with MI's own fluid
 * hatches; while connected, breaking first flushes it.
 */
public class MeFluidOutputHatchBlockEntity extends MeHatchBlockEntity {

    private static final String TAG_BLOCKED = "me_blocked";

    private final MIInventory inventory;
    /** The last flush left something behind: ME is full or offline. Shown on the screen. */
    private boolean blocked;

    public MeFluidOutputHatchBlockEntity(BEP bep, ResourceLocation id) {
        super(bep, id);
        List<ConfigurableFluidStack> tanks = new ArrayList<>(MeHatches.OUTPUT_SLOTS);
        for (int i = 0; i < MeHatches.OUTPUT_SLOTS; i++) {
            tanks.add(ConfigurableFluidStack.standardOutputSlot(MeHatches.FLUID_OUTPUT_CAPACITY));
        }
        // Slot positions only matter to MI's own screen, which these hatches do not use.
        this.inventory = new MIInventory(Collections.emptyList(), tanks, SlotPositions.empty(),
                new SlotPositions.Builder().addSlots(62, 40, MeHatches.OUTPUT_SLOTS, 1).build());
        registerComponents(inventory);
    }

    /** Tank {@code i}, for the screen; {@code null} when empty. */
    public @Nullable GenericStack getTank(int slot) {
        ConfigurableFluidStack tank = inventory.getFluidStacks().get(slot);
        if (tank.getAmount() <= 0) return null;
        AEKey key = MeKeys.of(tank.getResource());
        return key == null ? null : new GenericStack(key, tank.getAmount());
    }

    public boolean isBlocked() {
        return blocked;
    }

    @Override
    protected void tickMe(long gameTime) {
        if (every(gameTime, MeHatches.FLUSH_INTERVAL)) flush();
    }

    private void flush() {
        IGrid grid = activeGrid();
        boolean left = false;
        // Always read the live list: MI swaps the stack objects when it loads NBT.
        for (ConfigurableFluidStack tank : inventory.getFluidStacks()) {
            long amount = tank.getAmount();
            if (amount <= 0) continue;
            AEKey key = MeKeys.of(tank.getResource());
            long pushed = grid == null || key == null ? 0 : pushToMe(grid, key, amount);
            if (pushed > 0) tank.decrement(pushed);
            if (tank.getAmount() > 0) left = true;
        }
        if (left != blocked) {
            blocked = left;
            markClientDirty();
        }
    }

    /** Broken: what ME takes now is saved; the rest is lost with the hatch, as in MI's fluid hatches. */
    @Override
    public List<ItemStack> dropExtra() {
        if (activeGrid() != null) flush();
        return super.dropExtra();
    }

    @Override
    public HatchType getHatchType() {
        return HatchTypes.FLUID_OUTPUT;
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
    public void appendFluidOutputs(List<ConfigurableFluidStack> list) {
        list.addAll(inventory.getFluidStacks());
    }

    @Override
    public List<Component> getTooltips() {
        return List.of(
                Component.translatable("tooltip.roll_mod.me_hatch.output.fluid", MeHatches.OUTPUT_SLOTS)
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.roll_mod.me_hatch.output.flush").withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.roll_mod.me_hatch.channel").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void writeMeData(CompoundTag tag, HolderLookup.Provider registries) {}

    @Override
    protected void readMeData(CompoundTag tag, HolderLookup.Provider registries) {}

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
        return MeHatchUI.output(this, player, this::getTank, this::isBlocked);
    }
}
