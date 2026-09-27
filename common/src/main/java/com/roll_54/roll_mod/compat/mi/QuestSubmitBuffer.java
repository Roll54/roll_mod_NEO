package com.roll_54.roll_mod.compat.mi;

import aztech.modern_industrialization.compat.ftbquests.FTBQuestsFacade;
import com.roll_54.roll_mod.RollMod;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Batches Modern Industrialization's "player produced an item" notifications to FTB Quests.
 *
 * <p>MI calls {@code PlayerStatistics.addProducedItems} for every machine output, every tick, and
 * from there straight into {@code FTBQuestsFacade.INSTANCE.addCompleted(uuid, item, amount)}. That
 * is not a cheap notification: for each call FTB resolves the player's team, then walks
 * {@code ServerQuestFile.getSubmitTasks()} and, per task, re-runs {@code TeamData.canStartTasks}
 * (itself recursive over quest dependencies) and {@code ItemTask.test} — which goes through
 * {@code ItemMatchingSystem.doesItemMatch} and a {@code Stream.findFirst} to pick a filter adapter.
 * All of it to add a number to a counter.
 *
 * <p>An hour of server profiling put the chain at 35.5s of tick time, ~0.5 ms/tick, entirely in
 * re-derivation. Every bit of that work is identical across the calls within a tick.
 *
 * <p>So the calls are accumulated per (player, item) and flushed once every
 * {@value #FLUSH_INTERVAL_TICKS} ticks. That is sound because the only thing
 * {@code addCompleted} ultimately does is {@code TeamData.addProgress(task, amount)}, which is
 * additive: N calls of 1 and one call of N leave the same progress, and FTB clamps progress at the
 * task's target either way.
 *
 * <p>The one visible difference is timing, and it is bounded by the interval: quest progress from
 * machine output lands within a second rather than the same tick. A quest that unlocks another
 * quest wanting the same item hands it the following batch instead of the same one — the item keeps
 * being produced, so it catches up on the next flush.
 *
 * <p>Buffering is skipped entirely when FTB Quests is absent; MI's own facade is a no-op stub then,
 * and letting it run is cheaper than remembering work nobody will do.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class QuestSubmitBuffer {

    /** One second. Long enough to collapse a tick's worth of outputs, short enough to feel live. */
    private static final int FLUSH_INTERVAL_TICKS = 20;

    private static final Map<UUID, Reference2LongMap<Item>> PENDING = new HashMap<>();

    private static int ticksSinceFlush;

    private QuestSubmitBuffer() {}

    /**
     * Records a production event instead of submitting it now.
     *
     * @return true when the caller should skip its own submit because this took ownership of it,
     *     false when it should proceed as normal.
     */
    public static boolean record(UUID uuid, Item item, long amount) {
        if (uuid == null || amount <= 0 || !ModList.get().isLoaded("ftbquests")) {
            return false;
        }
        PENDING.computeIfAbsent(uuid, ignored -> new Reference2LongOpenHashMap<>())
                .mergeLong(item, amount, Long::sum);
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++ticksSinceFlush < FLUSH_INTERVAL_TICKS) {
            return;
        }
        ticksSinceFlush = 0;
        flush();
    }

    /**
     * Flush before the server goes down, so a shutdown inside the interval does not drop the last
     * batch. Also drops any leftovers, because the statics outlive a single-player world.
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        flush();
        PENDING.clear();
        ticksSinceFlush = 0;
    }

    private static void flush() {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Reference2LongMap<Item>> perPlayer : PENDING.entrySet()) {
            UUID uuid = perPlayer.getKey();
            for (Reference2LongMap.Entry<Item> produced : perPlayer.getValue().reference2LongEntrySet()) {
                FTBQuestsFacade.INSTANCE.addCompleted(uuid, produced.getKey(), produced.getLongValue());
            }
        }
        PENDING.clear();
    }
}
