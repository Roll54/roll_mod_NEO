package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.letters.ClientLetterCache;
import com.roll_54.roll_mod.minestar.letters.LetterView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Every visible letter, as the receiving player sees it. */
public record SyncLettersPacket(List<LetterView> letters) implements CustomPacketPayload {

    /** Past this the oldest are left off; a letter list that long wants expiries, not a bigger packet. */
    private static final int MAX = 100;

    public static final Type<SyncLettersPacket> TYPE = new Type<>(RollMod.id("sync_letters"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLettersPacket> STREAM_CODEC =
            StreamCodec.of(SyncLettersPacket::encode, SyncLettersPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, SyncLettersPacket p) {
        int count = Math.min(MAX, p.letters.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) p.letters.get(i).encode(buf);
    }

    private static SyncLettersPacket decode(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX, buf.readVarInt());
        List<LetterView> letters = new ArrayList<>(count);
        for (int i = 0; i < count; i++) letters.add(LetterView.decode(buf));
        return new SyncLettersPacket(List.copyOf(letters));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncLettersPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientLetterCache.accept(payload.letters()));
    }
}
