package com.roll_54.roll_mod.minestar.battlepass;

import net.minecraft.nbt.CompoundTag;

/** The player's battle pass as the server last sent it, and a version that moves with every update. */
public final class ClientBattlePassCache {

    public static volatile CompoundTag DATA = new CompoundTag();
    public static volatile int VERSION;

    /** The battle pass's settings, for the Moderation tab's editor; only sent to those who may change them. */
    public static volatile CompoundTag ADMIN = new CompoundTag();
    public static volatile int ADMIN_VERSION;

    /** The daily tasks' reward pool, for the Moderation tab's Dailies editor. */
    public static volatile CompoundTag DAILY = new CompoundTag();
    public static volatile int DAILY_VERSION;

    private ClientBattlePassCache() {}

    public static void putDaily(CompoundTag data) {
        DAILY = data;
        DAILY_VERSION++;
    }

    public static void putAdmin(CompoundTag data) {
        ADMIN = data;
        ADMIN_VERSION++;
    }

    public static void put(CompoundTag data) {
        DATA = data;
        VERSION++;
    }
}
