package com.roll_54.roll_mod.compat.ftb;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.Set;

/**
 * The real {@link TeamsFacade}. Never loaded unless {@code ftbteams} is present — see
 * {@link TeamsFacade.Holder}.
 */
public final class FTBTeamsFacadeImpl implements TeamsFacade {

    @Override
    public Optional<Party> party(ServerPlayer player) {
        var api = FTBTeamsAPI.api();
        if (!api.isManagerLoaded()) {
            return Optional.empty();
        }
        return api.getManager()
                .getTeamForPlayer(player)
                .filter(Team::isPartyTeam)
                .map(team -> new Party(team.getTeamId(), Set.copyOf(team.getMembers())));
    }
}
