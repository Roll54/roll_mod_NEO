package com.roll_54.roll_mod.compat.MBD2.greenhouse;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client side only: blocks of formed greenhouses that the chunk renderer skips, so the controller's
 * model is all that shows. The blocks themselves stay in the world as they are — collision, clicks,
 * pipes and hatches all work — they are just not drawn. Read by {@code SectionCompilerHideMixin}
 * from chunk-compile threads, hence the concurrent collections.
 *
 * <p>Nothing here is ever touched on a dedicated server: only {@link GrowerTrait#clientTick} and the
 * client mixin call it.
 */
public final class HiddenStructureBlocks {

    /** Hidden positions per controller. */
    private static final Map<BlockPos, Set<BlockPos>> BY_CONTROLLER = new ConcurrentHashMap<>();
    /** All hidden positions, with how many controllers hide each (structures never overlap, but be safe). */
    private static final Map<BlockPos, Integer> HIDDEN = new ConcurrentHashMap<>();

    private HiddenStructureBlocks() {}

    public static boolean isHidden(BlockPos pos) {
        return !HIDDEN.isEmpty() && HIDDEN.containsKey(pos);
    }

    /** Hides exactly {@code positions} for this controller, redrawing what changed. */
    public static void set(BlockPos controller, Collection<BlockPos> positions) {
        Set<BlockPos> wanted = Set.copyOf(positions);
        Set<BlockPos> current = BY_CONTROLLER.get(controller);
        if (wanted.equals(current)) return;
        clear(controller);
        BY_CONTROLLER.put(controller.immutable(), wanted);
        wanted.forEach(pos -> HIDDEN.merge(pos, 1, Integer::sum));
        redraw(wanted);
    }

    /** Shows this controller's blocks again. */
    public static void clear(BlockPos controller) {
        Set<BlockPos> removed = BY_CONTROLLER.remove(controller);
        if (removed == null) return;
        removed.forEach(pos -> HIDDEN.computeIfPresent(pos, (key, count) -> count > 1 ? count - 1 : null));
        redraw(removed);
    }

    private static void redraw(Set<BlockPos> positions) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.levelRenderer == null) return;
        for (BlockPos pos : positions) {
            minecraft.levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
