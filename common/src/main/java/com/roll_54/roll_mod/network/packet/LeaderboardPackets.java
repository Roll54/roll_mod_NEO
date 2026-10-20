package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.leaderboard.ClientLeaderboardCache;
import com.roll_54.roll_mod.minestar.leaderboard.LeaderboardCategory;
import com.roll_54.roll_mod.minestar.leaderboard.LeaderboardRow;
import com.roll_54.roll_mod.minestar.leaderboard.LeaderboardService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The leaderboard tab's two packets: the client names a board, the server sends it. */
public final class LeaderboardPackets {

    /** Boards are cached server-side, but each request still costs a send; this bounds spam-clicking. */
    private static final long MIN_INTERVAL_MILLIS = 300L;
    private static final Map<UUID, Long> LAST_REQUEST = new ConcurrentHashMap<>();

    private LeaderboardPackets() {}

    private static LeaderboardCategory categoryOf(int ordinal) {
        // Modulo rather than a bounds check: a crafted ordinal must not throw on the network thread.
        return LeaderboardCategory.values()[Math.floorMod(ordinal, LeaderboardCategory.values().length)];
    }

    /** Client → server: send me this board. */
    public record Request(LeaderboardCategory category) implements CustomPacketPayload {

        public static final Type<Request> TYPE = new Type<>(RollMod.id("leaderboard_request"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeVarInt(payload.category().ordinal()),
                buf -> new Request(categoryOf(buf.readVarInt())));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Request payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                long now = System.currentTimeMillis();
                Long last = LAST_REQUEST.put(player.getUUID(), now);
                if (last != null && now - last < MIN_INTERVAL_MILLIS) return;
                LeaderboardService.request(player, payload.category());
            });
        }
    }

    /** Server → client: the top rows, and the viewer's own line ({@code null} when there is none). */
    public record Sync(LeaderboardCategory category, List<LeaderboardRow> rows, @Nullable LeaderboardRow own,
                       boolean failed) implements CustomPacketPayload {

        public static final Type<Sync> TYPE = new Type<>(RollMod.id("leaderboard_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeVarInt(payload.category().ordinal());
                    int count = Math.min(LeaderboardService.TOP, payload.rows().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) payload.rows().get(i).encode(buf);
                    buf.writeBoolean(payload.own() != null);
                    if (payload.own() != null) payload.own().encode(buf);
                    buf.writeBoolean(payload.failed());
                },
                buf -> {
                    LeaderboardCategory category = categoryOf(buf.readVarInt());
                    int count = Math.min(LeaderboardService.TOP, buf.readVarInt());
                    List<LeaderboardRow> rows = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) rows.add(LeaderboardRow.decode(buf));
                    LeaderboardRow own = buf.readBoolean() ? LeaderboardRow.decode(buf) : null;
                    return new Sync(category, List.copyOf(rows), own, buf.readBoolean());
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sync payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ClientLeaderboardCache.CATEGORY = payload.category();
                ClientLeaderboardCache.ROWS = payload.rows();
                ClientLeaderboardCache.OWN = payload.own();
                ClientLeaderboardCache.FAILED = payload.failed();
                ClientLeaderboardCache.LOADED = true;
                ClientLeaderboardCache.VERSION++;
            });
        }
    }
}
