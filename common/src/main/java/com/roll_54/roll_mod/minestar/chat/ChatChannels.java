package com.roll_54.roll_mod.minestar.chat;

import com.roll_54.roll_mod.minestar.interactions.InteractionSettings;
import com.roll_54.roll_mod.network.packet.ChatChannelPacket;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The one way a channel message reaches a player: tagged with its channel, so the client can show
 * it under the right button over the chat box. Used by global, local, friends and trade chat alike.
 */
public final class ChatChannels {

    private ChatChannels() {}

    public static void deliver(ServerPlayer to, ChatChannel channel, Component message) {
        PacketDistributor.sendToPlayer(to, new ChatChannelPacket(channel, message));
    }

    /**
     * A player's message: not delivered to anyone who ignores {@code sender} (see
     * {@code InteractionSettings}). The sender always gets their own line back.
     */
    public static void deliver(ServerPlayer to, ChatChannel channel, Component message, @Nullable UUID sender) {
        if (sender != null && !sender.equals(to.getUUID()) && InteractionSettings.ignores(to.getUUID(), sender)) return;
        deliver(to, channel, message);
    }
}
