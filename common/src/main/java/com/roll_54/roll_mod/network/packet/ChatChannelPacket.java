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
 * Server → client: one chat line of a channel — global, local, friends or trade — instead of an untagged
 * system message, so the client knows which channel button it belongs under.
 */
public record ChatChannelPacket(ChatChannel channel, Component message) implements CustomPacketPayload {

    public static final Type<ChatChannelPacket> TYPE = new Type<>(RollMod.id("chat_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChatChannelPacket> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.channel().ordinal());
                ComponentSerialization.STREAM_CODEC.encode(buf, payload.message());
            },
            buf -> {
                // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
                ChatChannel channel = ChatChannel.values()[Math.floorMod(buf.readVarInt(), ChatChannel.values().length)];
                return new ChatChannelPacket(channel, ComponentSerialization.STREAM_CODEC.decode(buf));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChatChannelPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Guarded so the client-only store is never class-loaded on a dedicated server.
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientChatStore.receive(payload.channel(), payload.message());
            }
        });
    }
}
