package com.roll_54.roll_mod.minestar.dailytasks.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link DailyTask} implementation for automatic pickup.
 *
 * <p>You do not register a task; the registry finds it. At mod setup
 * {@code DailyTaskRegistry.bootstrap()} scans every loaded mod file for this annotation and
 * instantiates what it finds, so writing a task means writing one class and nothing else:
 *
 * <pre>{@code
 * @AutoDailyTask
 * public final class MineOresTask implements DailyTask {
 *     private static final DailyTaskIcon ICON = DailyTaskIcon.of(Items.IRON_ORE);
 *
 *     @Override public String id()             { return "mine_ores"; }
 *     @Override public DailyTaskHook hook()    { return DailyTaskHook.MINE; }
 *     @Override public int baseAmount()        { return 32; }
 *     @Override public DailyTaskIcon icon()    { return ICON; }
 *
 *     @Override
 *     public boolean matches(Object subject) {
 *         return subject instanceof BlockState state && state.is(Tags.Blocks.ORES);
 *     }
 * }
 * }</pre>
 *
 * <p>{@code name()} and {@code tooltip(int)} default to the {@code dailytask.roll_mod.<id>.title}
 * and {@code .desc} translations, so the two lang keys are all that is left to add.
 *
 * <p>The annotated class must implement {@link DailyTask} and have a public no-argument
 * constructor. Because the scan covers every mod file, another mod can add tasks the same way
 * without touching this one.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoDailyTask {
}
