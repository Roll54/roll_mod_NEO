package com.roll_54.roll_mod.net;

import com.roll_54.roll_mod.Roll_mod;
import com.roll_54.roll_mod.machine.LargeChemicalReactorBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@EventBusSubscriber(modid = Roll_mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetRegistration {
    private NetRegistration() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Roll_mod.MODID);
        registrar.playToServer(
                C2SSwitchModePayload.TYPE,
                C2SSwitchModePayload.STREAM_CODEC,
                NetRegistration::handleSwitchMode
        );
    }

    private static void handleSwitchMode(final C2SSwitchModePayload msg, final IPayloadContext ctx) {
        ctx.workHandler().submitAsync(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player().orElse(null);
            if (player == null) return;

            var be = player.serverLevel().getBlockEntity(msg.pos());
            if (be instanceof LargeChemicalReactorBlockEntity lcr) {
                lcr.cycleMode();
            }
        });
    }
}