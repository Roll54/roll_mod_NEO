package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.interactions.InteractionSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** One switch of "configure interaction" flipped — always about the sender's own settings. */
public record InteractionSettingPacket(UUID target, InteractionSettings.Flag flag, boolean on)
        implements CustomPacketPayload {

    public static final Type<InteractionSettingPacket> TYPE = new Type<>(RollMod.id("interaction_setting"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InteractionSettingPacket> STREAM_CODEC =
            StreamCodec.of(InteractionSettingPacket::encode, InteractionSettingPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, InteractionSettingPacket payload) {
        buf.writeUUID(payload.target());
        buf.writeVarInt(payload.flag().ordinal());
        buf.writeBoolean(payload.on());
    }

    private static InteractionSettingPacket decode(RegistryFriendlyByteBuf buf) {
        UUID target = buf.readUUID();
        // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
        InteractionSettings.Flag flag = InteractionSettings.Flag.values()[
                Math.floorMod(buf.readVarInt(), InteractionSettings.Flag.values().length)];
        return new InteractionSettingPacket(target, flag, buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(InteractionSettingPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                InteractionSettings.set(player, payload.target(), payload.flag(), payload.on());
                // Ignoring someone changes which of their friend requests this player is shown.
                if (payload.flag() == InteractionSettings.Flag.IGNORE) {
                    com.roll_54.roll_mod.minestar.friends.FriendsService.syncTo(player);
                }
            }
        });
    }
}
