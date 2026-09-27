package com.roll_54.roll_mod.compat.ftb;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.server.level.ServerPlayer;

import dev.ftb.mods.ftbteams.api.TeamRank;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

    @Override
    public Optional<UUID> partyId(ServerPlayer player) {
        var api = FTBTeamsAPI.api();
        if (!api.isManagerLoaded()) {
            return Optional.empty();
        }
        // Same lookup as party(), minus the per-call member-set copy — this runs per event.
        return api.getManager()
                .getTeamForPlayer(player)
                .filter(Team::isPartyTeam)
                .map(Team::getTeamId);
    }

    @Override
    public boolean sameParty(ServerPlayer player, UUID other) {
        return party(player).map(party -> party.members().contains(other)).orElse(false);
    }

    @Override
    public boolean trusted(ServerPlayer player, UUID other) {
        var api = FTBTeamsAPI.api();
        if (!api.isManagerLoaded()) return false;

        return api.getManager()
                .getTeamForPlayer(player)
                // A solo player's own "player team" has no allies to speak of: allying needs a party.
                .filter(Team::isPartyTeam)
                .map(team -> {
                    TeamRank rank = team.getRankForPlayer(other);
                    // Not isAllyOrBetter(): that also takes INVITED, which outranks ALLY and means
                    // somebody who was asked to join and has not. An unanswered invitation is not
                    // a trust the owner has granted.
                    return rank == TeamRank.ALLY || rank.isMemberOrBetter();
                })
                .orElse(false);
    }
}
