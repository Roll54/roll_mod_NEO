package com.roll_54.roll_mod.gtmogs.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.roll_54.roll_mod.gtmogs.api.GTMOGSAPI;
import com.roll_54.roll_mod.gtmogs.common.network.packets.SCPacketShareProspection;
import com.roll_54.roll_mod.gtmogs.common.network.packets.SPacketSendWorldID;
import com.roll_54.roll_mod.gtmogs.common.network.packets.prospecting.SPacketProspectOre;

public class GTNetwork {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(GTMOGSAPI.NETWORK_VERSION);
        // spotless:off
        registrar.playToClient(SPacketProspectOre.TYPE, SPacketProspectOre.CODEC, SPacketProspectOre::execute);

        registrar.playToClient(SPacketSendWorldID.TYPE, SPacketSendWorldID.CODEC, SPacketSendWorldID::execute);
        registrar.playBidirectional(SCPacketShareProspection.TYPE, SCPacketShareProspection.CODEC, SCPacketShareProspection::execute);
        // spotless:on
    }
}
