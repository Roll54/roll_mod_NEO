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
    /**
     * A furnace, smoker or blast furnace finished an item. Subject: the produced
     * {@link net.minecraft.world.item.ItemStack}.
     *
     * <p>Counted when it is produced, not when it is taken out, and credited to the furnace's owner
     * — whoever placed it or last opened it. That way output pulled out by hoppers or pipes counts
     * too, which the player-only {@code ItemSmeltedEvent} (and FTB Quests, built on it) never sees.
     * An owner who is offline gets nothing.
     */
    SMELT,
    /** Something was reeled in. Subject: {@link net.minecraft.world.item.ItemStack}. */
    FISH,
    /** Two animals were bred. Subject: {@link net.minecraft.world.entity.LivingEntity} (a parent). */
    BREED,
    /** Food finished being eaten. Subject: {@link net.minecraft.world.item.ItemStack}. */
    EAT,
    /**
     * A mob was sheared by a player with shears. Subject: the sheared
     * {@link net.minecraft.world.entity.LivingEntity}. A mooshroom still reports as a mooshroom,
     * even though shearing has already replaced it with a cow.
     */
    SHEAR,
    /**
     * A player jumped. Subject: the {@link net.minecraft.server.level.ServerPlayer} it counted for.
     * Sampled from the vanilla {@code minecraft:jump} statistic.
     */
    JUMP,
    /**
     * A Farmer's Delight dish was cooked. Subject: the cooked {@link net.minecraft.world.item.ItemStack}.
     *
     * <p>A cooking pot counts each batch the moment it finishes, credited to the pot's owner (whoever
     * placed it or last opened it), like SMELT; a hand-held skillet counts for the player holding it.
     */
    COOK,

    EAT_SPEFIC,
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
    DISTANCE,
    /**
     * A raid was won. Subject: the {@link net.minecraft.server.level.ServerPlayer} it counted for.
     *
     * <p>Sampled from the vanilla {@code minecraft:raid_win} statistic, which {@code Raid} awards
     * on victory only to the players it counted as heroes of the village — those who actually
     * fought the raiders. A bystander who watched the village defend itself gets nothing, which is
     * the wanted behaviour.
     */
    RAID,
    /* ---- Made in the task editor only (no code task uses these). ---- */
    /** A block was placed by a player. Subject: the placed {@link net.minecraft.world.level.block.state.BlockState}. */
    PLACE,
    /** An animal was tamed. Subject: the tamed {@link net.minecraft.world.entity.LivingEntity}. */
    TAME,
    /** A trade with a villager or wandering trader. Subject: the bought {@link net.minecraft.world.item.ItemStack}. */
    TRADE,
    /** An item was enchanted at an enchanting table. Subject: the enchanted {@link net.minecraft.world.item.ItemStack}. */
    ENCHANT,
    /** A potion was taken out of a brewing stand. Subject: the potion {@link net.minecraft.world.item.ItemStack}. */
    BREW,
    /** An anvil result was taken. Subject: the output {@link net.minecraft.world.item.ItemStack}. */
    ANVIL,
    /** An advancement was earned (recipe unlocks excluded). Subject: its id as a {@link String}. */
    ADVANCEMENT,
    /** Experience levels gained, counted per level. Subject: the {@link net.minecraft.server.level.ServerPlayer}. */
    LEVEL_UP,
    /** Using an item finished (eating, drinking, a spyglass…). Subject: the used {@link net.minecraft.world.item.ItemStack}. */
    USE,
    /** Damage dealt by the player, in whole points (2 = a heart). Subject: the hurt {@link net.minecraft.world.entity.LivingEntity}. */
    DAMAGE,
    /** The player entered a dimension. Subject: the dimension id as a {@link String}. */
    VISIT,
    /**
     * Any vanilla statistic of the "custom" kind ({@code minecraft:fly_one_cm}, {@code minecraft:swim_one_cm},
     * {@code minecraft:bell_ring}…), named in the task's target. Subject: the statistic id as a {@link String}.
     * Distances count in blocks, times in minutes, the rest as they are.
     */
    STAT,
    /** A block was right-clicked (a bell rung, a lever pulled, a crafting table opened…). Subject: its {@link net.minecraft.world.level.block.state.BlockState}. */
    INTERACT_BLOCK,
    /** A creature was right-clicked (milked, fed, named, ridden…). Subject: the {@link net.minecraft.world.entity.LivingEntity}. */
    INTERACT_ENTITY,
    /** An item was right-clicked in the air or on a block (a pearl thrown, a firework launched…). Subject: the {@link net.minecraft.world.item.ItemStack}. */
    RIGHT_CLICK_ITEM,
    /** A night slept through in a bed. Subject: the {@link net.minecraft.server.level.ServerPlayer}. Sampled from {@code minecraft:sleep_in_bed}. */
    SLEEP,
    /** A ripe crop was broken (any block whose "age" is at its maximum). Subject: its {@link net.minecraft.world.level.block.state.BlockState}. */
    HARVEST
}
