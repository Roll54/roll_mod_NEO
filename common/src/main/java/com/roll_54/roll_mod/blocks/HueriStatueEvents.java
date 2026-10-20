package com.roll_54.roll_mod.blocks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.RMMAttachment;
import com.roll_54.roll_mod.network.packet.HueriStatueSeenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Tells each client on login whether its player has read a Hueri statue yet, and resets that. */
@EventBusSubscriber(modid = RollMod.MODID)
public final class HueriStatueEvents {

    private HueriStatueEvents() {}

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new HueriStatueSeenPacket(player.getData(RMMAttachment.HUERI_STATUE_SEEN)));
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HueriStatuePlayers.forget(event.getEntity().getUUID());
    }

    /** As if {@code player} had never opened a statue; their client shows the indicators again at once. */
    public static void reset(ServerPlayer player) {
        player.setData(RMMAttachment.HUERI_STATUE_SEEN, false);
        PacketDistributor.sendToPlayer(player, new HueriStatueSeenPacket(false));
    }
}
