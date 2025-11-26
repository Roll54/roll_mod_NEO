package com.roll_54.roll_mod.modItems;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import com.roll_54.roll_mod.data.ToggleableItem;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class EnergyBatteryItem extends Item implements ISimpleEnergyItem, ToggleableItem {

    private final long energyCapacity;
    private final long maxInput;
    private final long maxOutput;
    private final int barColor;

    public EnergyBatteryItem(Properties properties,
                             long energyCapacity,
                             long maxInput,
                             long maxOutput,
                             int barColor) {
        super(properties);
        this.energyCapacity = energyCapacity;
        this.maxInput = maxInput;
        this.maxOutput = maxOutput;
        this.barColor = barColor;
    }

    // === ISimpleEnergyItem ===
    @Override
    public net.minecraft.core.component.DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return energyCapacity;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return maxInput;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return maxOutput;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double frac = (double) getStoredEnergy(stack) / getEnergyCapacity(stack);
        return (int) (13 * frac);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor;
    }

    // === Перемикач ON/OFF через ПКМ ===
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean active = isActivated(stack);

        if (!level.isClientSide) {
            boolean newState = !active;
            setActivated(player, stack, newState);

            // ⚡️ Оновити дані на сервері й клієнті
            player.getInventory().setChanged();

            Component stateText = Component.translatable(
                    newState ? "tooltip.roll_mod.on" : "tooltip.roll_mod.off"
            ).withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED);

            player.displayClientMessage(
                    Component.translatable("tooltip.roll_mod.battery_toggled", stateText),
                    true
            );
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // === Основна логіка живлення предметів ===
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        if (!isActivated(stack)) return;
        long stored = getStoredEnergy(stack);
        if (stored <= 0) return;

        // 1) Броня
        for (ItemStack armor : player.getInventory().armor) {
            tryChargeItem(stack, armor, "armor");
            if (getStoredEnergy(stack) <= 0) return;
        }

        // 2) Офхенд
        for (ItemStack off : player.getInventory().offhand) {
            tryChargeItem(stack, off, "offhand");
            if (getStoredEnergy(stack) <= 0) return;
        }

        // 3) Предмети в рюкзаку
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack other = player.getInventory().items.get(i);
            if (other == stack) continue;
            if (other.isEmpty()) continue;

            tryChargeItem(stack, other, "slot " + i);
            if (getStoredEnergy(stack) <= 0) return;
        }
    }

    private void tryChargeItem(ItemStack battery, ItemStack targetStack, String debugName) {
        if (targetStack.isEmpty()) return;
        long stored = getStoredEnergy(battery);
        if (stored <= 0) return;

        // Capability GrandPower
        ILongEnergyStorage target = null;
        try { target = targetStack.getCapability(ILongEnergyStorage.ITEM); } catch (Exception ignored) {}

        long received = 0;

        if (target != null) {
            long toSend = Math.min(getEnergyMaxOutput(battery), stored);
            received = target.receive(toSend, false);

            if (received > 0) {
                setStoredEnergy(battery, stored - received);
                return;
            }
        }

        // Modern Industrialization / EI component
        var energyComp = aztech.modern_industrialization.MIComponents.ENERGY.get();

        if (targetStack.has(energyComp)) {
            long cur = targetStack.getOrDefault(energyComp, 0L);
            long cap = 1_000_000_000L;

            if (targetStack.getItem() instanceof ISimpleEnergyItem simple)
                cap = simple.getEnergyCapacity(targetStack);

            long toSend = Math.min(getEnergyMaxOutput(battery), stored);
            long space = cap - cur;

            if (space > 0) {
                long insert = Math.min(space, toSend);
                targetStack.set(energyComp, cur + insert);
                setStoredEnergy(battery, stored - insert);
            }
        }
    }

    // === Tooltip ===
    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        long cap = getEnergyCapacity(stack);
        boolean active = isActivated(stack);

        String formattedStored = EnergyFormatUtils.formatEnergy(stored);
        String formattedCap = EnergyFormatUtils.formatEnergy(cap);

        tooltip.add(Component.translatable("tooltip.roll_mod.energy", formattedStored, formattedCap)
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("tooltip.roll_mod.mode",
                        Component.translatable(active ? "tooltip.roll_mod.on" : "tooltip.roll_mod.off"))
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));

        tooltip.add(Component.translatable("tooltip.roll_mod.toggle_hint")
                .withStyle(ChatFormatting.GRAY));

    }
}
