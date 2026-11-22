package com.roll_54.roll_mod.minestar.staff;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The last data the server sent for each kind ("commands", "polls", "tickets", "my_tickets"), and a version per kind. */
public final class ClientStaffCache {

    private static final Map<String, CompoundTag> DATA = new ConcurrentHashMap<>();
    private static final Map<String, Integer> VERSIONS = new ConcurrentHashMap<>();

    private ClientStaffCache() {}

    public static void put(String kind, CompoundTag data) {
        DATA.put(kind, data);
        VERSIONS.merge(kind, 1, Integer::sum);
    }

    public static CompoundTag get(String kind) {
        return DATA.getOrDefault(kind, new CompoundTag());
    }

    /** Moves whenever {@code kind} arrives again, for the tabs' rebuild signatures. */
    public static int version(String kind) {
        return VERSIONS.getOrDefault(kind, 0);
    }
}
