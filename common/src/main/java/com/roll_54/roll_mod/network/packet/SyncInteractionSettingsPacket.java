package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.interactions.ClientInteractionCache;
import com.roll_54.roll_mod.minestar.interactions.InteractionSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Everything the player has set in "configure interaction", by target. */
public record SyncInteractionSettingsPacket(Map<UUID, InteractionSettings.Flags> flags) implements CustomPacketPayload {

    /** A cap, so a crafted or bloated file cannot make an unbounded payload. */
    private static final int MAX = 1024;

    public static final Type<SyncInteractionSettingsPacket> TYPE = new Type<>(RollMod.id("sync_interaction_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInteractionSettingsPacket> STREAM_CODEC =
            StreamCodec.of(SyncInteractionSettingsPacket::encode, SyncInteractionSettingsPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncInteractionSettingsPacket payload) {
        int count = Math.min(MAX, payload.flags().size());
        buf.writeVarInt(count);
        int written = 0;
        for (Map.Entry<UUID, InteractionSettings.Flags> entry : payload.flags().entrySet()) {
            if (written++ >= count) break;
            buf.writeUUID(entry.getKey());
            entry.getValue().encode(buf);
        }
    }

    private static SyncInteractionSettingsPacket decode(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX, buf.readVarInt());
        Map<UUID, InteractionSettings.Flags> flags = new HashMap<>();
        for (int i = 0; i < count; i++) {
            flags.put(buf.readUUID(), InteractionSettings.Flags.decode(buf));
        }
        return new SyncInteractionSettingsPacket(Map.copyOf(flags));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncInteractionSettingsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientInteractionCache.FLAGS = payload.flags();
            ClientInteractionCache.VERSION++;
        });
    }
}
