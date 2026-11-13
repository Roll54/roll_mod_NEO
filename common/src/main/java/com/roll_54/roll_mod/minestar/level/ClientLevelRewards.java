package com.roll_54.roll_mod.minestar.level;

import com.roll_54.roll_mod.network.packet.LevelRewardsPacket;

import java.util.ArrayList;
import java.util.List;

/** The level reward rules and the player's level, as the server last sent them ({@link LevelRewardsPacket}). */
public final class ClientLevelRewards {

    public static volatile int LEVEL;
    public static volatile List<LevelPerks.Rule> RULES = List.of();
    public static volatile int VERSION;

    private ClientLevelRewards() {}

    public static void accept(LevelRewardsPacket payload) {
        List<LevelPerks.Rule> rules = new ArrayList<>();
        for (String line : payload.rules()) {
            LevelPerks.Rule rule = LevelPerks.Rule.parse(line);
            if (rule != null) rules.add(rule);
        }
        LEVEL = payload.level();
        RULES = List.copyOf(rules);
        VERSION++;
    }
}
