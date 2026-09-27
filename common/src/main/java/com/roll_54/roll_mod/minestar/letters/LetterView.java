package com.roll_54.roll_mod.minestar.letters;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A letter as one reader receives it: the content, whether <em>they</em> accepted it, and — for the
 * moderation tab — how many people have. The rewards always travel, accepted or not: the letter is
 * read in full either way, and accepting only gates the button.
 *
 * <p>{@code personal} marks a letter addressed to this reader alone — a server notice. The reader
 * tab shows it like any other; the staff composer leaves it out, it is not theirs to manage.
 */
public record LetterView(UUID id, String title, String body, String author, long createdAt, long sendAt,
                         long expiresAt, List<LetterReward> rewards, String icon, boolean accepted,
                         int acceptedCount, boolean personal) {

    /** The same test {@link Letter#visible} makes, on the client's clock. */
    public boolean visible(long now) {
        return !due(now) && !expired(now);
    }

    public boolean due(long now) {
        return sendAt > now;
    }

    public boolean expired(long now) {
        return expiresAt != 0L && now >= expiresAt;
    }

    public static LetterView of(Letter letter, UUID reader) {
        return new LetterView(letter.id(), letter.title(), letter.body(), letter.author(),
                letter.createdAt(), letter.sendAt(), letter.expiresAt(), letter.rewards(), letter.icon(),
                letter.acceptedBy(reader), letter.accepted().size(), letter.recipient() != null);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(title, Letter.MAX_TITLE);
        buf.writeUtf(body, Letter.MAX_BODY);
        buf.writeUtf(author, 64);
        buf.writeVarLong(createdAt);
        buf.writeVarLong(sendAt);
        buf.writeVarLong(expiresAt);
        int count = Math.min(Letter.MAX_REWARDS, rewards.size());
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) rewards.get(i).encode(buf);
        buf.writeUtf(icon, LetterIcon.MAX);
        buf.writeBoolean(accepted);
        buf.writeVarInt(acceptedCount);
        buf.writeBoolean(personal);
    }

    public static LetterView decode(RegistryFriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String title = buf.readUtf(Letter.MAX_TITLE);
        String body = buf.readUtf(Letter.MAX_BODY);
        String author = buf.readUtf(64);
        long created = buf.readVarLong();
        long sendAt = buf.readVarLong();
        long expires = buf.readVarLong();
        int count = Math.min(Letter.MAX_REWARDS, buf.readVarInt());
        List<LetterReward> rewards = new ArrayList<>(count);
        for (int i = 0; i < count; i++) rewards.add(LetterReward.decode(buf));
        String icon = buf.readUtf(LetterIcon.MAX);
        return new LetterView(id, title, body, author, created, sendAt, expires, List.copyOf(rewards),
                icon, buf.readBoolean(), buf.readVarInt(), buf.readBoolean());
    }
}
