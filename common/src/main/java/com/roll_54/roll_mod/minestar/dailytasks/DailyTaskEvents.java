package com.roll_54.roll_mod.minestar.dailytasks;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.common.registry.ModItems;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.RMMAttachment;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Translates NeoForge game events into {@link DailyTaskManager#progress} calls, rolls a player's
 * group on login, and drives the 06:00 day turnover off the server tick.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class DailyTaskEvents {

    /** The day only turns over once, so checking the clock once a second is plenty. */
    private static final int ROLL_CHECK_INTERVAL_TICKS = 20;

    /** Playtime, distance and raids are read from vanilla statistics; once a second is fine-grained enough. */
    private static final int SAMPLE_INTERVAL_TICKS = 20;

    private static final int TICKS_PER_MINUTE = 20 * 60;
    private static final int CM_PER_BLOCK = 100;

    private DailyTaskEvents() {}

    /* ------------------------------------------ the 06:00 turnover -------------------------------------------- */

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Catch up immediately: the server may have been down across a 06:00 boundary.
        DailyTaskManager.rollIfNeeded(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % ROLL_CHECK_INTERVAL_TICKS == 0) {
            DailyTaskManager.rollIfNeeded(server);
        }
        DailyTaskStatus.tick(server);
        resolvePendingClips(server);
        if (server.getTickCount() % SAMPLE_INTERVAL_TICKS == 0) {
            sampleStats(server);
        }
    }

    /**
     * The "first join of the day" trigger: a player logging in is what normally draws their group's
     * set. Not load-bearing on its own — every other entry point rolls lazily too — but it means the
     * screen and the announcements are right from the moment they arrive, rather than after their
     * first swing.
     */
    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DailyTaskManager.rollGroupIfNeeded(player);
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SAMPLES.remove(event.getEntity().getUUID());
    }

    /* ---------------------------------------------------- MINE ------------------------------------------------ */

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        ServerPlayer player = serverPlayer(event.getPlayer());
        if (player == null) return;

        DailyTaskManager.progress(player, DailyTaskHook.MINE, event.getState(), 1);
    }

    /* ---------------------------------------------------- KILL ------------------------------------------------ */

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        ServerPlayer killer = serverPlayer(
                event.getSource().getEntity() instanceof Player p ? p : null);
        if (killer == null) return;

        LivingEntity victim = event.getEntity();
        if (victim == killer) return;
        DailyTaskManager.progress(killer, DailyTaskHook.KILL, victim, 1);
    }

    /* --------------------------------------------- CRAFT / SMELT ---------------------------------------------- */

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        ServerPlayer player = serverPlayer(event.getEntity() instanceof Player p ? p : null);
        if (player == null) return;
        ItemStack crafted = event.getCrafting();
        DailyTaskManager.progress(player, DailyTaskHook.CRAFT, crafted, crafted.getCount());
    }

    /*
     * SMELT and the cooking pot's COOK are not counted from player take-out events: those only fire
     * when a player pulls output by hand, so a hopper-fed furnace or a pot left to cook would never
     * count. AbstractFurnaceBlockEntityMixin and CookingPotBlockEntityMixin count each result as it is
     * produced instead, for the block's owner — set by the two listeners below.
     */

    /**
     * Farmer's Delight's cooking pot, matched by id so this class never loads an FD type. Absent the
     * mod, no block entity has this id and the check is simply never true.
     */
    private static final ResourceLocation COOKING_POT = ResourceLocation.fromNamespaceAndPath("farmersdelight", "cooking_pot");

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            claimStation(event.getLevel().getBlockEntity(event.getPos()), player);
        }
    }

    @SubscribeEvent
    public static void onStationOpened(PlayerInteractEvent.RightClickBlock event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player == null) return;
        claimStation(event.getLevel().getBlockEntity(event.getPos()), player);
    }

    private static boolean isStation(BlockEntity blockEntity) {
        return blockEntity instanceof AbstractFurnaceBlockEntity
                || COOKING_POT.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
    }

    /** The last player to place or open a furnace or cooking pot is the one it works for. */
    private static void claimStation(@Nullable BlockEntity blockEntity, ServerPlayer player) {
        if (blockEntity == null || !isStation(blockEntity)) return;
        if (player.getUUID().equals(blockEntity.getData(RMMAttachment.STATION_OWNER))) return;
        blockEntity.setData(RMMAttachment.STATION_OWNER, player.getUUID());
        blockEntity.setChanged();
    }

    /**
     * Credits {@code result} to the player a furnace or cooking pot works for. Called from the
     * production mixins; does nothing for a block nobody has claimed, or whose owner is offline.
     */
    public static void progressForOwner(BlockEntity station, DailyTaskHook hook, ItemStack result) {
        Level level = station.getLevel();
        if (level == null || level.getServer() == null || result.isEmpty()) return;

        UUID owner = station.getData(RMMAttachment.STATION_OWNER);
        if (RMMAttachment.NO_OWNER.equals(owner)) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return;

        DailyTaskManager.progress(player, hook, result, result.getCount());
    }

    /* ---------------------------------------------- FISH / BREED / EAT ---------------------------------------- */

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        ServerPlayer player = serverPlayer(event.getEntity() instanceof Player p ? p : null);
        if (player == null) return;
        for (ItemStack drop : event.getDrops()) {
            DailyTaskManager.progress(player, DailyTaskHook.FISH, drop, 1);
        }
    }

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        ServerPlayer player = serverPlayer(event.getCausedByPlayer());
        if (player == null || event.getParentA() == null) return;
        DailyTaskManager.progress(player, DailyTaskHook.BREED, event.getParentA(), 1);
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        ServerPlayer player = serverPlayer(event.getEntity() instanceof Player p ? p : null);
        if (player == null) return;

        ItemStack stack = event.getItem();
        if (stack.get(DataComponents.FOOD) == null) return; // drinking a potion is not a meal
        DailyTaskManager.progress(player, DailyTaskHook.EAT, stack, 1);
    }

    /* --------------------------------------------- CLIP (AgriCraft) ------------------------------------------- */

    /**
     * A clipper right-click on a planted crop, waiting to be confirmed once the block has acted.
     * {@code stageAtClick} is what the confirmation compares against.
     */
    private record PendingClip(UUID playerId, ResourceKey<Level> dimension, BlockPos pos,
                               String plantId, int stageAtClick) {}

    private static final List<PendingClip> PENDING_CLIPS = new ArrayList<>();

    /**
     * AgriCraft publishes no event when a crop is clipped, so the right-click is only a candidate:
     * it may be cancelled, or refused because the crop is not ripe enough. Runs at {@code LOWEST}
     * so earlier handlers have had their say; the outcome is confirmed in
     * {@link #resolvePendingClips}.
     *
     * <p>Only the clipper is watched. Plain right-click harvesting is deliberately not counted: in
     * AgriCraft 4.0.8 {@code CropBlock.rightClickLogic} gives a held seed, fertilizer or crop stick
     * priority over picking the crop, so a right-click harvest only lands with a hand holding
     * nothing the mod cares about — too conditional to build a daily task on.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ServerPlayer player = serverPlayer(event.getEntity());
        if (player == null || !event.getItemStack().is(ModItems.CLIPPER.get())) return;

        // Deliberately not filtered on canBeHarvested(): the confirmation below keys off the growth
        // stage moving, which holds whether the clipper ends up clipping the crop or -- as it does
        // today, because rightClickLogic passes it through to useWithoutItem -- harvesting it.
        AgriApi.getCrop(event.getLevel(), event.getPos()).filter(AgriCrop::hasPlant).ifPresent(crop -> {
            PENDING_CLIPS.add(new PendingClip(player.getUUID(), event.getLevel().dimension(),
                    event.getPos().immutable(), crop.getPlantId(), crop.getGrowthStage().index()));
            RollMod.LOGGER.debug("[DailyTasks] Queued clip candidate: {} at {} stage {}",
                    crop.getPlantId(), event.getPos(), crop.getGrowthStage().index());
        });
    }

    /**
     * Confirms the queued right-clicks. Note this is not necessarily the same tick that queued
     * them: an interaction packet is drained in {@code MinecraftServer.waitUntilNextTick}, which
     * runs after {@code ServerTickEvent.Post}, so a candidate queued during tick N is normally
     * settled at the end of tick N+1. Either way the block has already acted by then.
     */
    private static void resolvePendingClips(MinecraftServer server) {
        if (PENDING_CLIPS.isEmpty()) return;

        for (PendingClip pending : PENDING_CLIPS) {
            ServerLevel level = server.getLevel(pending.dimension());
            ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
            if (level == null || player == null) continue;

            // Both outcomes wind the crop back -- a clip to the plant's initial stage, a harvest to
            // its harvest stage -- so the stage going backwards is the proof the clipper did
            // something. A crop that was broken or unloaded instead no longer counts.
            AgriCrop crop = AgriApi.getCrop(level, pending.pos()).filter(AgriCrop::hasPlant).orElse(null);
            if (crop == null) {
                RollMod.LOGGER.debug("[DailyTasks] Clip candidate at {} dropped: no crop there now.",
                        pending.pos());
                continue;
            }
            if (crop.getGrowthStage().index() >= pending.stageAtClick()) {
                RollMod.LOGGER.debug("[DailyTasks] Clip candidate at {} dropped: stage still {}.",
                        pending.pos(), crop.getGrowthStage().index());
                continue;
            }

            RollMod.LOGGER.debug("[DailyTasks] Clip confirmed: {} at {} ({} -> {}).",
                    pending.plantId(), pending.pos(), pending.stageAtClick(), crop.getGrowthStage().index());
            DailyTaskManager.progress(player, DailyTaskHook.CLIP, pending.plantId(), 1);
        }
        PENDING_CLIPS.clear();
    }

    /* --------------------------------------- PLAYTIME / DISTANCE / RAID --------------------------------------- */

    /**
     * Last-seen vanilla statistics for one player, plus the sub-unit remainder.
     *
     * <p>Deltas rather than absolute values: a player's lifetime play time is not today's progress.
     * The remainders keep a task honest across samples — walking 60cm a second still adds up to
     * whole blocks instead of rounding away to nothing.
     */
    private static final class Sample {
        int lastPlayTicks = -1;
        int lastDistanceCm = -1;
        int lastRaidWins;
        int lastJumps;
        int carryTicks;
        int carryCm;
    }

    private static final Map<UUID, Sample> SAMPLES = new HashMap<>();

    private static void sampleStats(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Sample sample = SAMPLES.computeIfAbsent(player.getUUID(), id -> new Sample());

            int playTicks = player.getStats().getValue(Stats.CUSTOM, Stats.PLAY_TIME);
            int distanceCm = player.getStats().getValue(Stats.CUSTOM, Stats.WALK_ONE_CM)
                    + player.getStats().getValue(Stats.CUSTOM, Stats.SPRINT_ONE_CM)
                    + player.getStats().getValue(Stats.CUSTOM, Stats.CROUCH_ONE_CM);
            // Vanilla awards this on raid victory, to each player the raid counted as a hero of
            // the village -- so the delta is "raids this player helped win since the last sample".
            int raidWins = player.getStats().getValue(Stats.CUSTOM, Stats.RAID_WIN);
            int jumps = player.getStats().getValue(Stats.CUSTOM, Stats.JUMP);

            // The first sample after login only establishes a baseline. Math.max guards the case
            // where a statistic is reset underneath us.
            if (sample.lastPlayTicks >= 0) {
                sample.carryTicks += Math.max(0, playTicks - sample.lastPlayTicks);
                sample.carryCm += Math.max(0, distanceCm - sample.lastDistanceCm);

                int minutes = sample.carryTicks / TICKS_PER_MINUTE;
                if (minutes > 0) {
                    sample.carryTicks -= minutes * TICKS_PER_MINUTE;
                    DailyTaskManager.progress(player, DailyTaskHook.PLAYTIME, player, minutes);
                }

                int blocks = sample.carryCm / CM_PER_BLOCK;
                if (blocks > 0) {
                    sample.carryCm -= blocks * CM_PER_BLOCK;
                    DailyTaskManager.progress(player, DailyTaskHook.DISTANCE, player, blocks);
                }

                // Already a whole count, so no carry: a raid is either won or it is not.
                int raids = Math.max(0, raidWins - sample.lastRaidWins);
                if (raids > 0) {
                    DailyTaskManager.progress(player, DailyTaskHook.RAID, player, raids);
                }

                int jumped = Math.max(0, jumps - sample.lastJumps);
                if (jumped > 0) {
                    DailyTaskManager.progress(player, DailyTaskHook.JUMP, player, jumped);
                }
            }

            sample.lastPlayTicks = playTicks;
            sample.lastDistanceCm = distanceCm;
            sample.lastRaidWins = raidWins;
            sample.lastJumps = jumps;
        }
    }

    /* ---------------------------------------------------------------------------------------------------------- */

    private static ServerPlayer serverPlayer(Player player) {
        return player instanceof ServerPlayer server && !server.level().isClientSide() ? server : null;
    }
}
