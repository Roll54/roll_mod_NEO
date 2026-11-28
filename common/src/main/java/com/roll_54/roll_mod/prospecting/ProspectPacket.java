package com.roll_54.roll_mod.prospecting;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server → client: the player's prospecting finds. {@code replaceAll} on login (everything they have); otherwise the
 * chunks that just changed. {@code report}, when not null, is what the pick just found in its column, for the chat.
 */
public record ProspectPacket(boolean replaceAll, List<ChunkFinds> chunks, List<Discovery> report)
        implements CustomPacketPayload {

    /** One chunk's finds; {@code ores} empty means the chunk has none left. */
    public record ChunkFinds(String dimension, long chunk, List<Discovery> ores) {}

    public static final Type<ProspectPacket> TYPE = new Type<>(RollMod.id("prospect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProspectPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.replaceAll());
                buf.writeVarInt(p.chunks().size());
                for (ChunkFinds c : p.chunks()) {
                    buf.writeUtf(c.dimension(), 256);
                    buf.writeLong(c.chunk());
                    writeList(buf, c.ores());
                }
                buf.writeBoolean(p.report() != null);
                if (p.report() != null) writeList(buf, p.report());
            },
            buf -> {
                boolean replace = buf.readBoolean();
                int n = buf.readVarInt();
                List<ChunkFinds> chunks = new ArrayList<>(Math.min(n, 4096));
                for (int i = 0; i < n; i++) {
                    chunks.add(new ChunkFinds(buf.readUtf(256), buf.readLong(), readList(buf)));
                }
                List<Discovery> report = buf.readBoolean() ? readList(buf) : null;
                return new ProspectPacket(replace, chunks, report);
            });

    private static void writeList(RegistryFriendlyByteBuf buf, List<Discovery> list) {
        buf.writeVarInt(list.size());
        for (Discovery d : list) d.write(buf);
    }

    private static List<Discovery> readList(RegistryFriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), 256);
        List<Discovery> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(Discovery.read(buf));
        return out;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProspectPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientProspects.accept(payload));
    }
}
