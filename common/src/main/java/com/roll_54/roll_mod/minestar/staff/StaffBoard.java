package com.roll_54.roll_mod.minestar.staff;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Polls and player tickets, kept on the overworld's data storage. Polls are made by staff in the Moderation tab and
 * voted on by players in the Letters tab; tickets (a suggestion or a bug report) are written by players from the
 * Letters tab and read and answered by staff in the Moderation tab — the answer reaches the player as a letter.
 */
public class StaffBoard extends SavedData {

    public static final String NAME = "roll_mod_staff_board";

    /** A question with two or more answers; one vote per player, which they may change while it is open. */
    public static final class Poll {
        public UUID id = UUID.randomUUID();
        public String question = "";
        public final List<String> options = new ArrayList<>();
        public String author = "";
        public long created;
        public boolean closed;
        /** Player → the index of the answer they picked. */
        public final Map<UUID, Integer> votes = new HashMap<>();

        public int[] counts() {
            int[] out = new int[options.size()];
            for (int v : votes.values()) if (v >= 0 && v < out.length) out[v]++;
            return out;
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", id);
            t.putString("question", question);
            ListTag opts = new ListTag();
            options.forEach(o -> opts.add(StringTag.valueOf(o)));
            t.put("options", opts);
            t.putString("author", author);
            t.putLong("created", created);
            t.putBoolean("closed", closed);
            ListTag vs = new ListTag();
            votes.forEach((who, idx) -> {
                CompoundTag v = new CompoundTag();
                v.putUUID("who", who);
                v.putInt("at", idx);
                vs.add(v);
            });
            t.put("votes", vs);
            return t;
        }

        static Poll load(CompoundTag t) {
            Poll p = new Poll();
            p.id = t.getUUID("id");
            p.question = t.getString("question");
            ListTag opts = t.getList("options", Tag.TAG_STRING);
            for (int i = 0; i < opts.size(); i++) p.options.add(opts.getString(i));
            p.author = t.getString("author");
            p.created = t.getLong("created");
            p.closed = t.getBoolean("closed");
            ListTag vs = t.getList("votes", Tag.TAG_COMPOUND);
            for (int i = 0; i < vs.size(); i++) p.votes.put(vs.getCompound(i).getUUID("who"), vs.getCompound(i).getInt("at"));
            return p;
        }
    }

    public enum TicketKind { SUGGESTION, BUG }

    public enum TicketStatus { OPEN, ANSWERED, CLOSED }

    /** A player's suggestion or bug report to the staff. */
    public static final class Ticket {
        public UUID id = UUID.randomUUID();
        public UUID author;
        public String authorName = "";
        public TicketKind kind = TicketKind.SUGGESTION;
        public String title = "";
        public String body = "";
        public long created;
        public TicketStatus status = TicketStatus.OPEN;
        public String answer = "";
        public String answeredBy = "";

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", id);
            t.putUUID("author", author);
            t.putString("authorName", authorName);
            t.putString("kind", kind.name());
            t.putString("title", title);
            t.putString("body", body);
            t.putLong("created", created);
            t.putString("status", status.name());
            t.putString("answer", answer);
            t.putString("answeredBy", answeredBy);
            return t;
        }

        static Ticket load(CompoundTag t) {
            Ticket k = new Ticket();
            k.id = t.getUUID("id");
            k.author = t.getUUID("author");
            k.authorName = t.getString("authorName");
            k.kind = parse(TicketKind.class, t.getString("kind"), TicketKind.SUGGESTION);
            k.title = t.getString("title");
            k.body = t.getString("body");
            k.created = t.getLong("created");
            k.status = parse(TicketStatus.class, t.getString("status"), TicketStatus.OPEN);
            k.answer = t.getString("answer");
            k.answeredBy = t.getString("answeredBy");
            return k;
        }
    }

    /** Newest last, in the order they were made. */
    public final Map<UUID, Poll> polls = new LinkedHashMap<>();
    public final Map<UUID, Ticket> tickets = new LinkedHashMap<>();

    public static StaffBoard get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(StaffBoard::new, StaffBoard::load), NAME);
    }

    public static StaffBoard load(CompoundTag tag, HolderLookup.Provider provider) {
        StaffBoard b = new StaffBoard();
        ListTag ps = tag.getList("polls", Tag.TAG_COMPOUND);
        for (int i = 0; i < ps.size(); i++) {
            Poll p = Poll.load(ps.getCompound(i));
            b.polls.put(p.id, p);
        }
        ListTag ts = tag.getList("tickets", Tag.TAG_COMPOUND);
        for (int i = 0; i < ts.size(); i++) {
            Ticket t = Ticket.load(ts.getCompound(i));
            b.tickets.put(t.id, t);
        }
        return b;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag ps = new ListTag();
        polls.values().forEach(p -> ps.add(p.save()));
        tag.put("polls", ps);
        ListTag ts = new ListTag();
        tickets.values().forEach(t -> ts.add(t.save()));
        tag.put("tickets", ts);
        return tag;
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
