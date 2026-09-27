package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.letters.Letter;
import com.roll_54.roll_mod.minestar.letters.LetterIcon;
import com.roll_54.roll_mod.minestar.letters.LetterReward;
import com.roll_54.roll_mod.minestar.letters.LetterService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client → server: send or withdraw a letter. Staff-only; {@link LetterService} re-checks.
 *
 * <p>Kept apart from {@link LetterAcceptPacket} on purpose: this is a multi-kilobyte payload from a
 * handful of trusted players, that one is two fields from anyone, and they should not share a
 * decoder.
 */
public record LetterComposePacket(Action action, UUID letter, String title, String body, long delay,
                                  long lifetime, List<LetterReward> rewards, String icon) implements CustomPacketPayload {

    /** Append only: the codec sends the ordinal. */
    public enum Action { CREATE, DELETE, UPDATE }

    private static final UUID NONE = new UUID(0, 0);

    public static final Type<LetterComposePacket> TYPE = new Type<>(RollMod.id("letter_compose"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LetterComposePacket> STREAM_CODEC =
            StreamCodec.of(LetterComposePacket::encode, LetterComposePacket::decode);

    public static LetterComposePacket create(String title, String body, long delay, long lifetime,
                                             List<LetterReward> rewards, String icon) {
        return content(Action.CREATE, NONE, title, body, delay, lifetime, rewards, icon);
    }

    /** Saves an edit to {@code letter}; see {@code LetterService.update} for what is kept. */
    public static LetterComposePacket update(UUID letter, String title, String body, long delay, long lifetime,
                                             List<LetterReward> rewards, String icon) {
        return content(Action.UPDATE, letter, title, body, delay, lifetime, rewards, icon);
    }

    public static LetterComposePacket delete(UUID letter) {
        return new LetterComposePacket(Action.DELETE, letter, "", "", 0L, 0L, List.of(), "");
    }

    private static LetterComposePacket content(Action action, UUID letter, String title, String body,
                                               long delay, long lifetime, List<LetterReward> rewards,
                                               String icon) {
        return new LetterComposePacket(action, letter, clamp(title, Letter.MAX_TITLE),
                clamp(body, Letter.MAX_BODY), delay, lifetime,
                rewards.size() <= Letter.MAX_REWARDS ? rewards : rewards.subList(0, Letter.MAX_REWARDS),
                clamp(icon, LetterIcon.MAX));
    }

    /** {@code writeUtf} throws past its limit, so the text is cut here rather than failing the send. */
    private static String clamp(String text, int max) {
        String clean = text == null ? "" : text;
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private static void encode(RegistryFriendlyByteBuf buf, LetterComposePacket p) {
        buf.writeVarInt(p.action.ordinal());
        buf.writeUUID(p.letter);
        buf.writeUtf(p.title, Letter.MAX_TITLE);
        buf.writeUtf(p.body, Letter.MAX_BODY);
        buf.writeVarLong(Math.max(0L, p.delay));
        buf.writeVarLong(Math.max(0L, p.lifetime));
        int count = Math.min(Letter.MAX_REWARDS, p.rewards.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) p.rewards.get(i).encode(buf);
        buf.writeUtf(p.icon, LetterIcon.MAX);
    }

    private static LetterComposePacket decode(RegistryFriendlyByteBuf buf) {
        Action action = Action.values()[Math.floorMod(buf.readVarInt(), Action.values().length)];
        UUID letter = buf.readUUID();
        String title = buf.readUtf(Letter.MAX_TITLE);
        String body = buf.readUtf(Letter.MAX_BODY);
        long delay = buf.readVarLong();
        long lifetime = buf.readVarLong();
        int count = Math.min(Letter.MAX_REWARDS, buf.readVarInt());
        List<LetterReward> rewards = new ArrayList<>(count);
        for (int i = 0; i < count; i++) rewards.add(LetterReward.decode(buf));
        return new LetterComposePacket(action, letter, title, body, delay, lifetime, List.copyOf(rewards),
                buf.readUtf(LetterIcon.MAX));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LetterComposePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer author)) return;
            switch (payload.action) {
                case CREATE -> LetterService.create(author, payload.title, payload.body,
                        payload.delay, payload.lifetime, payload.rewards, payload.icon);
                case UPDATE -> LetterService.update(author, payload.letter, payload.title, payload.body,
                        payload.delay, payload.lifetime, payload.rewards, payload.icon);
                case DELETE -> LetterService.delete(author, payload.letter);
            }
        });
    }
}
