package com.roll_54.roll_mod.compat.tide;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Our fishing rods, as real Tide rods: bait slots, hooks, lines and bobbers from the angling table, Tide's casting and
 * minigame — each with perks of its own ({@link RollFishingRodItem.Perk}). Only ever touched when Tide is loaded ({@code ItemRegistry.register} checks), since the class extends
 * Tide's own. Tiers match Tide's: bamboo like its stone rod, carbon fibre like diamond, meteorite metal past netherite
 * (and, like netherite, it fishes in lava — see the {@code tide:lava_fishing_rods} tag).
 *
 * <p>No recipes here: how they are obtained is up to the pack.
 */
public final class TideRods {

    public static DeferredHolder<Item, Item> BAMBOO;
    public static DeferredHolder<Item, Item> CARBON_FIBER;
    public static DeferredHolder<Item, Item> METEORITE_METAL;
    public static DeferredHolder<Item, Item> ELECTRIC;
    /** A Tide hook (angling table): {@link TideRodPerks#METEORITE_HOOK} chance of a meteorite metal nugget per catch. */
    public static DeferredHolder<Item, Item> METEORITE_HOOK;

    private TideRods() {}

    public static void register(DeferredRegister<Item> items) {
        BAMBOO = items.register("bamboo_rod", () -> new RollFishingRodItem(1, 48, new Item.Properties(),
                RollFishingRodItem.Perk.REGROWS, RollFishingRodItem.Perk.NATURAL_BAIT));
        CARBON_FIBER = items.register("carbon_fiber_rod", () -> new RollFishingRodItem(2, 160, new Item.Properties(),
                RollFishingRodItem.Perk.DOUBLE_CATCH, RollFishingRodItem.Perk.SPARES_WEAR));
        METEORITE_METAL = items.register("meteorite_metal_fishing_rod",
                () -> new RollFishingRodItem(3, 640, new Item.Properties().fireResistant(),
                        // Lava fishing comes from the tide:lava_fishing_rods tag, and Tide's own tooltip already says so.
                        RollFishingRodItem.Perk.STAR_XP, RollFishingRodItem.Perk.STARFALL)
                        // Like the other meteorite metal items: the deep blue name, a light blue lore line.
                        .looks(com.roll_54.roll_mod.util.TooltipOptions.nameAndLore(0x3B2AB8, 2, 0x005acf)));
        ELECTRIC = items.register("electric_fishing_rod", () -> new ElectricFishingRodItem(3, new Item.Properties()));
        // A hook as Tide's own are: listed in tide:hooks, its line in the rod's tooltip, the bobber's hook drawn from
        // assets/tide/textures/entity/fishing_hook/meteorite_hook.png.
        METEORITE_HOOK = items.register("meteorite_hook", () -> new com.li64.tide.registries.items.FishingHookItem(
                new Item.Properties().stacksTo(1).fireResistant(), "item.roll_mod.meteorite_hook.desc"));
        // The catch perks, on Tide's catch event.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(TideRodPerks.class);
    }

    public static List<DeferredHolder<Item, Item>> all() {
        return BAMBOO == null ? List.of() : List.of(BAMBOO, CARBON_FIBER, METEORITE_METAL, ELECTRIC);
    }
}
