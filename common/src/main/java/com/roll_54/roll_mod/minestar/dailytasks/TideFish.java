package com.roll_54.roll_mod.minestar.dailytasks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Tide's fish by the rarity its journal gives them ({@code data/tide/fishing/fish/**}, "journal_profile.rarity"; no
 * rarity there means common). Tide has no item tag per rarity, so the lists are kept here, taken from Tide 2.0.3; a fish
 * a later Tide adds simply counts for none of them until it is listed.
 */
public final class TideFish {

    public enum Rarity { COMMON, UNCOMMON, RARE, VERY_RARE, LEGENDARY }

    private static final Set<String> COMMON = Set.of("black_crappie", "bluegill", "brook_trout", "carp",
            "largemouth_bass", "pike", "rainbow_trout", "smallmouth_bass", "white_crappie", "yellow_perch", "ash_perch",
            "ember_koi", "magma_mackerel", "anchovy", "flounder", "mackerel", "ocean_perch", "red_snapper", "snook", "tuna",
            "cave_crawler", "cave_eel", "deep_grouper", "shadow_snapper", "amber_rockfish", "bedrock_bug", "enderfin",
            "incandescent_larva", "pale_clubfish", "sleepy_carp");
    private static final Set<String> UNCOMMON = Set.of("catfish", "guppy", "walleye", "obsidian_pike", "volcano_tuna",
            "angelfish", "abyss_angler", "anglerfish", "crystal_shrimp", "dripstone_darter", "glowfish", "iron_tetra",
            "lapis_lanternfish", "blue_neonfish", "chorus_cod", "deep_blue", "ender_glider", "endergazer", "judgment_fish",
            "violet_carp");
    private static final Set<String> RARE = Set.of("arapaima", "blossom_bass", "frostbite_flounder", "mirage_catfish",
            "sand_tiger_shark", "slimy_salmon", "sturgeon", "crimson_fangjaw", "inferno_guppy", "soulscale", "warped_guppy",
            "witherfin", "mahi_mahi", "manta_ray", "sailfish", "spore_stalker", "swordfish", "bedrock_tetra",
            "crystalline_carp", "gilded_minnow", "luminescent_jellyfish", "dutchman_sock", "elytrout", "nephrosilu",
            "pentapus", "red_40", "vengeance");
    private static final Set<String> VERY_RARE = Set.of("bull_shark", "mooneye", "blazing_swordfish", "aquathorn",
            "great_white_shark", "marstilus", "neptune_koi", "pluto_snail", "saturn_cuttlefish", "sun_emblem",
            "uranias_pisces", "chasm_eel", "echo_snapper", "windbass", "darkness_eater", "mantyvern", "shadow_shark",
            "snatcher_squid");
    private static final Set<String> LEGENDARY = Set.of("coelacanth", "shooting_starfish", "devils_hole_pupfish",
            "midas_fish", "alpha_fish", "dragon_fish", "voidseeker");

    private TideFish() {}

    /** Tide's fish of {@code rarity}, by item path, sorted. */
    public static java.util.List<String> fish(Rarity rarity) {
        Set<String> set = switch (rarity) {
            case COMMON -> COMMON;
            case UNCOMMON -> UNCOMMON;
            case RARE -> RARE;
            case VERY_RARE -> VERY_RARE;
            case LEGENDARY -> LEGENDARY;
        };
        return set.stream().sorted().toList();
    }

    /** Whether {@code subject} is a Tide fish of {@code rarity}. */
    public static boolean is(Object subject, Rarity rarity) {
        if (!(subject instanceof ItemStack stack) || stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"tide".equals(id.getNamespace())) return false;
        return switch (rarity) {
            case COMMON -> COMMON.contains(id.getPath());
            case UNCOMMON -> UNCOMMON.contains(id.getPath());
            case RARE -> RARE.contains(id.getPath());
            case VERY_RARE -> VERY_RARE.contains(id.getPath());
            case LEGENDARY -> LEGENDARY.contains(id.getPath());
        };
    }
}
