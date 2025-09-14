package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.ModItems.TooltipItem;
import com.roll_54.roll_mod.Roll_mod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemRegistry {

    private ItemRegistry(){}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Roll_mod.MODID);


    public static final DeferredHolder<Item, Item> COPPER_GEAR = ITEMS.register("copper_gear", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CHEMICAL_CORE = ITEMS.register("chemical_core", () -> new Item(new Item.Properties().stacksTo(16)));
    // 1) supersteel_gear з 2 рядками опису
    public static final DeferredHolder<Item, Item> SUPERSTEEL_GEAR = ITEMS.register("supersteel_gear", () -> new TooltipItem(new Item.Properties(), 2));

    // 2) super_circuit з 3 рядками опису
    public static final DeferredHolder<Item, Item> SUPER_CIRCUIT = ITEMS.register("super_circuit", () -> new TooltipItem(new Item.Properties(), 3));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
