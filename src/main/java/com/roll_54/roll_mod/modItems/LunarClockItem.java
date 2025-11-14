package com.roll_54.roll_mod.modItems;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class LunarClockItem extends Item implements ISimpleEnergyItem {
    private static final long ENERGY_CAPACITY = 5_000_000L; // 5M EU запас
    private static final long ENERGY_COST = 1_000_000L;     // 1M EU за використання

    public LunarClockItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return ENERGY_CAPACITY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return ENERGY_CAPACITY;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }
    @Override
    public int getBarColor(ItemStack stack) {
        return 0x30e996;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double frac = (double) getStoredEnergy(stack) / getEnergyCapacity(stack);
        return (int) (13 * frac);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        long stored = getStoredEnergy(stack);

        if (stored < ENERGY_COST) {
            player.displayClientMessage(
                    Component.translatable("tooltip.roll_mod.lunar_clock.no_energy").withStyle(ChatFormatting.RED),
                    true
            );
            return InteractionResultHolder.fail(stack);
        }

        // Використати енергію
        tryUseEnergy(stack, ENERGY_COST);

        // Отримати фазу Місяця (0-7)
        if (level instanceof ServerLevel serverLevel) {
            int moonPhase = serverLevel.getMoonPhase();
            player.displayClientMessage(
                    Component.translatable("tooltip.roll_mod.lunar_clock.phase." + moonPhase)
                            .withStyle(ChatFormatting.AQUA),
                    false
            );
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        }

        return InteractionResultHolder.success(stack);
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

        tooltip.add(Component.translatable("tooltip.roll_mod.lunar_clock.desc")
                .withStyle(ChatFormatting.GRAY));

    }
}