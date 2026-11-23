package com.roll_54.roll_mod.compat.sophisticated;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem.TierUpgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The tank-only tier upgrades, {@code roll_mod:<from>_to_<to>_tank_upgrade}: one for every tank step Sophisticated
 * Storage has ({@code basic_to_gold} is wood to the aluminium tank, and so on). Registered only when Sophisticated
 * Storage is loaded ({@code ItemRegistry.register} checks), since the items extend its class.
 *
 * <p>No recipes here: how they are obtained is up to the pack.
 */
public final class TankUpgrades {

    /**
     * SS's tank steps, by name: every tier upgrade that has a tank in its table. Names, not the enum's constants —
     * {@link TierUpgrade}'s own initialiser reads Sophisticated Storage's blocks, which are not registered yet while
     * mods are being constructed; the constant is looked up only when the item is made, after blocks are registered.
     */
    public static final List<String> TIERS = List.of(
            "BASIC_TO_COPPER", "BASIC_TO_IRON", "BASIC_TO_GOLD", "BASIC_TO_DIAMOND", "BASIC_TO_NETHERITE",
            "COPPER_TO_IRON", "COPPER_TO_GOLD", "COPPER_TO_DIAMOND", "COPPER_TO_NETHERITE",
            "IRON_TO_GOLD", "IRON_TO_DIAMOND", "IRON_TO_NETHERITE",
            "GOLD_TO_DIAMOND", "GOLD_TO_NETHERITE",
            "DIAMOND_TO_NETHERITE");

    public static final List<DeferredHolder<Item, Item>> ITEMS = new ArrayList<>();

    private TankUpgrades() {}

    /** {@code basic_to_gold_tank_upgrade} for {@code BASIC_TO_GOLD}. */
    public static String id(String tier) {
        return tier.toLowerCase(Locale.ROOT) + "_tank_upgrade";
    }

    public static void register(DeferredRegister<Item> items) {
        for (String tier : TIERS) {
            ITEMS.add(items.register(id(tier), () -> new TankTierUpgradeItem(TierUpgrade.valueOf(tier))));
        }
    }
}
