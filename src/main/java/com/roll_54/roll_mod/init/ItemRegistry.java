package com.roll_54.roll_mod.init;

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

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
