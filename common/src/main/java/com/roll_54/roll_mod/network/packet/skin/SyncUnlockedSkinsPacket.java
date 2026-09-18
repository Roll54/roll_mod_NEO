package com.roll_54.roll_mod.network.packet.skin;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.cosmetics.client.ClientItemSkinCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Server → the owning client only: which skins this player has unlocked.
 *
 * <p>Kept in its own packet rather than folded into {@link SyncActiveSkinsPacket} so that a client
 * physically cannot read anybody else's unlock list — the same reasoning behind the homes feature
 * sending a projection instead of the stored record.
 */
public record SyncUnlockedSkinsPacket(Set<ResourceLocation> unlocked) implements CustomPacketPayload {

    public static final Type<SyncUnlockedSkinsPacket> TYPE = new Type<>(RollMod.id("sync_unlocked_skins"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncUnlockedSkinsPacket> STREAM_CODEC =
            StreamCodec.of(SyncUnlockedSkinsPacket::encode, SyncUnlockedSkinsPacket::decode);

    private static final int MAX_ENTRIES = 512;

    private static void encode(RegistryFriendlyByteBuf buf, SyncUnlockedSkinsPacket packet) {
        int size = Math.min(packet.unlocked.size(), MAX_ENTRIES);
        buf.writeVarInt(size);
        int written = 0;
        for (ResourceLocation skinId : packet.unlocked) {
            if (written++ == MAX_ENTRIES) {
                break;
            }
            buf.writeResourceLocation(skinId);
        }
    }

    private static SyncUnlockedSkinsPacket decode(RegistryFriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
        Set<ResourceLocation> unlocked = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            unlocked.add(buf.readResourceLocation());
        }
        return new SyncUnlockedSkinsPacket(unlocked);
    }

    public static void handle(SyncUnlockedSkinsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientItemSkinCache.setUnlocked(payload.unlocked));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
