package com.roll_54.roll_mod.minestar.battlepass;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyCause;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.ledger.LedgerReason;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.repository.CurrencyLedger;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskRegistry;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskRewards;
import com.roll_54.roll_mod.minestar.hub.QuestGate;
import com.roll_54.roll_mod.minestar.teleport.TeleportService;
import com.roll_54.roll_mod.network.packet.BattlePassPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The battle pass: points from daily tasks (each claimed one), battle-pass tasks and events (the {@code /battlepass
 * points} command), 128 free levels and 64 Prime ones, each block of them opened by a quest. Points keep coming
 * while a block is still shut; only claiming waits for its quest.
 *
 * <p>Battle-pass tasks come in batches every Monday and Thursday (and on the season's first day): three easy, one
 * hard, and one more for Prime players. They stay for the whole season.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class BattlePassService {

    private static int tick;
    /** The hooks some battle-pass task listens to, so the per-event funnel can leave early; null = rebuild. */
    private static Set<DailyTaskHook> hooks;

    private BattlePassService() {}

    /* ---------------------------------------------- season & batches ---------------------------------------------- */

    /** Starts the season on first use, wipes the state when the config's season moved on, and deals due batches. */
    public static boolean rollIfNeeded(MinecraftServer server) {
        BattlePassConfig config = BattlePassConfig.get();
        BattlePassState state = BattlePassState.get(server);
        long today = DailyTaskManager.currentPeriodDay();
        boolean changed = false;
        if (config.seasonStartDay == Long.MIN_VALUE) {
            config.seasonStartDay = today;
            config.save();
        }
        if (state.season != config.season) {
            state.season = config.season;
            state.batches.clear();
            state.seasonal.clear();
            state.players.clear();
            changed = true;
        }
        // A season holding a task no longer in the pool (a quest, or a challenge taken out) is dealt again, its progress
        // cleared.
        if (state.seasonal.stream().anyMatch(t -> !config.seasonalChallenges.contains(t.taskId()))) {
            state.seasonal.clear();
            for (BattlePassState.Player p : state.players.values()) {
                p.progress.keySet().removeIf(k -> k.startsWith("season:"));
                p.done.removeIf(k -> k.startsWith("season:"));
            }
            changed = true;
        }
        // A challenge's amount changed since the season was dealt (made harder, say): the season follows it.
        for (int i = 0; i < state.seasonal.size(); i++) {
            BattlePassState.Task task = state.seasonal.get(i);
            DailyTask def = DailyTaskRegistry.byId(task.taskId());
            if (def != null && def.battlePassOnly() && def.baseAmount() != task.required()) {
                state.seasonal.set(i, new BattlePassState.Task(task.taskId(), task.kind(), Math.max(1, def.baseAmount())));
                changed = true;
            }
        }
        if (state.seasonal.isEmpty() && !config.seasonalChallenges.isEmpty()) {
            // The long challenges only.
            List<String> pool = new ArrayList<>();
            for (String id : config.seasonalChallenges) if (DailyTaskRegistry.byId(id) != null) pool.add(id);
            Collections.shuffle(pool, new Random(config.season * 0x632BE59BD9B4E019L ^ seasonSalt));
            seasonSalt = 0;
            for (int i = 0; i < Math.min(config.seasonalCount, pool.size()); i++) {
                DailyTask def = DailyTaskRegistry.byId(pool.get(i));
                state.seasonal.add(new BattlePassState.Task(pool.get(i), BattlePassState.Kind.SEASON,
                        def == null ? 1 : Math.max(1, def.baseAmount())));
            }
            changed = true;
        }
        // A batch holding a task no longer in the weekly pool (a quest of the book, a challenge taken out) is dealt
        // again for the same day, its progress cleared.
        java.util.Set<String> weekly = new java.util.HashSet<>(config.weeklyDailies);
        weekly.addAll(config.weeklyChallenges);
        for (int i = 0; i < state.batches.size(); i++) {
            BattlePassState.Batch batch = state.batches.get(i);
            if (batch.tasks().stream().allMatch(t -> weekly.contains(t.taskId()))) continue;
            BattlePassState.Batch fresh = deal(config, batch.day());
            if (fresh == null) continue;
            state.batches.set(i, fresh);
            String prefix = batch.day() + ":";
            for (BattlePassState.Player p : state.players.values()) {
                p.progress.keySet().removeIf(k -> k.startsWith(prefix));
                p.done.removeIf(k -> k.startsWith(prefix));
            }
            changed = true;
        }
        // Amounts changed since a batch was dealt (made harder, multipliers raised): the batch follows them.
        for (int i = 0; i < state.batches.size(); i++) {
            BattlePassState.Batch batch = state.batches.get(i);
            List<BattlePassState.Task> tasks = new ArrayList<>(batch.tasks());
            boolean moved = false;
            for (int t = 0; t < tasks.size(); t++) {
                BattlePassState.Task task = tasks.get(t);
                int want = required(config, DailyTaskRegistry.byId(task.taskId()), task.kind());
                if (want != task.required()) {
                    tasks.set(t, new BattlePassState.Task(task.taskId(), task.kind(), want));
                    moved = true;
                }
            }
            if (moved) {
                state.batches.set(i, new BattlePassState.Batch(batch.day(), List.copyOf(tasks)));
                changed = true;
            }
        }
        long from = Math.max(config.seasonStartDay, today - 366);
        for (long day = from; day <= today; day++) {
            boolean due = day == config.seasonStartDay;
            DayOfWeek dow = LocalDate.ofEpochDay(day).getDayOfWeek();
            due |= dow == DayOfWeek.MONDAY || dow == DayOfWeek.THURSDAY;
            if (!due) continue;
            long d = day;
            if (state.batches.stream().anyMatch(b -> b.day() == d)) continue;
            BattlePassState.Batch batch = deal(config, d);
            if (batch != null) {
                state.batches.add(batch);
                changed = true;
            }
        }
        if (changed) {
            hooks = null;
            state.setDirty();
        }
        return changed;
    }

    /** Mixed into the next season deal's seed by a reroll, so it draws differently; 0 otherwise. */
    private static long seasonSalt;

    private static BattlePassState.Batch deal(BattlePassConfig config, long day) {
        return deal(config, day, 0);
    }

    private static BattlePassState.Batch deal(BattlePassConfig config, long day, long salt) {
        // The weekly pool: the listed daily tasks (scaled up) and the pass's own challenges — no quests of the book.
        List<String> pool = new ArrayList<>();
        for (String id : config.weeklyDailies) if (DailyTaskRegistry.byId(id) != null) pool.add(id);
        for (String id : config.weeklyChallenges) if (DailyTaskRegistry.byId(id) != null) pool.add(id);
        int easy = Math.max(0, config.weeklyEasyCount), hard = Math.max(0, config.weeklyHardCount);
        int total = easy + hard + Math.max(0, config.weeklyPrimeCount);
        if (total <= 0 || pool.size() < total) return null;
        Collections.shuffle(pool, new Random(config.season * 0x9E3779B97F4A7C15L ^ day * 0xBF58476D1CE4E5B9L ^ salt));
        // The per-fish epic weeklies count as one entry: at most one of them per batch.
        boolean[] epic = {false};
        pool.removeIf(id -> {
            if (!id.startsWith("bp_epic_")) return false;
            if (epic[0]) return true;
            epic[0] = true;
            return false;
        });
        List<BattlePassState.Task> tasks = new ArrayList<>();
        for (int i = 0; i < Math.min(total, pool.size()); i++) {
            String id = pool.get(i);
            BattlePassState.Kind kind = i < easy ? BattlePassState.Kind.EASY : i < easy + hard ? BattlePassState.Kind.HARD
                    : BattlePassState.Kind.PRIME;
            int required = required(config, DailyTaskRegistry.byId(id), kind);
            tasks.add(new BattlePassState.Task(id, kind, required));
        }
        return new BattlePassState.Batch(day, List.copyOf(tasks));
    }

    /** What a weekly task of {@code kind} asks for: a challenge its own amount (twice on a hard slot), a daily scaled up. */
    private static int required(BattlePassConfig config, DailyTask task, BattlePassState.Kind kind) {
        if (task == null) return 1;
        if (task.battlePassOnly()) return Math.max(1, task.baseAmount() * (kind == BattlePassState.Kind.HARD ? 2 : 1));
        int mult = kind == BattlePassState.Kind.HARD ? config.hardMultiplier : config.easyMultiplier;
        return Math.max(1, task.baseAmount() * mult);
    }

    /** The quest a {@code "quest:HEX"} task names, or 0. */
    static long questOf(String taskId) {
        if (taskId == null || !taskId.startsWith("quest:")) return 0;
        try {
            return Long.parseUnsignedLong(taskId.substring(6), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Points a finished task of {@code kind} pays. */
    private static int pointsFor(BattlePassConfig config, BattlePassState.Kind kind) {
        return switch (kind) {
            case EASY -> config.pointsEasy;
            case HARD -> config.pointsHard;
            case PRIME -> config.pointsPrimeTask;
            case SEASON -> config.pointsSeasonal;
        };
    }

    /**
     * Marks every quest task whose quest {@code player}'s team has finished as done, paying its points. Run when the
     * pass is synced and every minute for those online.
     */
    static void checkQuests(ServerPlayer player) {
        BattlePassConfig config = BattlePassConfig.get();
        BattlePassState state = BattlePassState.get(player.server);
        BattlePassState.Player data = state.player(player.getUUID());
        boolean prime = TeleportService.isPrime(player);
        boolean dirty = false;
        List<String[]> keys = new ArrayList<>();
        for (BattlePassState.Batch batch : state.batches) {
            for (int slot = 0; slot < batch.tasks().size(); slot++) {
                BattlePassState.Task task = batch.tasks().get(slot);
                if (task.kind() == BattlePassState.Kind.PRIME && !prime) continue;
                keys.add(new String[]{batch.day() + ":" + slot, task.taskId(), task.kind().name()});
            }
        }
        for (int slot = 0; slot < state.seasonal.size(); slot++) {
            keys.add(new String[]{"season:" + slot, state.seasonal.get(slot).taskId(), "SEASON"});
        }
        for (String[] k : keys) {
            long quest = questOf(k[1]);
            if (quest == 0 || data.done.contains(k[0]) || !QuestGate.isCompleted(player, quest)) continue;
            data.done.add(k[0]);
            data.progress.put(k[0], 1);
            int points = pointsFor(config, BattlePassState.Kind.valueOf(k[2]));
            data.points += points;
            player.sendSystemMessage(Component.translatable("msg.roll_mod.battlepass.task_done",
                    QuestGate.title(false, quest), points).withStyle(ChatFormatting.GOLD));
            dirty = true;
        }
        if (dirty) state.setDirty();
    }

    /* ---------------------------------------------------- points --------------------------------------------------- */

    /** Starcoins paid for each point (cup) earned past the pass's last level. */
    /** @deprecated the rate is a task setting now ({@code TaskSettings.starcoinsPerCup}); kept, not deleted. */
    @Deprecated
    public static final long STARCOINS_PER_POINT = 10;

    /** Adds {@code amount} points (an event, a task, a daily) and tells the player. */
    public static void addPoints(ServerPlayer player, long amount, Component why) {
        if (amount <= 0) return;
        BattlePassState state = BattlePassState.get(player.server);
        BattlePassState.Player data = state.player(player.getUUID());
        // Past the pass's last level the points are worth nothing more, so they pay out as starcoins,
        // TaskSettings.starcoinsPerCup each. The cap
        // is the longer of the two tracks, so a free player who buys Prime later still has its levels to open.
        BattlePassConfig config = BattlePassConfig.get();
        long cap = Math.max((long) BattlePassConfig.FREE_LEVELS * Math.max(1, config.pointsPerLevel),
                (long) BattlePassConfig.PRIME_LEVELS * Math.max(1, config.pointsPerPrimeLevel));
        long total = data.points + amount;
        long excess = Math.max(0, total - Math.max(cap, data.points));
        data.points = Math.min(total, Math.max(cap, data.points));
        state.setDirty();
        long coins = excess * com.roll_54.roll_mod.minestar.dailytasks.TaskSettings.get().starcoinsPerCup;
        if (coins > 0) {
            CurrencyLedger.logged(CurrencyService.deposit(player, CurrencyType.MAIN, coins, player.server,
                            CurrencyCause.player(player, LedgerReason.BATTLE_PASS).withDetail("battle pass points past the last level")),
                    player.getUUID(), CurrencyType.MAIN, coins, LedgerReason.BATTLE_PASS);
        }
        player.displayClientMessage((excess >= amount
                ? Component.translatable("msg.roll_mod.battlepass.points.coins", coins, why)
                : Component.translatable("msg.roll_mod.battlepass.points", amount, why))
                .withStyle(ChatFormatting.GOLD), true);
        sync(player);
    }

    /** A daily task's reward was claimed: the daily's share of points. Called from {@code DailyTaskManager.claim}. */
    public static void onDailyClaimed(ServerPlayer player) {
        addPoints(player, BattlePassConfig.get().pointsDaily, Component.translatable("msg.roll_mod.battlepass.why.daily"));
    }

    /** The same per-event funnel the daily tasks use: counts towards this player's open battle-pass tasks. */
    public static void progress(ServerPlayer player, DailyTaskHook hook, Object subject, int amount) {
        BattlePassState state = BattlePassState.get(player.server);
        if (hooks == null) {
            Set<DailyTaskHook> set = EnumSet.noneOf(DailyTaskHook.class);
            for (BattlePassState.Batch batch : state.batches) {
                for (BattlePassState.Task task : batch.tasks()) {
                    DailyTask def = DailyTaskRegistry.byId(task.taskId());
                    if (def != null) set.add(def.hook());
                }
            }
            for (BattlePassState.Task task : state.seasonal) {
                DailyTask def = DailyTaskRegistry.byId(task.taskId());
                if (def != null) set.add(def.hook());
            }
            hooks = set;
        }
        if (!hooks.contains(hook)) return;
        BattlePassConfig config = BattlePassConfig.get();
        BattlePassState.Player data = state.player(player.getUUID());
        boolean prime = TeleportService.isPrime(player);
        boolean dirty = false;
        for (BattlePassState.Batch batch : state.batches) {
            for (int slot = 0; slot < batch.tasks().size(); slot++) {
                BattlePassState.Task task = batch.tasks().get(slot);
                if (task.kind() == BattlePassState.Kind.PRIME && !prime) continue;
                dirty |= count(player, config, data, batch.day() + ":" + slot, task, hook, subject, amount);
            }
        }
        // The season's challenges count the same way.
        for (int slot = 0; slot < state.seasonal.size(); slot++) {
            dirty |= count(player, config, data, "season:" + slot, state.seasonal.get(slot), hook, subject, amount);
        }
        if (dirty) state.setDirty();
    }

    /** One task's step for an event: counts it if it matches, and pays the points when it completes. */
    private static boolean count(ServerPlayer player, BattlePassConfig config, BattlePassState.Player data, String key,
                                 BattlePassState.Task task, DailyTaskHook hook, Object subject, int amount) {
        if (data.done.contains(key)) return false;
        DailyTask def = DailyTaskRegistry.byId(task.taskId());
        if (def == null || def.hook() != hook || !def.matches(subject) || !def.allowedFor(player)) return false;
        int now = Math.min(task.required(), data.progress.getOrDefault(key, 0) + amount);
        data.progress.put(key, now);
        if (now >= task.required()) {
            data.done.add(key);
            int points = pointsFor(config, task.kind());
            player.sendSystemMessage(Component.translatable("msg.roll_mod.battlepass.task_done",
                    def.name(), points).withStyle(ChatFormatting.GOLD));
            data.points += points;
            sync(player);
        }
        return true;
    }

    /* ---------------------------------------------------- levels --------------------------------------------------- */

    public static int freeLevel(long points) {
        return (int) Math.min(BattlePassConfig.FREE_LEVELS, points / Math.max(1, BattlePassConfig.get().pointsPerLevel));
    }

    public static int primeLevel(long points) {
        return (int) Math.min(BattlePassConfig.PRIME_LEVELS, points / Math.max(1, BattlePassConfig.get().pointsPerPrimeLevel));
    }

    /** The furthest quest stage {@code player} has opened in a row: 0 before the bronze quest, up to 7 (IV). */
    public static int stage(ServerPlayer player) {
        int stage = 0;
        for (int b = 1; b < BattlePassConfig.BLOCKS; b++) {
            if (!blockOpen(player, b)) break;
            stage = b;
        }
        return stage;
    }

    private static boolean blockOpen(ServerPlayer player, int block) {
        long quest = BattlePassConfig.get().gate(block);
        return quest == 0 || QuestGate.isCompleted(player, quest);
    }

    /** Claims {@code level} on the free ({@code prime = false}) or Prime track, if reached, open and not yet taken. */
    public static void claim(ServerPlayer player, boolean prime, int level) {
        claim(player, prime, level, -1);
    }

    /**
     * As above; {@code pick} is which option of a choice reward the player took ({@code -1} for a reward that is not a
     * choice — a choice claimed without a valid pick is refused, so nothing is taken).
     */
    public static void claim(ServerPlayer player, boolean prime, int level, int pick) {
        BattlePassConfig config = BattlePassConfig.get();
        BattlePassState state = BattlePassState.get(player.server);
        BattlePassState.Player data = state.player(player.getUUID());
        int max = prime ? BattlePassConfig.PRIME_LEVELS : BattlePassConfig.FREE_LEVELS;
        if (level < 1 || level > max) return;
        if (prime && !TeleportService.isPrime(player)) return;
        int reached = prime ? primeLevel(data.points) : freeLevel(data.points);
        if (level > reached) return;
        if (prime ? data.primeClaimed(level) : data.freeClaimed(level)) return;
        int block = (level - 1) / (prime ? BattlePassConfig.PRIME_PER_BLOCK : BattlePassConfig.FREE_PER_BLOCK);
        if (!blockOpen(player, block)) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.battlepass.gated").withStyle(ChatFormatting.RED));
            return;
        }
        String spec = (prime ? config.prime : config.free).get(level - 1);
        if (BattlePassReward.kind(spec) == BattlePassReward.Kind.CHOICE) {
            List<String> options = BattlePassReward.options(spec);
            if (pick < 0 || pick >= options.size()) return;
            spec = options.get(pick);
        }
        if (prime) data.claimPrime(level);
        else data.claimFree(level);
        state.setDirty();
        grant(player, spec);
        sync(player);
    }

    /** Pays a reward written as {@code "item*count"} or {@code "money:amount"}. */
    private static void grant(ServerPlayer player, String spec) {
        if (spec == null || spec.isBlank()) return;
        if (BattlePassReward.randomSkin(spec)) {
            com.roll_54.roll_mod.cosmetics.SkinRoll.roll(player, null);
            return;
        }
        if (spec.startsWith(BattlePassReward.SKIN)) {
            com.roll_54.roll_mod.cosmetics.ItemSkinDefinition skin = BattlePassReward.skin(spec);
            if (skin == null) return;
            boolean fresh = com.roll_54.roll_mod.cosmetics.ItemSkinService.unlock(player, skin.skinId());
            player.sendSystemMessage(Component.translatable(fresh ? "msg.roll_mod.battlepass.skin"
                    : "msg.roll_mod.battlepass.skin_owned", skin.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (spec.startsWith("money:")) {
            long amount;
            try {
                amount = Long.parseLong(spec.substring(6).strip());
            } catch (NumberFormatException e) {
                return;
            }
            CurrencyLedger.logged(CurrencyService.deposit(player, CurrencyType.MAIN, amount, player.server,
                            CurrencyCause.player(player, LedgerReason.BATTLE_PASS).withDetail("battle pass reward")),
                    player.getUUID(), CurrencyType.MAIN, amount, LedgerReason.BATTLE_PASS);
            return;
        }
        ItemStack stack = stack(spec);
        if (!stack.isEmpty()) DailyTaskRewards.give(player, stack);
    }

    /** The item a reward spec names, or empty for money or an unknown id. */
    public static ItemStack stack(String spec) {
        if (spec == null || spec.startsWith("money:")) return ItemStack.EMPTY;
        int star = spec.lastIndexOf('*');
        String id = star < 0 ? spec : spec.substring(0, star);
        int count = 1;
        if (star >= 0) {
            try {
                count = Integer.parseInt(spec.substring(star + 1).strip());
            } catch (NumberFormatException ignored) {
            }
        }
        ResourceLocation rl = ResourceLocation.tryParse(id.strip());
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.get(rl), Math.max(1, count));
    }

    /* ----------------------------------------------------- sync ---------------------------------------------------- */

    public static void sync(ServerPlayer player) {
        checkQuests(player);
        BattlePassConfig config = BattlePassConfig.get();
        BattlePassState state = BattlePassState.get(player.server);
        BattlePassState.Player data = state.player(player.getUUID());
        boolean prime = TeleportService.isPrime(player);
        CompoundTag tag = new CompoundTag();
        tag.putInt("season", config.season);
        tag.putLong("points", data.points);
        tag.putInt("ppl", config.pointsPerLevel);
        tag.putInt("pplPrime", config.pointsPerPrimeLevel);
        tag.putInt("pDaily", config.pointsDaily);
        tag.putInt("pEasy", config.pointsEasy);
        tag.putInt("pHard", config.pointsHard);
        tag.putInt("pPrime", config.pointsPrimeTask);
        tag.putLongArray("claimedFree", data.claimedFree);
        tag.putLong("claimedPrime", data.claimedPrime);
        tag.putBoolean("prime", prime);
        int open = 0;
        for (int b = 0; b < BattlePassConfig.BLOCKS; b++) if (blockOpen(player, b)) open |= 1 << b;
        tag.putInt("open", open);
        // Each block's quest, so the track can name it on the block's gate.
        ListTag gateList = new ListTag();
        // The quests that actually open each block (the stage table's, where it has one).
        for (int b = 0; b < BattlePassConfig.BLOCKS; b++) {
            long quest = config.gate(b);
            gateList.add(StringTag.valueOf(quest == 0 ? "" : String.format("%016X", quest)));
        }
        tag.put("gates", gateList);
        ListTag free = new ListTag();
        config.free.forEach(s -> free.add(StringTag.valueOf(s)));
        tag.put("free", free);
        ListTag primeList = new ListTag();
        config.prime.forEach(s -> primeList.add(StringTag.valueOf(s)));
        tag.put("primeRewards", primeList);
        ListTag tasks = new ListTag();
        List<BattlePassState.Batch> batches = new ArrayList<>(state.batches);
        batches.sort((a, b) -> Long.compare(b.day(), a.day()));
        for (BattlePassState.Batch batch : batches) {
            for (int slot = 0; slot < batch.tasks().size(); slot++) {
                BattlePassState.Task task = batch.tasks().get(slot);
                if (task.kind() == BattlePassState.Kind.PRIME && !prime) continue;
                String key = batch.day() + ":" + slot;
                CompoundTag t = new CompoundTag();
                t.putLong("day", batch.day());
                t.putInt("task", DailyTaskRegistry.indexOf(task.taskId()));
                t.putString("quest", task.taskId().startsWith("quest:") ? task.taskId().substring(6) : "");
                t.putString("kind", task.kind().name());
                t.putInt("required", task.required());
                t.putInt("progress", data.progress.getOrDefault(key, 0));
                t.putBoolean("done", data.done.contains(key));
                tasks.add(t);
            }
        }
        tag.put("tasks", tasks);
        ListTag seasonal = new ListTag();
        for (int slot = 0; slot < state.seasonal.size(); slot++) {
            BattlePassState.Task task = state.seasonal.get(slot);
            String key = "season:" + slot;
            CompoundTag t = new CompoundTag();
            t.putInt("task", DailyTaskRegistry.indexOf(task.taskId()));
            t.putString("quest", task.taskId().startsWith("quest:") ? task.taskId().substring(6) : "");
            t.putString("kind", "SEASON");
            t.putInt("required", task.required());
            t.putInt("progress", data.progress.getOrDefault(key, 0));
            t.putBoolean("done", data.done.contains(key));
            seasonal.add(t);
        }
        tag.put("seasonal", seasonal);
        tag.putInt("pSeason", config.pointsSeasonal);
        PacketDistributor.sendToPlayer(player, new BattlePassPackets.Data(tag));
    }

    /* ---------------------------------------------------- admin ---------------------------------------------------- */

    private static final String[] NUMBERS = {"pointsPerLevel", "pointsPerPrimeLevel", "pointsDaily", "pointsEasy",
            "pointsHard", "pointsPrimeTask", "easyMultiplier", "hardMultiplier"};

    private static int number(BattlePassConfig c, String key) {
        return switch (key) {
            case "pointsPerLevel" -> c.pointsPerLevel;
            case "pointsPerPrimeLevel" -> c.pointsPerPrimeLevel;
            case "pointsDaily" -> c.pointsDaily;
            case "pointsEasy" -> c.pointsEasy;
            case "pointsHard" -> c.pointsHard;
            case "pointsPrimeTask" -> c.pointsPrimeTask;
            case "easyMultiplier" -> c.easyMultiplier;
            case "hardMultiplier" -> c.hardMultiplier;
            default -> 0;
        };
    }

    private static void setNumber(BattlePassConfig c, String key, int v) {
        switch (key) {
            case "pointsPerLevel" -> c.pointsPerLevel = Math.max(1, v);
            case "pointsPerPrimeLevel" -> c.pointsPerPrimeLevel = Math.max(1, v);
            case "pointsDaily" -> c.pointsDaily = Math.max(0, v);
            case "pointsEasy" -> c.pointsEasy = Math.max(0, v);
            case "pointsHard" -> c.pointsHard = Math.max(0, v);
            case "pointsPrimeTask" -> c.pointsPrimeTask = Math.max(0, v);
            case "easyMultiplier" -> c.easyMultiplier = Math.max(1, v);
            case "hardMultiplier" -> c.hardMultiplier = Math.max(1, v);
            default -> { }
        }
    }

    /** The settings, for the Moderation tab's battle pass editor. Those who may ban only. */
    public static void sendAdmin(ServerPlayer player) {
        if (!com.roll_54.roll_mod.minestar.moderation.ModerationPermissions.canBan(player)) return;
        BattlePassConfig config = BattlePassConfig.get();
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("admin", true);
        tag.putInt("season", config.season);
        CompoundTag numbers = new CompoundTag();
        for (String key : NUMBERS) numbers.putInt(key, number(config, key));
        tag.put("numbers", numbers);
        ListTag gates = new ListTag();
        config.gates.forEach(g -> gates.add(StringTag.valueOf(g == null ? "" : g)));
        tag.put("gates", gates);
        // Which blocks the quest stage table decides instead (their quest shown, not editable here).
        ListTag fromTable = new ListTag();
        for (int b = 0; b < BattlePassConfig.BLOCKS; b++) {
            long quest = config.gate(b);
            fromTable.add(StringTag.valueOf(quest != config.ownGate(b) ? String.format("%016X", quest) : ""));
        }
        tag.put("tableGates", fromTable);
        ListTag free = new ListTag();
        config.free.forEach(r -> free.add(StringTag.valueOf(r)));
        tag.put("free", free);
        ListTag prime = new ListTag();
        config.prime.forEach(r -> prime.add(StringTag.valueOf(r)));
        tag.put("prime", prime);
        PacketDistributor.sendToPlayer(player, new BattlePassPackets.Data(tag));
    }

    /** Saves what the editor sent — numbers, block quests, rewards — and refreshes everyone online. */
    public static void applyAdmin(ServerPlayer player, CompoundTag args) {
        if (!com.roll_54.roll_mod.minestar.moderation.ModerationPermissions.canBan(player)) return;
        BattlePassConfig config = BattlePassConfig.get();
        CompoundTag numbers = args.getCompound("numbers");
        for (String key : NUMBERS) if (numbers.contains(key)) setNumber(config, key, numbers.getInt(key));
        ListTag gates = args.getList("gates", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < Math.min(gates.size(), BattlePassConfig.BLOCKS); i++) config.gates.set(i, gates.getString(i).strip());
        ListTag free = args.getList("free", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < Math.min(free.size(), BattlePassConfig.FREE_LEVELS); i++) config.free.set(i, free.getString(i).strip());
        ListTag prime = args.getList("prime", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < Math.min(prime.size(), BattlePassConfig.PRIME_LEVELS); i++) config.prime.set(i, prime.getString(i).strip());
        config.save();
        // Live: everyone's pass is rebuilt from the saved settings at once.
        applyChanges(player.server);
        sendAdmin(player);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.battlepass.saved").withStyle(ChatFormatting.GREEN));
    }

    /* ---------------------------------------------------- events --------------------------------------------------- */

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        hooks = null;
        rollIfNeeded(player.server);
        sync(player);
        com.roll_54.roll_mod.minestar.dailytasks.DailyRewardConfig.sync(player);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ++tick;
        // battlepass.json edited by hand: picked up within two seconds, no command needed.
        if (tick % 40 == 0 && BattlePassConfig.reloadIfEdited()) applyChanges(event.getServer());
        if (tick % 1200 != 0) return;
        MinecraftServer server = event.getServer();
        // Quest tasks finish when their quest does: look once a minute.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BattlePassState.Player data = BattlePassState.get(server).player(player.getUUID());
            long before = data.points;
            checkQuests(player);
            if (data.points != before) sync(player);
        }
        if (rollIfNeeded(server)) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.translatable("msg.roll_mod.battlepass.new_tasks")
                        .withStyle(ChatFormatting.AQUA));
                sync(player);
            }
        }
    }

    /**
     * Rerolls the battle pass's weekly tasks (the latest batch) or the season's tasks for everyone: new ones drawn, their
     * progress cleared. Returns false when there was nothing to reroll.
     */
    public static boolean reroll(MinecraftServer server, boolean seasonal) {
        BattlePassConfig config = BattlePassConfig.get();
        BattlePassState state = BattlePassState.get(server);
        rollIfNeeded(server);
        String prefix;
        if (seasonal) {
            state.seasonal.clear();
            seasonSalt = new Random().nextLong() | 1;
            prefix = "season:";
        } else {
            BattlePassState.Batch latest = null;
            for (BattlePassState.Batch b : state.batches) if (latest == null || b.day() > latest.day()) latest = b;
            if (latest == null) return false;
            BattlePassState.Batch fresh = deal(config, latest.day(), new Random().nextLong() | 1);
            if (fresh == null) return false;
            state.batches.set(state.batches.indexOf(latest), fresh);
            prefix = latest.day() + ":";
        }
        for (BattlePassState.Player p : state.players.values()) {
            p.progress.keySet().removeIf(k -> k.startsWith(prefix));
            p.done.removeIf(k -> k.startsWith(prefix));
        }
        state.setDirty();
        applyChanges(server);
        return true;
    }

    /** After the settings changed: the task hooks recomputed, any due batch dealt, and every player online re-sent. */
    public static void applyChanges(MinecraftServer server) {
        hooks = null;
        rollIfNeeded(server);
        server.getPlayerList().getPlayers().forEach(BattlePassService::sync);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        // /rollmod admin battlepass … — gated on "battlepass", since several classes register "rollmod admin" and
        // Brigadier keeps only the first one's requirement.
        event.getDispatcher().register(Commands.literal("rollmod").then(Commands.literal("admin").then(Commands.literal("battlepass")
                .requires(src -> src.hasPermission(2))
                // Event points: /rollmod admin battlepass points <players> <amount>
                .then(Commands.literal("points")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            var targets = EntityArgument.getPlayers(ctx, "targets");
                                            for (ServerPlayer p : targets) {
                                                addPoints(p, amount, Component.translatable("msg.roll_mod.battlepass.why.event"));
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "msg.roll_mod.battlepass.given", amount, targets.size()), true);
                                            return targets.size();
                                        }))))
                // Every level reached on both tracks (claiming still waits for each block's quest).
                .then(Commands.literal("complete")
                        .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> {
                            var targets = EntityArgument.getPlayers(ctx, "targets");
                            BattlePassConfig config = BattlePassConfig.get();
                            long full = Math.max((long) BattlePassConfig.FREE_LEVELS * config.pointsPerLevel,
                                    (long) BattlePassConfig.PRIME_LEVELS * config.pointsPerPrimeLevel);
                            BattlePassState state = BattlePassState.get(ctx.getSource().getServer());
                            for (ServerPlayer p : targets) {
                                state.player(p.getUUID()).points = Math.max(state.player(p.getUUID()).points, full);
                                sync(p);
                            }
                            state.setDirty();
                            ctx.getSource().sendSuccess(() -> Component.translatable("msg.roll_mod.battlepass.completed",
                                    targets.size()), true);
                            return targets.size();
                        })))
                // Back to nothing: points, claimed rewards and task progress this season.
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> {
                            var targets = EntityArgument.getPlayers(ctx, "targets");
                            BattlePassState state = BattlePassState.get(ctx.getSource().getServer());
                            for (ServerPlayer p : targets) {
                                state.players.remove(p.getUUID());
                                sync(p);
                            }
                            state.setDirty();
                            ctx.getSource().sendSuccess(() -> Component.translatable("msg.roll_mod.battlepass.reset",
                                    targets.size()), true);
                            return targets.size();
                        })))
                // An exact number of points.
                .then(Commands.literal("setpoints")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> {
                                    var targets = EntityArgument.getPlayers(ctx, "targets");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    BattlePassState state = BattlePassState.get(ctx.getSource().getServer());
                                    for (ServerPlayer p : targets) {
                                        state.player(p.getUUID()).points = amount;
                                        sync(p);
                                    }
                                    state.setDirty();
                                    ctx.getSource().sendSuccess(() -> Component.translatable("msg.roll_mod.battlepass.set",
                                            amount, targets.size()), true);
                                    return targets.size();
                                }))))
                // New tasks for everyone: the dailies (every group), this half-week's battle-pass tasks, or the season's.
                .then(Commands.literal("reroll")
                        .then(Commands.literal("daily").executes(ctx -> {
                            DailyTaskManager.forceRoll(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.translatable("msg.roll_mod.battlepass.reroll.daily"), true);
                            return 1;
                        }))
                        .then(Commands.literal("weekly").executes(ctx -> {
                            boolean done = reroll(ctx.getSource().getServer(), false);
                            if (done) ctx.getSource().sendSuccess(() -> Component.translatable("msg.roll_mod.battlepass.reroll.weekly"), true);
                            else ctx.getSource().sendFailure(Component.translatable("msg.roll_mod.battlepass.reroll.none"));
                            return done ? 1 : 0;
                        }))
                        .then(Commands.literal("seasonal").executes(ctx -> {
                            reroll(ctx.getSource().getServer(), true);
                            ctx.getSource().sendSuccess(() -> Component.translatable("msg.roll_mod.battlepass.reroll.seasonal"), true);
                            return 1;
                        })))
                // A new season: everyone back to zero, a first batch of tasks today.
                .then(Commands.literal("newseason").executes(ctx -> {
                    BattlePassConfig config = BattlePassConfig.get();
                    config.season++;
                    config.seasonStartDay = DailyTaskManager.currentPeriodDay();
                    config.save();
                    MinecraftServer server = ctx.getSource().getServer();
                    rollIfNeeded(server);
                    server.getPlayerList().getPlayers().forEach(BattlePassService::sync);
                    ctx.getSource().sendSuccess(() -> Component.translatable("msg.roll_mod.battlepass.season",
                            config.season), true);
                    return 1;
                }))
                .then(Commands.literal("reload").executes(ctx -> {
                    BattlePassConfig.reload();
                    applyChanges(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("battlepass.json reloaded"), true);
                    return 1;
                })))));
    }
}
