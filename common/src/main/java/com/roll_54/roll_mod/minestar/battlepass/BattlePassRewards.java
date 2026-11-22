package com.roll_54.roll_mod.minestar.battlepass;

import java.util.ArrayList;
import java.util.List;

/**
 * The default rewards, laid out by the quest book's main chapters — one block of 16 free levels (and 8 Prime) per
 * chapter: stone age, bronze, steel, LV, MV, HV, EV, IV. Each block pays in what that stage of the book uses, so a
 * player is never handed parts far beyond where they are; the last level of a block is its big one. Edit the result
 * in {@code battlepass.json}.
 */
final class BattlePassRewards {

    private BattlePassRewards() {}

    private static final String MI = "modern_industrialization:";

    /** Per block: the everyday pool (cycled through levels 1–15) and the block's milestone (level 16). */
    private static final String[][] POOLS = {
            {"minecraft:bread*16", "minecraft:torch*32", "minecraft:coal*16", "minecraft:iron_ingot*8",
                    "minecraft:copper_ingot*16", "minecraft:oak_log*32", "money:300"},
            {MI + "tin_ingot*16", MI + "bronze_ingot*12", MI + "bronze_plate*8", MI + "bronze_gear*4",
                    "minecraft:iron_ingot*16", "minecraft:cooked_beef*16", "money:500"},
            {MI + "steel_ingot*12", MI + "steel_plate*8", MI + "steel_gear*4", MI + "analog_circuit*2",
                    MI + "bronze_ingot*24", "roll_mod:redstone_dust*32", "money:800"},
            {MI + "analog_circuit*4", MI + "motor*2", MI + "piston*2", MI + "conveyor*2", MI + "pump*2",
                    MI + "copper_wire*16", "money:1200"},
            {MI + "electronic_circuit*2", MI + "aluminum_ingot*12", MI + "aluminum_plate*8", MI + "large_motor*2",
                    MI + "robot_arm*2", MI + "electrum_ingot*8", "money:2000"},
            // Past MV no ingots at all (and no uranium): plates, gears, parts and money instead.
            {MI + "digital_circuit*2", MI + "stainless_steel_gear*4", MI + "stainless_steel_plate*8",
                    MI + "advanced_motor*2", MI + "silicon_plate*8", MI + "chromium_plate*6", "money:3000"},
            {MI + "processing_unit*2", MI + "titanium_gear*4", MI + "titanium_plate*6",
                    MI + "large_advanced_motor*2", MI + "platinum_plate*4", "minecraft:diamond*6", "money:4500"},
            {MI + "quantum_circuit*1", MI + "tungsten_plate*8", MI + "iridium_plate*4", "minecraft:nether_star*1",
                    MI + "superconductor_cable*8", MI + "ultradense_metal_ball*1", "money:7000"},
    };

    private static final String[] MILESTONES = {
            "minecraft:diamond*3", MI + "bronze_plate*32", MI + "steel_plate*24", MI + "redstone_battery*2",
            MI + "sodium_battery*2", MI + "cadmium_battery*2", MI + "silicon_battery*2", MI + "plutonium_battery*2"};

    /** Prime: the same block's pool at double the count, plus more money, and two milestones per block. */
    private static final String[] PRIME_MILESTONES = {
            "choice:money:3000|minecraft:diamond*8|roll_mod:redstone_dust*128",
            "choice:" + MI + "bronze_ingot*48|" + MI + "bronze_plate*48|money:4000",
            "choice:" + MI + "steel_ingot*48|" + MI + "steel_plate*40|money:6000",
            "choice:" + MI + "motor*8|" + MI + "analog_circuit*16|money:9000",
            "choice:" + MI + "large_motor*6|" + MI + "electronic_circuit*8|money:14000",
            "choice:" + MI + "advanced_motor*6|" + MI + "digital_circuit*8|money:20000",
            "choice:" + MI + "large_advanced_motor*6|" + MI + "processing_unit*6|money:30000",
            "choice:" + MI + "quantum_circuit*4|skin:roll_mod:murasama|money:50000"};

    static List<String> defaultFree() {
        List<String> out = new ArrayList<>();
        for (int block = 0; block < BattlePassConfig.BLOCKS; block++) {
            String[] pool = POOLS[block];
            for (int i = 0; i < BattlePassConfig.FREE_PER_BLOCK - 1; i++) out.add(pool[i % pool.length]);
            out.add(MILESTONES[block]);
        }
        return out;
    }

    static List<String> defaultPrime() {
        List<String> out = new ArrayList<>();
        for (int block = 0; block < BattlePassConfig.BLOCKS; block++) {
            String[] pool = POOLS[block];
            for (int i = 0; i < BattlePassConfig.PRIME_PER_BLOCK - 1; i++) out.add(doubled(pool[(i * 2 + 1) % pool.length]));
            out.add(PRIME_MILESTONES[block]);
        }
        return out;
    }

    /** Vanilla redstone in an older config becomes roll_mod's redstone dust, which the pack uses everywhere. */
    static String migrate(String spec) {
        if (spec == null) return null;
        String out = spec.replace("minecraft:redstone*", "roll_mod:redstone_dust*");
        // No ingots past MV and no uranium in the pass: an older config's are swapped for the same metal's plate.
        for (String[] swap : NO_INGOTS) out = out.replace(MI + swap[0] + "*", MI + swap[1] + "*");
        return out;
    }

    private static final String[][] NO_INGOTS = {
            {"stainless_steel_ingot", "stainless_steel_plate"}, {"silicon_ingot", "silicon_plate"},
            {"chromium_ingot", "chromium_plate"}, {"titanium_ingot", "titanium_plate"},
            {"platinum_ingot", "platinum_plate"}, {"tungsten_ingot", "tungsten_plate"},
            {"iridium_ingot", "iridium_plate"}, {"uranium_ingot", "ultradense_metal_ball"},
            {"plutonium_ingot", "superconductor_cable"}};

    private static String doubled(String spec) {
        int star = spec.lastIndexOf('*');
        if (spec.startsWith("money:")) return "money:" + Long.parseLong(spec.substring(6)) * 2;
        if (star < 0) return spec;
        return spec.substring(0, star + 1) + Integer.parseInt(spec.substring(star + 1)) * 2;
    }
}
