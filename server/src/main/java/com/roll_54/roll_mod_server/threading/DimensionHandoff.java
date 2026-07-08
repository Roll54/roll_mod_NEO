package com.roll_54.roll_mod_server.threading;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;

/**
 * Asynchronous entity/player transfer between world threads (Stage 3).
 *
 * <p>Vanilla {@code Entity.changeDimension(DimensionTransition)} ({@code Entity.java:2719}) and
 * {@code ServerPlayer.changeDimension} ({@code ServerPlayer.java:887}) move an entity by removing
 * it from the source level and adding it to the destination level <i>synchronously on one thread</i>.
 * With levels on separate threads that is unsafe: the destination must be mutated on <i>its</i>
 * thread. This class turns the transfer into a handoff — the source thread removes/serializes, then
 * schedules the add onto the destination world's {@link WorldEventLoop} via
 * {@link GlobalCoordinator#executeOn}. For players, inbound packets are buffered while "in transit".
 *
 * <p><b>Stage 0:</b> declaration only — no call sites are rewired yet, so dimension changes still go
 * through the vanilla synchronous path.
 *
 * <p>Plan: {@code ~/.claude/plans/how-hard-is-to-wiggly-goblet.md}.
 */
public final class DimensionHandoff {

    private DimensionHandoff() {}

    /**
     * Perform an async cross-thread dimension change.
     * TODO(stage-3): implement remove-on-source → schedule add-on-destination, covering portals,
     * ender pearls, {@code /tp}, {@code /execute in}, and respawn; buffer packets for in-transit players.
     */
    public static Entity changeDimensionAsync(Entity entity, DimensionTransition transition) {
        throw new UnsupportedOperationException("DimensionHandoff is implemented in Stage 3");
    }
}
