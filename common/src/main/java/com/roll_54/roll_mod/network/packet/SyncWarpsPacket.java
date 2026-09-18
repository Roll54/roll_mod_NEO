package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.hub.warp.ClientWarpCache;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.minestar.hub.warp.WarpApproval;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server → client: every warp on the server, plus how many the receiving player may own. Sent when
 * the hub opens and whenever a warp is created or deleted.
 */
public record SyncWarpsPacket(List<Warp> warps, int limit) implements CustomPacketPayload {

    public static final Type<SyncWarpsPacket> TYPE = new Type<>(RollMod.id("sync_warps"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncWarpsPacket> STREAM_CODEC =
            StreamCodec.of(SyncWarpsPacket::encode, SyncWarpsPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncWarpsPacket packet) {
        buf.writeVarInt(packet.limit);
        buf.writeVarInt(packet.warps.size());
        for (Warp warp : packet.warps) {
            buf.writeUUID(warp.id());
            buf.writeUUID(warp.owner());
            buf.writeUtf(warp.ownerName());
            buf.writeUtf(warp.name());
            buf.writeUtf(warp.description());
            buf.writeResourceLocation(warp.dimension());
            buf.writeDouble(warp.x());
            buf.writeDouble(warp.y());
            buf.writeDouble(warp.z());
            buf.writeFloat(warp.yaw());
            buf.writeFloat(warp.pitch());
            buf.writeVarLong(warp.price());
            buf.writeVarLong(warp.created());
            buf.writeUtf(warp.approval().id());
            buf.writeUtf(warp.moderator());
            // Only the count is needed on the client; who reported it is moderation's business,
            // and who visited is nobody's.
            buf.writeVarInt(warp.reports().size());
            buf.writeVarInt(warp.visitorsToday());
            buf.writeVarLong(warp.visitsTotal());
        }
    }

    private static SyncWarpsPacket decode(RegistryFriendlyByteBuf buf) {
        int limit = buf.readVarInt();
        int count = buf.readVarInt();
        List<Warp> warps = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID();
            UUID owner = buf.readUUID();
            String ownerName = buf.readUtf();
            String name = buf.readUtf();
            String description = buf.readUtf();
            ResourceLocation dimension = buf.readResourceLocation();
            double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
            float yaw = buf.readFloat(), pitch = buf.readFloat();
            long price = buf.readVarLong(), created = buf.readVarLong();
            WarpApproval approval = WarpApproval.byId(buf.readUtf());
            String moderator = buf.readUtf();
            int reportCount = buf.readVarInt();
            int visitorCount = buf.readVarInt();
            long visitsTotal = buf.readVarLong();

            // The client only ever asks how many reports there are, so stand in that many synthetic
            // ids rather than sending real ones — a report is between the reporter and moderation.
            // Today's visitors are stood in the same way, and stamped with the current period so
            // Warp.visitorsToday() reports the number the server sent instead of zeroing it.
            warps.add(new Warp(id, owner, ownerName, name, description, dimension, x, y, z,
                    yaw, pitch, price, created, approval, moderator, synthetic(reportCount),
                    synthetic(visitorCount), DailyTaskManager.currentPeriodDay(), visitsTotal));
        }
        return new SyncWarpsPacket(warps, limit);
    }

    /** Stand-in ids for a count the client is told but the identities of which it is not. */
    private static Set<UUID> synthetic(int count) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            ids.add(new UUID(0, i));
        }
        return Set.copyOf(ids);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncWarpsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientWarpCache.WARPS = List.copyOf(payload.warps);
            ClientWarpCache.LIMIT = payload.limit;
        });
    }
}
