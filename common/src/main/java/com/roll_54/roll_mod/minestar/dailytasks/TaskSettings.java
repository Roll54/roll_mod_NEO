package com.roll_54.roll_mod.minestar.dailytasks;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassConfig;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskQuota;
import com.roll_54.roll_mod.minestar.dailytasks.tasks.ConfiguredTask;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import com.roll_54.roll_mod.network.packet.BattlePassPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Moderation tab's task settings ({@code minestar/task_settings.json}): how many main daily tasks a player is
 * dealt, what a battle-pass cup is worth in starcoins once the pass is full, which code tasks leave the daily draw,
 * the amount each task asks for, and the tasks made in the editor ({@link ConfiguredTask}).
 *
 * <p>The amounts and the made tasks change the task registry, so they are synced: the server applies them at start
 * and after each save, and sends them to every client on login and on change; the client applies the same.
 *
 * <p>The cups each kind of task pays and the weekly and seasonal pools stay in {@link BattlePassConfig}; the editor
 * reads and writes them through here as well.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class TaskSettings {

    public static final int MIN_DAILY = 1;
    public static final long DEFAULT_STARCOINS_PER_CUP = 10;

    /** One task made in the editor. */
    public static final class Made {
        public String id = "";
        public String hook = DailyTaskHook.MINE.name();
        public String target = "";
        public int amount = 10;
        public int weight = 1;
        public String title = "";
        public String icon = "";
        /** The description; {@code %s} the amount, {@code %2$s} the target of the day. Blank: a generic one. */
        public String desc = "";
        /** Whether a party is asked for more than one player. */
        public boolean scales = true;
        /** A party's growth per member past the first ({@code base * m^(members-1)}); 0: {@code base * (members+1)}. */
        public double teamMultiplier = 0;
        /** A mod the task needs; without it the task is left out. */
        public String requiredMod = "";
        /** A battle-pass challenge: weekly or seasonal only, never on the daily board. */
        public boolean passOnly = false;
        /** Only one of the targets counts each day (the same for everyone). */
        public boolean oneADay = false;
        /* Where and how the player must be for it to count; blank: anywhere ({@code ConfiguredTask.Conditions}). */
        public String dimension = "";
        public String biome = "";
        public String held = "";
        /** "", "day" or "night". */
        public String time = "";
        /** "", "clear", "rain" or "thunder". */
        public String weather = "";
        /** A Y range, blank for open. */
        public String minY = "";
        public String maxY = "";
        public boolean sneaking = false;

        public Made() {}

        ConfiguredTask task() {
            DailyTaskHook h;
            try {
                h = DailyTaskHook.valueOf(hook.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                h = DailyTaskHook.MINE;
            }
            return new ConfiguredTask(id, h, target, amount, weight, title, desc, icon, scales, teamMultiplier,
                    requiredMod, passOnly, oneADay).withConditions(new ConfiguredTask.Conditions(nz(dimension),
                    nz(biome), nz(held), nz(time), nz(weather), y(minY, Integer.MIN_VALUE), y(maxY, Integer.MAX_VALUE),
                    sneaking));
        }

        private static String nz(String s) {
            return s == null ? "" : s.strip();
        }

        private static int y(String s, int open) {
            try {
                return s == null || s.isBlank() ? open : Integer.parseInt(s.strip());
            } catch (NumberFormatException e) {
                return open;
            }
        }

        public CompoundTag toTag() {
            CompoundTag t = new CompoundTag();
            t.putString("id", id);
            t.putString("hook", hook);
            t.putString("target", target);
            t.putInt("amount", amount);
            t.putInt("weight", weight);
            t.putString("title", title);
            t.putString("icon", icon);
            t.putString("desc", desc == null ? "" : desc);
            t.putBoolean("scales", scales);
            t.putDouble("teamMultiplier", teamMultiplier);
            t.putString("requiredMod", requiredMod == null ? "" : requiredMod);
            t.putBoolean("passOnly", passOnly);
            t.putBoolean("oneADay", oneADay);
            t.putString("dimension", nz(dimension));
            t.putString("biome", nz(biome));
            t.putString("held", nz(held));
            t.putString("time", nz(time));
            t.putString("weather", nz(weather));
            t.putString("minY", nz(minY));
            t.putString("maxY", nz(maxY));
            t.putBoolean("sneaking", sneaking);
            return t;
        }

        public static Made of(CompoundTag t) {
            Made m = new Made();
            m.id = t.getString("id");
            m.hook = t.getString("hook");
            m.target = t.getString("target");
            m.amount = t.getInt("amount");
            m.weight = t.getInt("weight");
            m.title = t.getString("title");
            m.icon = t.getString("icon");
            m.desc = t.getString("desc");
            m.scales = !t.contains("scales") || t.getBoolean("scales");
            m.teamMultiplier = t.getDouble("teamMultiplier");
            m.requiredMod = t.getString("requiredMod");
            m.passOnly = t.getBoolean("passOnly");
            m.oneADay = t.getBoolean("oneADay");
            m.dimension = t.getString("dimension");
            m.biome = t.getString("biome");
            m.held = t.getString("held");
            m.time = t.getString("time");
            m.weather = t.getString("weather");
            m.minY = t.getString("minY");
            m.maxY = t.getString("maxY");
            m.sneaking = t.getBoolean("sneaking");
            return m;
        }
    }

    /** How many main daily tasks a player is dealt (before earned extra slots and the Prime slot). */
    public int dailyCount = DailyTaskQuota.DEFAULT;
    /** Starcoins per battle-pass cup once the pass's last level is reached. */
    public long starcoinsPerCup = DEFAULT_STARCOINS_PER_CUP;
    /** Code tasks left out of the daily draw (still usable for weeklies and seasonals). */
    public List<String> disabledDaily = new ArrayList<>();
    /** Per task id, the amount it asks for instead of its own. */
    public Map<String, Integer> amounts = new LinkedHashMap<>();
    /** Per code task id, another team multiplier: 0 the usual (+1 per member), 1 none, more compounds per member. */
    public Map<String, Double> multipliers = new LinkedHashMap<>();
    public List<Made> made = new ArrayList<>();

    private static TaskSettings current;

    private static Path file() {
        return MinestarFiles.resolve("task_settings.json");
    }

    public static synchronized TaskSettings get() {
        if (current == null) current = load();
        return current;
    }

    private static TaskSettings load() {
        JsonObject json = MinestarFiles.readObject(file());
        TaskSettings settings = json.size() == 0 ? null : MinestarFiles.GSON.fromJson(json, TaskSettings.class);
        if (settings == null) settings = new TaskSettings();
        if (settings.disabledDaily == null) settings.disabledDaily = new ArrayList<>();
        if (settings.amounts == null) settings.amounts = new LinkedHashMap<>();
        if (settings.multipliers == null) settings.multipliers = new LinkedHashMap<>();
        if (settings.made == null) settings.made = new ArrayList<>();
        for (Made m : settings.made) {
            if (m.desc == null) m.desc = "";
            if (m.requiredMod == null) m.requiredMod = "";
            if (m.dimension == null) m.dimension = "";
            if (m.biome == null) m.biome = "";
            if (m.held == null) m.held = "";
            if (m.time == null) m.time = "";
            if (m.weather == null) m.weather = "";
            if (m.minY == null) m.minY = "";
            if (m.maxY == null) m.maxY = "";
        }
        settings.save();
        return settings;
    }

    public synchronized void save() {
        MinestarFiles.write(file(), MinestarFiles.GSON.toJsonTree(this));
    }

    /** The settings into the registry and the daily quota. */
    void apply() {
        List<ConfiguredTask> tasks = new ArrayList<>();
        for (Made m : made) tasks.add(m.task());
        DailyTaskRegistry.applySettings(amounts, multipliers, tasks);
    }

    public static int clampDaily(int count) {
        return Math.max(MIN_DAILY, Math.min(DailyTasksState.MAX_TASK_COUNT - 1, count));
    }

    /** Whether a code task is left out of the daily draw. Server side. */
    public static boolean dailyDisabled(String id) {
        return get().disabledDaily.contains(id);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        current = load();
        current.apply();
        DailyTaskQuota.setProvider(player -> clampDaily(get().dailyCount));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    /* ------------------------------------------------------- sync ------------------------------------------------------- */

    /** The settings and the battle pass's task numbers and pools, as the editor and the clients need them. */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("taskSettings", true);
        tag.putInt("dailyCount", dailyCount);
        tag.putLong("starcoinsPerCup", starcoinsPerCup);
        tag.put("disabledDaily", strings(disabledDaily));
        CompoundTag amountTag = new CompoundTag();
        amounts.forEach(amountTag::putInt);
        tag.put("amounts", amountTag);
        CompoundTag multTag = new CompoundTag();
        multipliers.forEach(multTag::putDouble);
        tag.put("multipliers", multTag);
        ListTag madeTag = new ListTag();
        for (Made m : made) madeTag.add(m.toTag());
        tag.put("made", madeTag);
        BattlePassConfig bp = BattlePassConfig.get();
        CompoundTag cups = new CompoundTag();
        for (String key : CUP_KEYS) cups.putInt(key, cup(bp, key));
        tag.put("cups", cups);
        tag.putInt("seasonalCount", bp.seasonalCount);
        tag.putInt("weeklyEasyCount", bp.weeklyEasyCount);
        tag.putInt("weeklyHardCount", bp.weeklyHardCount);
        tag.putInt("weeklyPrimeCount", bp.weeklyPrimeCount);
        tag.put("weeklyDailies", strings(bp.weeklyDailies));
        tag.put("weeklyChallenges", strings(bp.weeklyChallenges));
        tag.put("seasonalChallenges", strings(bp.seasonalChallenges));
        return tag;
    }

    /** The cups each kind of task pays, in {@link BattlePassConfig}. */
    public static final String[] CUP_KEYS = {"pointsDaily", "pointsEasy", "pointsHard", "pointsPrimeTask", "pointsSeasonal"};

    private static int cup(BattlePassConfig bp, String key) {
        return switch (key) {
            case "pointsDaily" -> bp.pointsDaily;
            case "pointsEasy" -> bp.pointsEasy;
            case "pointsHard" -> bp.pointsHard;
            case "pointsPrimeTask" -> bp.pointsPrimeTask;
            case "pointsSeasonal" -> bp.pointsSeasonal;
            default -> 0;
        };
    }

    private static void setCup(BattlePassConfig bp, String key, int value) {
        int v = Math.max(0, value);
        switch (key) {
            case "pointsDaily" -> bp.pointsDaily = v;
            case "pointsEasy" -> bp.pointsEasy = v;
            case "pointsHard" -> bp.pointsHard = v;
            case "pointsPrimeTask" -> bp.pointsPrimeTask = v;
            case "pointsSeasonal" -> bp.pointsSeasonal = v;
            default -> {
            }
        }
    }

    /** A client's copy: the registry gets the same amounts and made tasks as the server's. */
    public static void applyTag(CompoundTag tag) {
        Map<String, Integer> amounts = amounts(tag);
        List<ConfiguredTask> tasks = new ArrayList<>();
        for (Made m : made(tag)) tasks.add(m.task());
        DailyTaskRegistry.applySettings(amounts, multipliers(tag), tasks);
    }

    public static Map<String, Double> multipliers(CompoundTag tag) {
        Map<String, Double> out = new HashMap<>();
        CompoundTag a = tag.getCompound("multipliers");
        for (String key : a.getAllKeys()) out.put(key, a.getDouble(key));
        return out;
    }

    public static Map<String, Integer> amounts(CompoundTag tag) {
        Map<String, Integer> out = new HashMap<>();
        CompoundTag a = tag.getCompound("amounts");
        for (String key : a.getAllKeys()) out.put(key, a.getInt(key));
        return out;
    }

    public static List<Made> made(CompoundTag tag) {
        List<Made> out = new ArrayList<>();
        ListTag list = tag.getList("made", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) out.add(Made.of(list.getCompound(i)));
        return out;
    }

    public static List<String> strings(CompoundTag tag, String key) {
        List<String> out = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    private static ListTag strings(List<String> values) {
        ListTag list = new ListTag();
        for (String v : values) list.add(StringTag.valueOf(v));
        return list;
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new BattlePassPackets.Data(get().toTag()));
    }

    /**
     * From the Moderation tab: replaces the settings, the cups and the weekly and seasonal pools, saves both files,
     * applies the registry, and sends the result to everyone online.
     */
    public static void applyAdmin(ServerPlayer player, CompoundTag args) {
        if (!com.roll_54.roll_mod.minestar.moderation.ModerationPermissions.canBan(player)) return;
        TaskSettings settings = get();
        settings.dailyCount = clampDaily(args.getInt("dailyCount"));
        settings.starcoinsPerCup = Math.max(0, args.getLong("starcoinsPerCup"));
        settings.disabledDaily = new ArrayList<>(strings(args, "disabledDaily"));
        settings.amounts = new LinkedHashMap<>();
        amounts(args).forEach((id, amount) -> {
            if (amount > 0) settings.amounts.put(id, amount);
        });
        settings.multipliers = new LinkedHashMap<>();
        multipliers(args).forEach((id, mult) -> {
            if (mult >= 0 && mult <= 100) settings.multipliers.put(id, mult);
        });
        List<Made> made = new ArrayList<>();
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (DailyTask task : DailyTaskRegistry.scanned()) ids.add(task.id());
        for (Made m : made(args)) {
            // Made tasks live in their own namespace of ids, never one a code task already has.
            if (!m.id.matches("custom_[a-z0-9_]+") || !ids.add(m.id)) continue;
            m.amount = Math.max(1, m.amount);
            m.weight = Math.max(1, m.weight);
            m.teamMultiplier = Math.max(0, m.teamMultiplier);
            made.add(m);
        }
        settings.made = made;
        settings.save();
        settings.apply();

        BattlePassConfig bp = BattlePassConfig.get();
        CompoundTag cups = args.getCompound("cups");
        for (String key : CUP_KEYS) if (cups.contains(key)) setCup(bp, key, cups.getInt(key));
        bp.seasonalCount = Math.max(0, args.getInt("seasonalCount"));
        bp.weeklyEasyCount = Math.max(0, args.getInt("weeklyEasyCount"));
        bp.weeklyHardCount = Math.max(0, args.getInt("weeklyHardCount"));
        bp.weeklyPrimeCount = Math.max(0, args.getInt("weeklyPrimeCount"));
        bp.weeklyDailies = new ArrayList<>(strings(args, "weeklyDailies"));
        bp.weeklyChallenges = new ArrayList<>(strings(args, "weeklyChallenges"));
        bp.seasonalChallenges = new ArrayList<>(strings(args, "seasonalChallenges"));
        bp.save();

        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) sync(p);
    }

    private TaskSettings() {}

    /** Leaving a server: the registry goes back to the code tasks, so the next server's settings start clean. */
    @EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
    static final class ClientReset {
        @SubscribeEvent
        static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            DailyTaskRegistry.applySettings(Map.of(), List.of());
        }
    }
}
