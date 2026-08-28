package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.compat.ftb.TeamsFacade;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

/**
 * Resolves a player to the "group" whose task progress they contribute to.
 *
 * <p>Members of an FTB party share one group, keyed by the party's team id. Everyone else is their
 * own group, keyed by their own UUID — so the system works unchanged when ftbteams is absent.
 */
public final class DailyTaskGroups {

    /**
     * @param id          the key into {@link DailyTasksState#groups}
     * @param members     every member, online or not (a single UUID for a solo player)
     */
    public record TaskGroup(UUID id, Set<UUID> members) {
        public int memberCount() {
            return members.size();
        }
    }

    private DailyTaskGroups() {}

    public static TaskGroup of(ServerPlayer player) {
        return TeamsFacade.get().party(player)
                .map(party -> new TaskGroup(party.teamId(), party.members()))
                .orElseGet(() -> new TaskGroup(player.getUUID(), Set.of(player.getUUID())));
    }
}
