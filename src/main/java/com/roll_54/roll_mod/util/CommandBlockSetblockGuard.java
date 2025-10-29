//package com.roll_54.roll_mod.Util;
//
//import com.roll_54.roll_mod.RollMod;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.network.chat.Component;
//import net.minecraft.ChatFormatting;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.event.CommandEvent;
//
//@EventBusSubscriber(modid = RollMod.MODID)
//public class CommandBlockSetblockGuard {
//
//    @SubscribeEvent
//    public static void onCommand(CommandEvent event) {
//        // Джерело команди
//        if (!(event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player)) {
//            return; // консоль або командні блоки не чіпаємо
//        }
//
//        String raw = event.getParseResults().getReader().getString().toLowerCase();
//
//        // Якщо це setblock і намагається поставити *_command_block
//        if (raw.startsWith("/setblock") && raw.contains("minecraft:command_block")) {
//            String name = player.getGameProfile().getName();
//
//            if (!"roll_54".equalsIgnoreCase(name)) {
//                // ❌ Заборонено
//                player.displayClientMessage(
//                        Component.translatable("message.roll_mod.cb.no_access.setblock")
//                                .withStyle(ChatFormatting.RED),
//                        false
//                );
//                event.setCanceled(true);
//            }
//        }
//    }
//}