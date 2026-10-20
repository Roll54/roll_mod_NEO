package com.roll_54.roll_mod.minestar.leaderboard;

import javax.annotation.Nullable;
import java.util.List;

/** The client's copy of the board last asked for — see {@code LeaderboardPackets}. */
public final class ClientLeaderboardCache {

    public static volatile LeaderboardCategory CATEGORY = LeaderboardCategory.RICHEST;
    public static volatile List<LeaderboardRow> ROWS = List.of();
    @Nullable public static volatile LeaderboardRow OWN;
    public static volatile boolean FAILED;
    /** False until the first answer arrives, so the tab can say "loading" rather than "empty". */
    public static volatile boolean LOADED;
    public static volatile int VERSION;

    private ClientLeaderboardCache() {}
}
