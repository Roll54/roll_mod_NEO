package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.letters.LetterService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Client → server: "I take this letter". Anyone may send it; the store pays each reader once. */
public record LetterAcceptPacket(UUID letter) implements CustomPacketPayload {

    public static final Type<LetterAcceptPacket> TYPE = new Type<>(RollMod.id("letter_accept"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LetterAcceptPacket> STREAM_CODEC =
            StreamCodec.of((buf, p) -> buf.writeUUID(p.letter), buf -> new LetterAcceptPacket(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LetterAcceptPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer reader) LetterService.accept(reader, payload.letter);
        });
    }
}
