package com.roll_54.roll_mod.compat.curios;

import com.roll_54.roll_mod.minestar.deathlog.DeathRecord.CurioStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Curios, if it is installed. Free of any Curios type itself, so callers can ask without class-loading
 * the mod; the real work is in {@link CuriosAccess}, which is only touched once {@link #loaded()} says so.
 */
public final class CuriosCompat {

    private static Boolean loaded;

    private CuriosCompat() {}

    public static boolean loaded() {
        if (loaded == null) loaded = ModList.get().isLoaded("curios");
        return loaded;
    }

    /** Every non-empty curio the entity wears, cosmetic slots included; empty without Curios. */
    public static List<CurioStack> snapshot(LivingEntity entity) {
        return loaded() ? CuriosAccess.snapshot(entity) : List.of();
    }

    /**
     * Puts {@code stack} back where it was worn, if that slot still exists and is empty. {@code false}
     * when it could not, and the caller should hand the stack over some other way.
     */
    public static boolean restore(LivingEntity entity, CurioStack curio) {
        return loaded() && CuriosAccess.restore(entity, curio);
    }

    /**
     * One row of curio slots as a moderator's window lays it out: a slot type and whether it is the
     * cosmetic row of that type.
     */
    public record CurioRow(String id, int size, boolean cosmetic) {}

    /** The entity's curio rows, cosmetic rows after their type's own; empty without Curios. */
    public static List<CurioRow> rows(LivingEntity entity) {
        return loaded() ? CuriosAccess.rows(entity) : List.of();
    }

    /**
     * The live stacks of one curio row, refusing what Curios would not let the entity wear there.
     * {@code null} without Curios, or when the entity no longer has that row.
     */
    @Nullable
    public static IItemHandlerModifiable handler(LivingEntity entity, CurioRow row) {
        return loaded() ? CuriosAccess.handler(entity, row) : null;
    }

    /** {@link #restore} for a whole list; what did not fit comes back. */
    public static List<ItemStack> restoreAll(LivingEntity entity, List<CurioStack> curios) {
        List<ItemStack> left = new java.util.ArrayList<>();
        for (CurioStack curio : curios) {
            if (!restore(entity, curio)) left.add(curio.stack().copy());
        }
        return left;
    }
}
