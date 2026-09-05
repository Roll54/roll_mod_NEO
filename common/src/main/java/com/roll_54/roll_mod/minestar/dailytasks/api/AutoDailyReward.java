package com.roll_54.roll_mod.minestar.dailytasks.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link DailyReward} implementation for automatic pickup — the reward counterpart of
 * {@link AutoDailyTask}.
 *
 * <p>You do not register a reward; the registry finds it. At mod setup
 * {@code DailyRewardRegistry.bootstrap()} scans every loaded mod file for this annotation and
 * instantiates what it finds:
 *
 * <pre>{@code
 * @AutoDailyReward
 * public final class DiamondsReward implements DailyReward {
 *     private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.DIAMOND);
 *
 *     @Override public String id()          { return "diamonds"; }
 *     @Override public DailyTaskIcon icon() { return ICON; }
 *
 *     @Override
 *     public void grant(ServerPlayer player) {
 *         DailyTaskRewards.give(player, new ItemStack(Items.DIAMOND, 8));
 *     }
 * }
 * }</pre>
 *
 * <p>{@code name()} and {@code tooltip()} default to the {@code dailyreward.roll_mod.<id>.title}
 * and {@code .desc} translations, so the two lang keys are all that is left to add.
 *
 * <p>The annotated class must implement {@link DailyReward} and have a public no-argument
 * constructor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoDailyReward {
}
