package com.roll_54.roll_mod.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.network.packet.PacketLaunchRocket;
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
    }
}
