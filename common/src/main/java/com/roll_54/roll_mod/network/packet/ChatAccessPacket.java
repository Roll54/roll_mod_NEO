package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.client.ClientChatStore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client, at login: which staff-only chat tabs this player gets. Only the DEV tab so far
 * (operators, see {@code ChatDevFeed}).
 */
public record ChatAccessPacket(boolean dev) implements CustomPacketPayload {

    public static final Type<ChatAccessPacket> TYPE = new Type<>(RollMod.id("chat_access"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChatAccessPacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ChatAccessPacket::new, ChatAccessPacket::dev).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChatAccessPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) ClientChatStore.setDevAccess(payload.dev());
        });
    }
}
