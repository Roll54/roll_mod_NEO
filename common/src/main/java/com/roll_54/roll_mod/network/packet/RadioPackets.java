package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.radio.ClientRadioCache;
import com.roll_54.roll_mod.minestar.radio.ClientRadioCache.Station;
import com.roll_54.roll_mod.minestar.radio.RadioService;
import com.roll_54.roll_mod.minestar.radio.RadioSettings;
import earth.terrarium.adastra.common.utils.radio.StationLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The radio implant's packets: the owner's tuning both ways, and who is broadcasting what. */
public final class RadioPackets {

    private static final int MAX_URL = 512;
    private static final int MAX_STATIONS = 256;
    private static final int MAX_BROADCASTS = 1000;

    private RadioPackets() {}

    /** Client → server: tune to this station, and hear / broadcast it or not. */
    public record Tune(String station, boolean hear, boolean broadcast) implements CustomPacketPayload {

        public static final Type<Tune> TYPE = new Type<>(RollMod.id("radio_tune"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Tune> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(clamp(p.station(), MAX_URL), MAX_URL);
                    buf.writeBoolean(p.hear());
                    buf.writeBoolean(p.broadcast());
                },
                buf -> new Tune(buf.readUtf(MAX_URL), buf.readBoolean(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Tune payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                // Only an owner may tune, and only to a station on the list: broadcasting means every
                // client nearby opens the url, so it cannot be anything a player types.
                if (!RadioService.hasImplant(player)) return;
                String station = payload.station().isEmpty() || StationLoader.hasStation(payload.station())
                        ? payload.station() : RadioSettings.get(player.getUUID()).station();
                RadioSettings.set(player, new RadioSettings.Setting(station, payload.hear(), payload.broadcast()));
                RadioService.markDirty();
            });
        }
    }

    /**
     * Server → client: the owner's own state. {@code stations} is null when unchanged since the last
     * sync, so the list is not resent every time a switch flips.
     */
    public record Sync(boolean installed, String station, boolean hear, boolean broadcast,
                       @Nullable List<Station> stations) implements CustomPacketPayload {

        public static final Type<Sync> TYPE = new Type<>(RollMod.id("radio_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBoolean(p.installed());
                    buf.writeUtf(clamp(p.station(), MAX_URL), MAX_URL);
                    buf.writeBoolean(p.hear());
                    buf.writeBoolean(p.broadcast());
                    buf.writeBoolean(p.stations() != null);
                    if (p.stations() != null) {
                        int count = Math.min(MAX_STATIONS, p.stations().size());
                        buf.writeVarInt(count);
                        for (int i = 0; i < count; i++) {
                            Station s = p.stations().get(i);
                            buf.writeUtf(clamp(s.url(), MAX_URL), MAX_URL);
                            buf.writeUtf(clamp(s.title(), 128), 128);
                            buf.writeUtf(clamp(s.name(), 128), 128);
                        }
                    }
                },
                buf -> {
                    boolean installed = buf.readBoolean();
                    String station = buf.readUtf(MAX_URL);
                    boolean hear = buf.readBoolean();
                    boolean broadcast = buf.readBoolean();
                    List<Station> stations = null;
                    if (buf.readBoolean()) {
                        int count = Math.min(MAX_STATIONS, buf.readVarInt());
                        List<Station> list = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            list.add(new Station(buf.readUtf(MAX_URL), buf.readUtf(128), buf.readUtf(128)));
                        }
                        stations = List.copyOf(list);
                    }
                    return new Sync(installed, station, hear, broadcast, stations);
                });

        /** Same tuning, ignoring whether the station list rides along. */
        public boolean sameState(Sync other) {
            return installed == other.installed && hear == other.hear && broadcast == other.broadcast
                    && Objects.equals(station, other.station);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Sync payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ClientRadioCache.INSTALLED = payload.installed();
                ClientRadioCache.STATION = payload.station();
                ClientRadioCache.HEAR = payload.hear();
                ClientRadioCache.BROADCAST = payload.broadcast();
                if (payload.stations() != null) ClientRadioCache.STATIONS = payload.stations();
                ClientRadioCache.VERSION++;
            });
        }
    }

    /** Server → every client: entity id → station url, for each player broadcasting. */
    public record Broadcasts(Map<Integer, String> broadcasts) implements CustomPacketPayload {

        public static final Type<Broadcasts> TYPE = new Type<>(RollMod.id("radio_broadcasts"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Broadcasts> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    int count = Math.min(MAX_BROADCASTS, p.broadcasts().size());
                    buf.writeVarInt(count);
                    int written = 0;
                    for (Map.Entry<Integer, String> e : p.broadcasts().entrySet()) {
                        if (written++ >= count) break;
                        buf.writeVarInt(e.getKey());
                        buf.writeUtf(clamp(e.getValue(), MAX_URL), MAX_URL);
                    }
                },
                buf -> {
                    int count = Math.min(MAX_BROADCASTS, buf.readVarInt());
                    Map<Integer, String> map = new HashMap<>(count);
                    for (int i = 0; i < count; i++) map.put(buf.readVarInt(), buf.readUtf(MAX_URL));
                    return new Broadcasts(Map.copyOf(map));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Broadcasts payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                ClientRadioCache.BROADCASTS = payload.broadcasts();
                ClientRadioCache.VERSION++;
            });
        }
    }

    /** {@code writeUtf} throws past its limit, so an over-long value is cut, not fatal. */
    private static String clamp(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
