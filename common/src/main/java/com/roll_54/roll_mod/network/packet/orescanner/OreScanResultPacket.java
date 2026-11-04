package com.roll_54.roll_mod.network.packet.orescanner;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.electricItems.orescanner.ClientOreScanCache;
import com.roll_54.roll_mod.items.electricItems.orescanner.OreScanResult;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Server → client: an ore scanner's result, sent right before its screen opens. */
public record OreScanResultPacket(OreScanResult result) implements CustomPacketPayload {

    public static final Type<OreScanResultPacket> TYPE = new Type<>(RollMod.id("ore_scan_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreScanResultPacket> STREAM_CODEC =
            StreamCodec.of(OreScanResultPacket::encode, OreScanResultPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, OreScanResultPacket packet) {
        OreScanResult result = packet.result;
        buf.writeVarInt(result.radius());
        buf.writeVarInt(result.centerX());
        buf.writeVarInt(result.centerZ());
        buf.writeVarInt(result.playerX());
        buf.writeVarInt(result.playerZ());
        buf.writeVarInt(result.palette().size());
        result.palette().forEach(buf::writeUtf);
        for (OreScanResult.Chunk chunk : result.chunks()) {
            for (int[] column : chunk.columns()) {
                buf.writeVarInt(column.length / 2);
                for (int value : column) buf.writeVarInt(value);
            }
        }
    }

    private static OreScanResultPacket decode(RegistryFriendlyByteBuf buf) {
        int radius = buf.readVarInt();
        int centerX = buf.readVarInt();
        int centerZ = buf.readVarInt();
        int playerX = buf.readVarInt();
        int playerZ = buf.readVarInt();
        int paletteSize = buf.readVarInt();
        List<String> palette = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) palette.add(buf.readUtf());
        int size = 2 * radius + 1;
        List<OreScanResult.Chunk> chunks = new ArrayList<>(size * size);
        for (int i = 0; i < size * size; i++) {
            int[][] columns = new int[256][];
            for (int c = 0; c < 256; c++) {
                int[] column = new int[buf.readVarInt() * 2];
                for (int j = 0; j < column.length; j++) column[j] = buf.readVarInt();
                columns[c] = column;
            }
            chunks.add(OreScanResult.Chunk.of(columns, palette));
        }
        return new OreScanResultPacket(new OreScanResult(radius, centerX, centerZ, playerX, playerZ, List.copyOf(palette), chunks));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OreScanResultPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientOreScanCache.set(packet.result()));
    }
}
