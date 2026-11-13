package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.client.MentionSound;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: someone wrote "@Name" to this player in {@code channel} — the ping sound, like a mention
 * on Discord. Sent apart from the line itself, so the history replayed on join never pings.
 */
public record MentionPingPacket(ChatChannel channel) implements CustomPacketPayload {

    public static final Type<MentionPingPacket> TYPE = new Type<>(RollMod.id("mention_ping"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MentionPingPacket> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.channel().ordinal()),
            buf -> new MentionPingPacket(
                    ChatChannel.values()[Math.floorMod(buf.readVarInt(), ChatChannel.values().length)]));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MentionPingPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) MentionSound.ping(payload.channel());
        });
    }
}
