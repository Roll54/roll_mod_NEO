package com.roll_54.roll_mod.compat.aae;

import appeng.core.definitions.ItemDefinition;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.pedroksl.advanced_ae.common.items.upgrades.QuantumUpgradeBaseItem;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;

/**
 * The oxygen tank card, a real Advanced AE quantum armour card: it goes into the chestplate through
 * AAE's own card slot (with AAE's progress bar), shows up in AAE's upgrade list, and can be switched
 * on and off there like every other card. While on, the chestplate's oxygen tank is x4
 * ({@code NanoSuitUtils#quantumTankSize}).
 *
 * <p>AAE's cards are the values of the {@link UpgradeType} enum, which has no extension API, so
 * {@code mixin.aae.UpgradeTypeMixin} appends {@link #TYPE} to it when the enum loads. That can happen
 * while AAE itself is being constructed, so this class holds its own item register (rather than
 * pulling in {@code ItemRegistry}) and is safe to load that early.
 */
public final class AaeOxygenCard {

    /** The enum constant's name; AAE derives its data component ids from it (lower-cased). */
    public static final String TYPE_NAME = "OXYGEN_TANK";
    /** The card's display name in AAE; its lang key is {@code gui.upgrades.advanced_ae.OxygenTankUpgrade}. */
    public static final String DISPLAY_NAME = "Oxygen Tank";
    /** Energy AAE charges for the card while it is on, the same as the Water Breathing card. */
    public static final int COST = 10;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RollMod.MODID);

    public static final DeferredItem<QuantumUpgradeBaseItem> CARD = ITEMS.register("oxygen_tank_card",
            () -> new QuantumUpgradeBaseItem(AaeOxygenCard.TYPE, new Item.Properties().stacksTo(1)));

    public static final ItemDefinition<QuantumUpgradeBaseItem> DEFINITION = new ItemDefinition<>("Oxygen Tank Card", CARD);

    /** Set by {@code UpgradeTypeMixin} as soon as {@link UpgradeType} initialises. */
    public static UpgradeType TYPE;

    private AaeOxygenCard() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
