package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.letters.LetterService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Client → server: "delete this letter from my list". Anyone may send it; the store only lets a
 * reader delete a letter that is for them and that they have already accepted.
 */
public record LetterDismissPacket(UUID letter) implements CustomPacketPayload {

    public static final Type<LetterDismissPacket> TYPE = new Type<>(RollMod.id("letter_dismiss"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LetterDismissPacket> STREAM_CODEC =
            StreamCodec.of((buf, p) -> buf.writeUUID(p.letter), buf -> new LetterDismissPacket(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LetterDismissPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer reader) LetterService.dismiss(reader, payload.letter);
        });
    }
}
