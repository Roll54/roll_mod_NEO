package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.ClientHueriStatue;
import com.roll_54.roll_mod.blocks.HueriStatueFlowers;
import com.roll_54.roll_mod.blocks.HueriStatuePlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Opening a Hueri statue, with the players its text names — see {@link HueriStatuePlayers}. */
public final class HueriStatuePackets {

    private HueriStatuePackets() {}

    /** Client → server: open the statue at {@code pos}; its text names {@code players}. */
    public record Open(BlockPos pos, List<UUID> players) implements CustomPacketPayload {

        public static final Type<Open> TYPE = new Type<>(RollMod.id("hueri_statue_open"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.pos);
                    int count = Math.min(HueriStatuePlayers.MAX, p.players.size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) buf.writeUUID(p.players.get(i));
                },
                buf -> {
                    BlockPos pos = buf.readBlockPos();
                    int count = Math.min(HueriStatuePlayers.MAX, buf.readVarInt());
                    List<UUID> players = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) players.add(buf.readUUID());
                    return new Open(pos, List.copyOf(players));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    HueriStatuePlayers.open(player, payload.pos, payload.players);
                }
            });
        }
    }

    /** Client → server: leave flowers at the statue at {@code pos} — see {@link HueriStatueFlowers}. */
    public record Donate(BlockPos pos) implements CustomPacketPayload {

        public static final Type<Donate> TYPE = new Type<>(RollMod.id("hueri_statue_donate"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Donate> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBlockPos(p.pos),
                buf -> new Donate(buf.readBlockPos()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Donate payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    HueriStatueFlowers.donate(player, payload.pos);
                }
            });
        }
    }

    /** Server → client: the players asked about, sent just before the screen opens. */
    public record Players(List<HueriStatuePlayers.Entry> players) implements CustomPacketPayload {

        public static final Type<Players> TYPE = new Type<>(RollMod.id("hueri_statue_players"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Players> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    int count = Math.min(HueriStatuePlayers.MAX, p.players.size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) p.players.get(i).encode(buf);
                },
                buf -> {
                    int count = Math.min(HueriStatuePlayers.MAX, buf.readVarInt());
                    List<HueriStatuePlayers.Entry> players = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) players.add(HueriStatuePlayers.Entry.decode(buf));
                    return new Players(List.copyOf(players));
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Players payload, IPayloadContext context) {
            context.enqueueWork(() -> {
                for (HueriStatuePlayers.Entry entry : payload.players) ClientHueriStatue.PLAYERS.put(entry.id(), entry);
            });
        }
    }
}
