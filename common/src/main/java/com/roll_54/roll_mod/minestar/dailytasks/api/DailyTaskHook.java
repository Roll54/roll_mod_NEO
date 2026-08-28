package com.roll_54.roll_mod.minestar.dailytasks.api;

/**
 * How a {@link DailyTask} is wired into Minecraft: each constant is backed by exactly one NeoForge
 * event in {@code DailyTaskEvents}, and fixes the runtime type of the {@code subject} handed to
 * {@link DailyTask#matches(Object)}.
 *
 * <p>Adding a hook means adding a constant here <em>and</em> a listener that funnels it into
 * {@code DailyTaskManager.progress}. Everything else — the tasks themselves — needs no change.
 */
public enum DailyTaskHook {
    /** A block was broken. Subject: {@link net.minecraft.world.level.block.state.BlockState}. */
    MINE,
    /**
     * An AgriCraft crop was clipped. Subject: the plant id as a {@link String}, e.g.
     * {@code "roll_mod:latex_dandelion"} — captured before the interaction, since it resets the
     * crop's growth stage. A task that counts any crop just checks the type; one that wants a
     * specific plant compares the id.
     */
    CLIP,
    /** A living entity was killed by the player. Subject: {@link net.minecraft.world.entity.LivingEntity}. */
    KILL,
    /** An item was crafted. Subject: {@link net.minecraft.world.item.ItemStack}. */
    CRAFT,
    /** An item was taken from a furnace result slot. Subject: {@link net.minecraft.world.item.ItemStack}. */
    SMELT,
    /** Something was reeled in. Subject: {@link net.minecraft.world.item.ItemStack}. */
    FISH,
    /** Two animals were bred. Subject: {@link net.minecraft.world.entity.LivingEntity} (a parent). */
    BREED,
    /** Food finished being eaten. Subject: {@link net.minecraft.world.item.ItemStack}. */
    EAT,
    /**
     * Time spent in game, counted in whole minutes. Subject: the
     * {@link net.minecraft.server.level.ServerPlayer} it accrued for.
     *
     * <p>Sampled from the vanilla {@code minecraft:play_time} statistic, so it advances only while
     * the player is actually online.
     */
    PLAYTIME,
    /**
     * Ground covered on foot, counted in whole blocks, in any direction. Subject: the
     * {@link net.minecraft.server.level.ServerPlayer} it accrued for.
     *
     * <p>Sampled from the vanilla walk, sprint and crouch distance statistics. Boats, minecarts,
     * horses, elytra and swimming are deliberately excluded — this is a walking task.
     */
    DISTANCE
}
