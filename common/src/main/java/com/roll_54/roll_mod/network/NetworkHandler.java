package com.roll_54.roll_mod.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.network.packet.ClaimDailyBonusPacket;
import com.roll_54.roll_mod.network.packet.ClaimDailyTaskPacket;
import com.roll_54.roll_mod.network.packet.DailyTaskToastPacket;
import com.roll_54.roll_mod.network.packet.OpenDailyTasksPacket;
import com.roll_54.roll_mod.network.packet.PacketLaunchRocket;
import com.roll_54.roll_mod.network.packet.armor.MultiProtectingGraviChestItemPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = RollMod.MODID)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                PacketLaunchRocket.TYPE,
                PacketLaunchRocket.STREAM_CODEC,
                PacketLaunchRocket::handle
        );
        registrar.playToServer(
                MultiProtectingGraviChestItemPacket.TYPE,
                MultiProtectingGraviChestItemPacket.STREAM_CODEC,
                MultiProtectingGraviChestItemPacket::handle
        );
        registrar.playToServer(
                OpenDailyTasksPacket.TYPE,
                OpenDailyTasksPacket.STREAM_CODEC,
                OpenDailyTasksPacket::handle
        );
        registrar.playToServer(
                ClaimDailyTaskPacket.TYPE,
                ClaimDailyTaskPacket.STREAM_CODEC,
                ClaimDailyTaskPacket::handle
        );
        registrar.playToServer(
                ClaimDailyBonusPacket.TYPE,
                ClaimDailyBonusPacket.STREAM_CODEC,
                ClaimDailyBonusPacket::handle
        );
        registrar.playToClient(
                DailyTaskToastPacket.TYPE,
                DailyTaskToastPacket.STREAM_CODEC,
                DailyTaskToastPacket::handle
        );
    }
}
