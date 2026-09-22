package com.roll_54.roll_mod.compat.ftb;

import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Reads FTB Teams party membership without hard-depending on the mod.
 *
 * <p>{@code ftbteams} is a {@code compileOnly} dependency, so no {@code dev.ftb.mods} class may be
 * referenced from a class that loads unconditionally. All such references live in
 * {@link FTBTeamsFacadeImpl}, which {@link #get()} only class-loads once it has confirmed the mod is
 * present. Everything else in the mod talks to this interface.
 */
public interface TeamsFacade {

    /**
     * The party the player belongs to, or empty when they are in no party (FTB always puts a player
     * in a single-member "player team"; that is reported as <em>no</em> party here, because it is
     * not a group the player deliberately joined).
     */
    Optional<Party> party(ServerPlayer player);

    /** A party: its stable id and every member, online or not. */
    record Party(UUID teamId, Set<UUID> members) {}

    /**
     * Whether {@code other} is in the same party as {@code player}.
     *
     * <p>A {@code default} rather than a second abstract method, because {@link #NONE} is a lambda:
     * a functional interface may only have the one.
     */
    default boolean sameParty(ServerPlayer player, UUID other) {
        return false;
    }

    /**
     * Whether {@code other} is in {@code player}'s party or allied to it.
     *
     * <p>Asked in the owner's direction, because an alliance is something one team extends to
     * another and only the owner's team knows who it has extended it to.
     */
    default boolean trusted(ServerPlayer player, UUID other) {
        return false;
    }

    /**
     * The no-op facade used when ftbteams is absent: everybody is on their own, and the two
     * team-shaped questions above answer no — so a player who has asked for team-only teleports
     * gets exactly that rather than an accidental free-for-all.
     */
    TeamsFacade NONE = player -> Optional.empty();

    static TeamsFacade get() {
        return Holder.INSTANCE;
    }

    /** Lazy holder so the {@code Class.forName} lookup happens once, on first use. */
    final class Holder {
        static final TeamsFacade INSTANCE = load();

        private Holder() {}

        private static TeamsFacade load() {
            if (!net.neoforged.fml.ModList.get().isLoaded("ftbteams")) {
                return NONE;
            }
            try {
                return (TeamsFacade) Class
                        .forName("com.roll_54.roll_mod.compat.ftb.FTBTeamsFacadeImpl")
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (Throwable t) {
                com.roll_54.roll_mod.RollMod.LOGGER.warn(
                        "[DailyTasks] ftbteams is loaded but its API could not be bound; "
                                + "falling back to per-player task groups.", t);
                return NONE;
            }
        }
    }
}
