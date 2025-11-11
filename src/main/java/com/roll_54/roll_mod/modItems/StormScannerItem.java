package com.roll_54.roll_mod.modItems;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.netherstorm.StormHandler;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import com.roll_54.roll_mod.util.TimeFormatUtil;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.roll_54.roll_mod.netherstorm.StormHandler.isStormActive;

public class StormScannerItem extends Item implements ISimpleEnergyItem {

    private final int tier;
    private final int barColor;
    private final long energyCapacity;


    public StormScannerItem(Properties properties, int tier, int barColor, long energyCapacity) {
        super(properties);
        this.tier = tier;
        this.barColor = barColor;
        this.energyCapacity = energyCapacity;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            long requiredEnergy = (tier == 1) ? 100_000L : 1_000_000L;
            long stored = getStoredEnergy(stack);

            // ⚡️ Перевірка, чи є достатньо енергії
            if (stored < requiredEnergy) {
                player.displayClientMessage(
                        Component.translatable("message.roll_mod.storm_scanner.not_enough_energy",
                                        EnergyFormatUtils.formatEnergy(stored), EnergyFormatUtils.formatEnergy(requiredEnergy))
                                .withStyle(style -> style.withColor(TextColor.fromRgb(0xFF5555))), // червоний
                        true
                );
                return InteractionResultHolder.fail(stack);
            }

            if (tier == 1) {
                // 🌩️ Примітивний сканер — лише стан шторму
                if (isStormActive()) {
                    player.displayClientMessage(
                            Component.translatable("message.roll_mod.storm_scanner.storm_active")
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0xb00538))),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.roll_mod.storm_scanner.storm_not_active")
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x009921))),
                            true
                    );
                }
                tryUseEnergy(stack, requiredEnergy);
            } else {
                // ⚡ Просунутий сканер — детальна інформація
                int ticks = StormHandler.getStormTicks();
                int duration = StormHandler.getStormDuration();
                int untilNext = StormHandler.getTicksUntilNextStorm();

                boolean active = isStormActive();

                // 1️⃣ Стан шторму
                player.displayClientMessage(
                        Component.translatable(active
                                ? "message.roll_mod.storm_scanner.status_active"
                                : "message.roll_mod.storm_scanner.status_inactive"
                        ).withStyle(style -> style.withColor(TextColor.fromRgb(active ? 0xb00538 : 0x009921))),
                        false
                );

                // 2️⃣ До кінця або початку
                if (active) {
                    player.displayClientMessage(
                            Component.translatable("message.roll_mod.storm_scanner.time_until_end",
                                            TimeFormatUtil.formatTime(duration - ticks))
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x0099ff))),
                            false
                    );
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.roll_mod.storm_scanner.time_until_start",
                                            TimeFormatUtil.formatTime(untilNext))
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x0099ff))),
                            false
                    );
                }

                // 3️⃣ Повідомлення у верхній частині екрана
                player.displayClientMessage(
                        Component.translatable("message.roll_mod.storm_scanner.detailed_info")
                                .withStyle(style -> style.withColor(TextColor.fromRgb(0x00bfff))),
                        true
                );
                tryUseEnergy(stack, requiredEnergy);
            }
        }

        player.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return energyCapacity;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return energyCapacity;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return energyCapacity;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor;
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
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        long cap = getEnergyCapacity(stack);

        String formattedStored = EnergyFormatUtils.formatEnergy(stored);
        String formattedCap = EnergyFormatUtils.formatEnergy(cap);

        tooltip.add(Component.translatable("tooltip.roll_mod.energy", formattedStored, formattedCap)
                .withStyle(ChatFormatting.AQUA));
        if(tier == 1) {
            tooltip.add(Component.translatable("tooltip.roll_mod.storm_hint.1")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.roll_mod.storm_hint.1")
                    .withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable("tooltip.roll_mod.hv_storm_hint.2")
                    .withStyle(ChatFormatting.GREEN));
        }
    }

}
