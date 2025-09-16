package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.ModArmor.HazmatBootsItem;
import com.roll_54.roll_mod.ModArmor.ModArmorMaterials;
import com.roll_54.roll_mod.ModItems.ModToolTiers;
import com.roll_54.roll_mod.ModItems.TooltipArmorItem;
import com.roll_54.roll_mod.ModItems.TooltipItem;
import com.roll_54.roll_mod.Roll_mod;
import com.roll_54.roll_mod.Util.TooltipManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.roll_54.roll_mod.init.ItemGroups.TABS;


public class ItemRegistry {

    private ItemRegistry() {
    }

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Roll_mod.MODID);


    public static final DeferredHolder<Item, Item> COPPER_GEAR = ITEMS.register("copper_gear", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CHEMICAL_CORE = ITEMS.register("chemical_core", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> SUPERSTEEL_GEAR = ITEMS.register(
            "supersteel_gear",
            () -> new TooltipItem.Builder()
                    .props(new Item.Properties())
                    .tooltipLines(2)
                    .nameColor(0xFF8C00)  // Dark Orange
                    .loreColor(0xE0B000)  // warm gold
                    .build()
    );
    public static final DeferredHolder<Item, Item> SUPER_CIRCUIT = ITEMS.register(
            "super_circuit",
            () -> new TooltipItem.Builder()
                    .props(new Item.Properties())
                    .tooltipLines(3)
                    .nameColor(0x00C8A0)  // teal
                    .loreColor(0x00C8A0)
                    .build()
    );
    public static final DeferredHolder<Item, Item> METEORITE_METAL_INGOT = ITEMS.register(
            "meteorite_metal_ingot",
            () -> new TooltipItem.Builder()
                    .props(new Item.Properties())
                    .tooltipLines(2)
                    .nameColor(0x3B2AB8)
                    .loreColor(0x005acf)
                    .build()
    );


    // БРОНІКИ!!!
    public static final DeferredHolder<Item, Item> HAZMAT_HELMET = ITEMS.register(
            "hazmat_helmet",
            () -> new TooltipArmorItem.Builder(
                    Holder.direct(ModArmorMaterials.HAZMAT_ARMOR.get()),
                    ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1)
            )
                    .tooltipLines(2)
                    .nameColor(0xe8c52a)  // золотий
                    .loreColor(0xc28400)  // яскраво-жовтий
                    .build()
    );

    public static final DeferredHolder<Item, Item> HAZMAT_CHESTPLATE = ITEMS.register(
            "hazmat_chestplate",
            () -> new TooltipArmorItem.Builder(
                    Holder.direct(ModArmorMaterials.HAZMAT_ARMOR.get()),
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1)
            )
                    .tooltipLines(2)
                    .nameColor(0xe8c52a)
                    .loreColor(0xc28400)
                    .build()
    );

    public static final DeferredHolder<Item, Item> HAZMAT_LEGGINGS = ITEMS.register(
            "hazmat_leggings",
            () -> new TooltipArmorItem.Builder(
                    Holder.direct(ModArmorMaterials.HAZMAT_ARMOR.get()),
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().stacksTo(1)
            )
                    .tooltipLines(1)
                    .nameColor(0xe8c52a)
                    .loreColor(0xc28400)
                    .build()
    );

    public static final DeferredHolder<Item, Item> HAZMAT_BOOTS = ITEMS.register(
            "hazmat_boots",
            () -> new HazmatBootsItem.Builder(
                    Holder.direct(ModArmorMaterials.HAZMAT_ARMOR.get()),
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1)
            )
                    .tooltipLines(2)
                    .nameColor(0xe8c52a)
                    .loreColor(0xc28400)
                    .build()
    );

    public static final DeferredHolder<Item, Item> METEORITE_HELMET = ITEMS.register(
            "meteorite_helmet",
            () -> new TooltipArmorItem.Builder(
                    Holder.direct(ModArmorMaterials.METEORITE_ARMOR.get()),
                    ArmorItem.Type.HELMET,
                    new Item.Properties().stacksTo(1)
            )
                    .tooltipLines(2)
                    .nameColor(0x3B2AB8)  // глибокий синій
                    .loreColor(0x005acf)  // світло-біло-блакитний
                    .build()
    );

    public static final DeferredHolder<Item, Item> METEORITE_CHESTPLATE = ITEMS.register(
            "meteorite_chestplate",
            () -> new TooltipArmorItem.Builder(
                    Holder.direct(ModArmorMaterials.METEORITE_ARMOR.get()),
                    ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1)
            )
                    .tooltipLines(2)
                    .nameColor(0x3B2AB8)
                    .loreColor(0x005acf)
                    .build()
    );

    public static final DeferredHolder<Item, Item> METEORITE_LEGGINGS = ITEMS.register(
            "meteorite_leggings",
            () -> new TooltipArmorItem.Builder(
                    Holder.direct(ModArmorMaterials.METEORITE_ARMOR.get()),
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().stacksTo(1)
            )
                    .tooltipLines(2)
                    .nameColor(0x3B2AB8)
                    .loreColor(0x005acf)
                    .build()
    );

    public static final DeferredHolder<Item, Item> METEORITE_BOOTS = ITEMS.register(
            "meteorite_boots",
            () -> new TooltipArmorItem.Builder(
                    Holder.direct(ModArmorMaterials.METEORITE_ARMOR.get()),
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1)
            )
                    .tooltipLines(2)
                    .nameColor(0x3B2AB8)
                    .loreColor(0x005acf)
                    .build()
    );

    public static final DeferredHolder<Item, Item> METEORITE_SWORD = ITEMS.register(
            "meteorite_sword",
            () -> new TooltipManager.TooltipSwordItem(
                    ModToolTiers.METEORITE_METAL, 12f, -2.4f,
                    new Item.Properties(),
                    2,                // 2 рядки лору
                    0x3B2AB8,         // колір назви
                    0x005ACF          // колір лору
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_PICKAXE = ITEMS.register(
            "meteorite_pickaxe",
            () -> new TooltipManager.TooltipPickaxeItem(
                    ModToolTiers.METEORITE_METAL, 6f, -2.8f,
                    new Item.Properties(),
                    2,
                    0x3B2AB8,
                    0x005ACF
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_AXE = ITEMS.register(
            "meteorite_axe",
            () -> new TooltipManager.TooltipAxeItem(
                    ModToolTiers.METEORITE_METAL, 11f, -3.0f,
                    new Item.Properties(),
                    2,
                    0x3B2AB8,
                    0x005ACF
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_SHOVEL = ITEMS.register(
            "meteorite_shovel",
            () -> new TooltipManager.TooltipShovelItem(
                    ModToolTiers.METEORITE_METAL, 5f, -3.0f,
                    new Item.Properties(),
                    2,
                    0x3B2AB8,
                    0x005ACF
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_HOE = ITEMS.register(
            "meteorite_hoe",
            () -> new TooltipManager.TooltipHoeItem(
                    ModToolTiers.METEORITE_METAL, 2f, -1.0f,
                    new Item.Properties(),
                    2,
                    0x3B2AB8,
                    0x005ACF
            )
    );


    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}