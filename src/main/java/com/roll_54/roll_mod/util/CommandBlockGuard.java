package com.roll_54.roll_mod.util;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;


/*
    Смішна хуйня, Сашадідух чорт який придумав що йому треба опка на серваку на якому я адмін, ну тепер тільки я можу мати таку превілегію, але з командою що робити... Оу чорт.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class CommandBlockGuard {
    private CommandBlockGuard() {}

    // Хто має доступ
    private static boolean isAllowed(Player p) {
        return p != null && "roll_54".equalsIgnoreCase(p.getGameProfile().getName());
    }

    private static boolean isCommandBlock(Block b) {
        return b instanceof CommandBlock;
    }

    private static void deny(Player p) {
        if (p == null) return;

        if (p.isCreative()) {
            p.displayClientMessage(
                    Component.translatable("message.roll_mod.cb.no_access.creative")
                            .withStyle(s -> s.withColor(TextColor.fromRgb(0x980f1f))),
                    true
            );
        } else if (p.isSpectator()) {
            p.displayClientMessage(
                    Component.translatable("message.roll_mod.cb.no_access.other")
                            .withStyle(s -> s.withColor(TextColor.fromRgb(0x011eaa))),
                    true
            );
        } else { // Survival або Adventure
            p.displayClientMessage(
                    Component.translatable("message.roll_mod.cb.no_access.survival")
                            .withStyle(s -> s.withColor(TextColor.fromRgb(0xff6926))),
                    true
            );
        }
    }

    // Заборона відкривати/клікати по командних блоках
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        Player p = e.getEntity();
        if (p == null || isAllowed(p)) return;

        BlockPos pos = e.getPos();
        BlockState state = e.getLevel().getBlockState(pos);
        if (isCommandBlock(state.getBlock())) {
            e.setCanceled(true);
            deny(p);
        }
    }

    // Заборона взаємодії з вагонеткою-командблоком
    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract e) {
        Player p = e.getEntity();
        if (p == null || isAllowed(p)) return;

        Entity target = e.getTarget();
        if (target instanceof MinecartCommandBlock) {
            e.setCanceled(true);
            deny(p);
        }
    }

    // Заборона ставити командні блоки
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (isAllowed(p)) return;

        if (isCommandBlock(e.getPlacedBlock().getBlock())) {
            e.setCanceled(true);
            deny(p);
        }
    }

    // Заборона ламати командні блоки
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        Player p = e.getPlayer();
        if (p == null || isAllowed(p)) return;

        if (isCommandBlock(e.getState().getBlock())) {
            e.setCanceled(true);
            deny(p);
        }
    }
}