package com.roll_54.roll_mod.minestar.dailytasks.api;

import com.roll_54.roll_mod.RollMod;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;

/**
 * Finds annotated implementations across every loaded mod file.
 *
 * <p>Shared by {@code DailyTaskRegistry} and {@code DailyRewardRegistry}: both are built by scanning
 * for a marker annotation ({@link AutoDailyTask}, {@link AutoDailyReward}) and instantiating what
 * turns up, so neither has to be told what exists — and another mod can contribute either kind
 * without this one knowing about it.
 *
 * <p>Only the scan lives here. Validation, ordering and indexing stay with each registry, because
 * they differ: a task must have a positive {@code baseAmount}, a reward has no such notion.
 */
public final class AutoScanner {

    private AutoScanner() {}

    /**
     * Every class annotated with {@code annotation} that implements {@code type}, instantiated via
     * its no-argument constructor. Order follows the scan and is therefore <em>not</em> stable —
     * callers that sync positions over the network must sort the result themselves.
     *
     * <p>A class that cannot be loaded or constructed is logged and skipped rather than failing the
     * whole scan: one broken contribution must not take the system down with it.
     */
    public static <T> List<T> scan(Class<? extends Annotation> annotation, Class<T> type) {
        List<T> found = new ArrayList<>();
        Type marker = Type.getType(annotation);

        for (ModFileScanData scan : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData data : scan.getAnnotations()) {
                if (!marker.equals(data.annotationType()) || data.targetType() != ElementType.TYPE) {
                    continue;
                }
                instantiate(data.memberName(), annotation, type).ifPresent(found::add);
            }
        }
        return found;
    }

    private static <T> java.util.Optional<T> instantiate(String className,
                                                         Class<? extends Annotation> annotation,
                                                         Class<T> type) {
        try {
            Class<?> clazz = Class.forName(className, false, AutoScanner.class.getClassLoader());
            if (!type.isAssignableFrom(clazz)) {
                RollMod.LOGGER.error("[DailyTasks] {} is annotated @{} but does not implement {}.",
                        className, annotation.getSimpleName(), type.getSimpleName());
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(type.cast(clazz.getDeclaredConstructor().newInstance()));
        } catch (Throwable t) {
            RollMod.LOGGER.error("[DailyTasks] Could not create {} — it needs a public no-arg constructor.",
                    className, t);
            return java.util.Optional.empty();
        }
    }
}
