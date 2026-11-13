package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.client.ClientChatStore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: one chat line of a channel instead of an untagged system message, so the client
 * knows which channel button it belongs under.
 *
 * @param partner on a {@link ChatChannel#PM} line, the name of the other side of the conversation,
 *                which the PM tab replies to; empty everywhere else
 * @param sender  the name of the player who wrote the line, which a reply mentions; empty for the
 *                server's own lines
 * @param time    when it was sent (epoch millis), shown on hover; 0 for now. The join history sends the real time
 */
public record ChatChannelPacket(ChatChannel channel, Component message, String partner, String sender, long time)
        implements CustomPacketPayload {

    public static final Type<ChatChannelPacket> TYPE = new Type<>(RollMod.id("chat_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChatChannelPacket> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.channel().ordinal());
                ComponentSerialization.STREAM_CODEC.encode(buf, payload.message());
                buf.writeUtf(payload.partner(), 64);
                buf.writeUtf(payload.sender(), 64);
                buf.writeVarLong(payload.time());
            },
            buf -> {
                // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
                ChatChannel channel = ChatChannel.values()[Math.floorMod(buf.readVarInt(), ChatChannel.values().length)];
                Component message = ComponentSerialization.STREAM_CODEC.decode(buf);
                return new ChatChannelPacket(channel, message, buf.readUtf(64), buf.readUtf(64), buf.readVarLong());
            });

    public ChatChannelPacket(ChatChannel channel, Component message) {
        this(channel, message, "", "", 0L);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChatChannelPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Guarded so the client-only store is never class-loaded on a dedicated server.
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientChatStore.receive(payload.channel(), payload.message(), payload.partner(), payload.sender(), payload.time());
            }
        });
    }
}
