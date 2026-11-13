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
 * A player's line also names its writer, so the client can reply to it.
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
        String name = "";
        if (sender != null) {
            ServerPlayer writer = to.server.getPlayerList().getPlayer(sender);
            if (writer != null) name = writer.getGameProfile().getName();
        }
        deliver(to, channel, message, sender, name);
    }

    /** As above, with the writer's name given (they may have left since, as in {@link ChatHistory}). */
    public static void deliver(ServerPlayer to, ChatChannel channel, Component message, @Nullable UUID sender,
                               String senderName) {
        deliver(to, channel, message, sender, senderName, 0L);
    }

    /** As above, sent at {@code time} (epoch millis; 0 for now), which the chat shows on hover. */
    public static void deliver(ServerPlayer to, ChatChannel channel, Component message, @Nullable UUID sender,
                               String senderName, long time) {
        if (sender != null && !sender.equals(to.getUUID()) && InteractionSettings.ignores(to.getUUID(), sender)) return;
        PacketDistributor.sendToPlayer(to, new ChatChannelPacket(channel, message, "", senderName, time));
    }

    /** A private message line, naming {@code partner} — the other side — so the PM tab can reply to them. */
    public static void deliverPm(ServerPlayer to, Component message, String partner) {
        PacketDistributor.sendToPlayer(to, new ChatChannelPacket(ChatChannel.PM, message, partner, partner, 0L));
    }
}
