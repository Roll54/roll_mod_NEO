package com.roll_54.roll_mod.minestar.battlepass;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The battle pass's saved data: each player's points and claims, and the season's task batches with progress. */
public class BattlePassState extends SavedData {

    public static final String NAME = "roll_mod_battle_pass";

    public enum Kind { EASY, HARD, PRIME, SEASON }

    /**
     * One battle-pass task: a daily task's kind of work at a bigger amount ({@code taskId} a daily task id), or a
     * quest of the book ({@code taskId} {@code "quest:HEX"}, required 1), done when the quest is.
     */
    public record Task(String taskId, Kind kind, int required) {}

    /** The tasks dealt on one Monday or Thursday; they stay until the season ends. */
    public record Batch(long day, List<Task> tasks) {}

    public static final class Player {
        public long points;
        public final long[] claimedFree = new long[2];
        public long claimedPrime;
        /** "day:slot" → progress, and the ones finished. */
        public final Map<String, Integer> progress = new HashMap<>();
        public final Set<String> done = new HashSet<>();

        public boolean freeClaimed(int level) {
            int i = level - 1;
            return (claimedFree[i >> 6] & (1L << (i & 63))) != 0;
        }

        public void claimFree(int level) {
            int i = level - 1;
            claimedFree[i >> 6] |= 1L << (i & 63);
        }

        public boolean primeClaimed(int level) {
            return (claimedPrime & (1L << (level - 1))) != 0;
        }

        public void claimPrime(int level) {
            claimedPrime |= 1L << (level - 1);
        }
    }

    public int season = 1;
    public final List<Batch> batches = new ArrayList<>();
    /** The season's own tasks, dealt once at its start; their progress keys are {@code "season:slot"}. */
    public final List<Task> seasonal = new ArrayList<>();
    public final Map<UUID, Player> players = new HashMap<>();

    public Player player(UUID id) {
        return players.computeIfAbsent(id, k -> new Player());
    }

    public static BattlePassState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(BattlePassState::new, BattlePassState::load), NAME);
    }

    public static BattlePassState load(CompoundTag tag, HolderLookup.Provider provider) {
        BattlePassState s = new BattlePassState();
        s.season = tag.contains("season") ? tag.getInt("season") : 1;
        ListTag bs = tag.getList("batches", Tag.TAG_COMPOUND);
        for (int i = 0; i < bs.size(); i++) {
            CompoundTag b = bs.getCompound(i);
            List<Task> tasks = new ArrayList<>();
            ListTag ts = b.getList("tasks", Tag.TAG_COMPOUND);
            for (int j = 0; j < ts.size(); j++) {
                CompoundTag t = ts.getCompound(j);
                Kind kind;
                try {
                    kind = Kind.valueOf(t.getString("kind"));
                } catch (RuntimeException e) {
                    kind = Kind.EASY;
                }
                tasks.add(new Task(t.getString("task"), kind, t.getInt("required")));
            }
            s.batches.add(new Batch(b.getLong("day"), List.copyOf(tasks)));
        }
        ListTag ss = tag.getList("seasonal", Tag.TAG_COMPOUND);
        for (int j = 0; j < ss.size(); j++) {
            CompoundTag t = ss.getCompound(j);
            s.seasonal.add(new Task(t.getString("task"), Kind.SEASON, t.getInt("required")));
        }
        ListTag ps = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < ps.size(); i++) {
            CompoundTag p = ps.getCompound(i);
            Player player = s.player(p.getUUID("id"));
            player.points = p.getLong("points");
            long[] free = p.getLongArray("free");
            System.arraycopy(free, 0, player.claimedFree, 0, Math.min(2, free.length));
            player.claimedPrime = p.getLong("prime");
            CompoundTag prog = p.getCompound("progress");
            for (String key : prog.getAllKeys()) player.progress.put(key, prog.getInt(key));
            ListTag done = p.getList("done", Tag.TAG_STRING);
            for (int j = 0; j < done.size(); j++) player.done.add(done.getString(j));
        }
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("season", season);
        ListTag bs = new ListTag();
        for (Batch batch : batches) {
            CompoundTag b = new CompoundTag();
            b.putLong("day", batch.day());
            ListTag ts = new ListTag();
            for (Task task : batch.tasks()) {
                CompoundTag t = new CompoundTag();
                t.putString("task", task.taskId());
                t.putString("kind", task.kind().name());
                t.putInt("required", task.required());
                ts.add(t);
            }
            b.put("tasks", ts);
            bs.add(b);
        }
        tag.put("batches", bs);
        ListTag ss = new ListTag();
        for (Task task : seasonal) {
            CompoundTag t = new CompoundTag();
            t.putString("task", task.taskId());
            t.putInt("required", task.required());
            ss.add(t);
        }
        tag.put("seasonal", ss);
        ListTag ps = new ListTag();
        players.forEach((id, player) -> {
            CompoundTag p = new CompoundTag();
            p.putUUID("id", id);
            p.putLong("points", player.points);
            p.putLongArray("free", player.claimedFree);
            p.putLong("prime", player.claimedPrime);
            CompoundTag prog = new CompoundTag();
            player.progress.forEach(prog::putInt);
            p.put("progress", prog);
            ListTag done = new ListTag();
            player.done.forEach(d -> done.add(net.minecraft.nbt.StringTag.valueOf(d)));
            p.put("done", done);
            ps.add(p);
        });
        tag.put("players", ps);
        return tag;
    }
}
