package com.roll_54.roll_mod.items.electricItems;

import aztech.modern_industrialization.MIComponents;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.roll_54.roll_mod.items.electricItems.orescanner.OreScan;
import com.roll_54.roll_mod.items.electricItems.orescanner.gui.OreScannerUI;
import com.roll_54.roll_mod.network.packet.orescanner.OreScanResultPacket;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Electric ore scanner: a right-click spends {@code costPerScan} EU, counts roll_mod's ore materials at every height
 * in the {@code (2 * radius + 1)²} chunks around the player, and opens {@link OreScannerUI} on the result.
 */
public class OreScannerItem extends Item implements ISimpleEnergyItem {

    private static final int COOLDOWN_TICKS = 20;

    private final int radius;
    private final long energyCapacity;
    private final long costPerScan;
    private final int barColor;

    public OreScannerItem(Properties properties, int radius, long energyCapacity, long costPerScan, int barColor) {
        super(properties);
        this.radius = radius;
        this.energyCapacity = energyCapacity;
        this.costPerScan = costPerScan;
        this.barColor = barColor;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            long stored = getStoredEnergy(stack);
            if (stored < costPerScan) {
                player.displayClientMessage(
                        Component.translatable("message.roll_mod.ore_scanner.not_enough_energy",
                                        EnergyFormatUtils.formatEnergy(stored), EnergyFormatUtils.formatEnergy(costPerScan))
                                .withStyle(style -> style.withColor(TextColor.fromRgb(0xFF5555))),
                        true);
                return InteractionResultHolder.fail(stack);
            }
            tryUseEnergy(stack, costPerScan);
            // Copy the ore-bearing sections here, count them on a worker, then come back to the server thread to
            // send the result and open the screen — a 9x9 scan stays off the tick.
            OreScan.Snapshot snapshot = OreScan.snapshot(serverLevel, player.blockPosition(), radius);
            MinecraftServer server = serverLevel.getServer();
            CompletableFuture.supplyAsync(snapshot::count, Util.backgroundExecutor())
                    .thenAcceptAsync(result -> {
                        if (serverPlayer.hasDisconnected()) return;
                        // The result first, so the client has it by the time the screen is built.
                        PacketDistributor.sendToPlayer(serverPlayer, new OreScanResultPacket(result));
                        PlayerUIMenuType.openUI(serverPlayer, OreScannerUI.UI_ID);
                    }, server);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
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
        return (int) (13 * (double) getStoredEnergy(stack) / getEnergyCapacity(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.roll_mod.energy",
                        EnergyFormatUtils.formatEnergy(getStoredEnergy(stack)),
                        EnergyFormatUtils.formatEnergy(getEnergyCapacity(stack)))
                .withStyle(ChatFormatting.AQUA));
        int size = 2 * radius + 1;
        tooltip.add(Component.translatable("tooltip.roll_mod.ore_scanner.area", size, size)
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.roll_mod.ore_scanner.cost", EnergyFormatUtils.formatEnergy(costPerScan))
                .withStyle(ChatFormatting.GRAY));
    }
}
