package com.roll_54.roll_mod.network.packet.skin;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.cosmetics.SkinCategory;
import com.roll_54.roll_mod.cosmetics.client.ClientItemSkinCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server → <em>every</em> client: which skin {@code player} is currently rendering in which slot.
 *
 * <p>Broadcast rather than sent to the owner, because the whole point is that other players see the
 * skin. It stays cheap to broadcast precisely because this is only the active half of
 * {@code PlayerItemSkins} — at most one id per slot — and never the unlock list.
 *
 * <p>Sending slots rather than item ids is what lets one choice cover every sword in the pack: the
 * receiving client resolves a held item to its slot locally, against tags it already has.
 *
 * <p>An empty map means "forget this player", which is how logout is announced.
 */
public record SyncActiveSkinsPacket(UUID player, Map<SkinCategory, ResourceLocation> active)
        implements CustomPacketPayload {

    public static final Type<SyncActiveSkinsPacket> TYPE = new Type<>(RollMod.id("sync_active_skins"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncActiveSkinsPacket> STREAM_CODEC =
            StreamCodec.of(SyncActiveSkinsPacket::encode, SyncActiveSkinsPacket::decode);

    /** One entry per slot is the most that can be meaningful; only here to bound a malformed packet. */
    private static final int MAX_ENTRIES = SkinCategory.values().length;

    private static void encode(RegistryFriendlyByteBuf buf, SyncActiveSkinsPacket packet) {
        buf.writeUUID(packet.player);
        buf.writeVarInt(Math.min(packet.active.size(), MAX_ENTRIES));
        int written = 0;
        for (Map.Entry<SkinCategory, ResourceLocation> entry : packet.active.entrySet()) {
            if (written++ == MAX_ENTRIES) {
                break;
            }
            SkinCategory.STREAM_CODEC.encode(buf, entry.getKey());
            buf.writeResourceLocation(entry.getValue());
        }
    }

    private static SyncActiveSkinsPacket decode(RegistryFriendlyByteBuf buf) {
        UUID player = buf.readUUID();
        int count = Math.min(buf.readVarInt(), MAX_ENTRIES);
        Map<SkinCategory, ResourceLocation> active = new EnumMap<>(SkinCategory.class);
        for (int i = 0; i < count; i++) {
            active.put(SkinCategory.STREAM_CODEC.decode(buf), buf.readResourceLocation());
        }
        return new SyncActiveSkinsPacket(player, active);
    }

    public static void handle(SyncActiveSkinsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientItemSkinCache.put(payload.player, payload.active));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
