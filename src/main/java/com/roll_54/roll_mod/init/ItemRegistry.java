package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.ModArmor.HazmatBootsItem;
import com.roll_54.roll_mod.ModArmor.ModArmorMaterials;
import com.roll_54.roll_mod.ModItems.ModToolTiers;
import com.roll_54.roll_mod.ModItems.TooltipArmorItem;
import com.roll_54.roll_mod.ModItems.TooltipItem;
import com.roll_54.roll_mod.Roll_mod;
import com.roll_54.roll_mod.Util.TooltipOptions;
import com.roll_54.roll_mod.Util.TooltipManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ItemRegistry {

    private ItemRegistry() {
    }
    private static final int AQUA = 0x55FFFF;       // §b
    private static final int YELLOW = 0xFFFF55;     // §e
    private static final int LIGHT_PURPLE = 0xFF55FF; // §d
    private static final int RED = 0xFF5555;        // §c
    private static final int BLUE = 0x5555FF;       // §9
    private static final int GREEN = 0x55FF55;


    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Roll_mod.MODID);


    public static final DeferredHolder<Item, Item> COPPER_GEAR = ITEMS.register("copper_gear", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CHEMICAL_CORE = ITEMS.register("chemical_core", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> SUPERSTEEL_GEAR = ITEMS.register(
            "supersteel_gear",
            () -> new TooltipItem.Builder()
                    .props(new Item.Properties())
                    .tooltipLines(2)
                    .nameColor(0xFF8C00)
                    .loreColor(0xE0B000)
                    .build()
    );
    public static final DeferredHolder<Item, Item> SUPER_CIRCUIT = ITEMS.register(
            "super_circuit",
            () -> new TooltipItem.Builder()
                    .props(new Item.Properties())
                    .tooltipLines(3)
                    .nameColor(0x00C8A0)
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
                    new Item.Properties().stacksTo(1).durability(2400)
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
                    new Item.Properties().stacksTo(1).durability(2400)
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
                    new Item.Properties().stacksTo(1).durability(2400)
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
                    new Item.Properties().stacksTo(1).durability(2400)
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
                    new Item.Properties().stacksTo(1).durability(3400)
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
                    new Item.Properties().stacksTo(1).durability(3400)
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
                    new Item.Properties().stacksTo(1).durability(3400)
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
                    new Item.Properties().stacksTo(1).durability(3400)
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
                    new Item.Properties().durability(3400),
                    TooltipOptions.nameAndLore(0x3B2AB8, 1, 0x005ACF)
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_PICKAXE = ITEMS.register(
            "meteorite_pickaxe",
            () -> new TooltipManager.TooltipPickaxeItem(
                    ModToolTiers.METEORITE_METAL, 6f, -2.8f,
                    new Item.Properties(),
                    TooltipOptions.nameAndLore(0x3B2AB8, 1, 0x005ACF)
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_AXE = ITEMS.register(
            "meteorite_axe",
            () -> new TooltipManager.TooltipAxeItem(
                    ModToolTiers.METEORITE_METAL, 11f, -3.0f,
                    new Item.Properties(),
                    TooltipOptions.nameAndLore(0x3B2AB8, 1, 0x005ACF)
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_SHOVEL = ITEMS.register(
            "meteorite_shovel",
            () -> new TooltipManager.TooltipShovelItem(
                    ModToolTiers.METEORITE_METAL, 5f, -3.0f,
                    new Item.Properties(),
                    TooltipOptions.nameAndLore(0x3B2AB8, 1, 0x005ACF)
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_HOE = ITEMS.register(
            "meteorite_hoe",
            () -> new TooltipManager.TooltipHoeItem(
                    ModToolTiers.METEORITE_METAL, 2f, -1.0f,
                    new Item.Properties(),
                    TooltipOptions.nameAndLore(0x3B2AB8, 1, 0x005ACF)
            )
    );
    // imported items from KUBEJS
    private static final int AQUA = 0x55FFFF;
    private static final int YELLOW = 0xFFFF55;
    private static final int LIGHT_PURPLE = 0xFF55FF;
    private static final int GREEN = 0x55FF55;
    private static final int BLUE = 0x5555FF;
    private static final int RED = 0xFF5555;

    // === Плати та компоненти ===
    public static final DeferredHolder<Item, Item> FR2_SHEET = ITEMS.register(
            "fr2_sheet", () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> FR4_SHEET = ITEMS.register(
            "fr4_sheet", () -> new Item(new Item.Properties())
    );

    // SMD (тільки колір назви)
    public static final DeferredHolder<Item, Item> SMD_RESISTOR = ITEMS.register(
            "smd_resistor", () -> new SimpleTooltipItem(new Item.Properties(), 0, AQUA, null)
    );
    public static final DeferredHolder<Item, Item> SMD_CAPACITOR = ITEMS.register(
            "smd_capacitor", () -> new SimpleTooltipItem(new Item.Properties(), 0, AQUA, null)
    );
    public static final DeferredHolder<Item, Item> SMD_DIODE = ITEMS.register(
            "smd_diode", () -> new SimpleTooltipItem(new Item.Properties(), 0, AQUA, null)
    );
    public static final DeferredHolder<Item, Item> SMD_TRANSISTOR = ITEMS.register(
            "smd_transistor", () -> new SimpleTooltipItem(new Item.Properties(), 0, AQUA, null)
    );

    // Особливий — просто предмет (текстуру підкинеш у ресурси)
    public static final DeferredHolder<Item, Item> AMETHYST_OSCILLATOR = ITEMS.register(
            "amethyst_oscillator", () -> new Item(new Item.Properties())
    );

    // Дзеркало з жовтою назвою
    public static final DeferredHolder<Item, Item> MIRROR = ITEMS.register(
            "mirror", () -> new SimpleTooltipItem(new Item.Properties(), 0, YELLOW, null)
    );

    // === T2 (фіолетові) ===
    public static final DeferredHolder<Item, Item> PURPLE_BOULE = ITEMS.register(
            "purple_boule", () -> new SimpleTooltipItem(new Item.Properties(), 1, LIGHT_PURPLE, LIGHT_PURPLE) // lore line1
    );
    public static final DeferredHolder<Item, Item> PURPLE_WAFER = ITEMS.register(
            "purple_wafer", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_NOR = ITEMS.register(
            "purple_wafer_nor", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_NOR_CHIP = ITEMS.register(
            "purple_wafer_nor_chip", () -> new SimpleTooltipItem(new Item.Properties(), 1, LIGHT_PURPLE, LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_NOT = ITEMS.register(
            "purple_wafer_not", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_NOT_CHIP = ITEMS.register(
            "purple_wafer_not_chip", () -> new SimpleTooltipItem(new Item.Properties(), 1, LIGHT_PURPLE, LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_OR = ITEMS.register(
            "purple_wafer_or", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_OR_CHIP = ITEMS.register(
            "purple_wafer_or_chip", () -> new SimpleTooltipItem(new Item.Properties(), 1, LIGHT_PURPLE, LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_XNOR = ITEMS.register(
            "purple_wafer_xnor", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_XNOR_CHIP = ITEMS.register(
            "purple_wafer_xnor_chip", () -> new SimpleTooltipItem(new Item.Properties(), 1, LIGHT_PURPLE, LIGHT_PURPLE)
    );

    // === T1 (сині/звичайні) ===
    public static final DeferredHolder<Item, Item> BLUE_BOULE = ITEMS.register(
            "blue_boule", () -> new SimpleTooltipItem(new Item.Properties(), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> BLUE_WAFER = ITEMS.register(
            "blue_wafer", () -> new SimpleTooltipItem(new Item.Properties(), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> BLUE_WAFER_RAM = ITEMS.register(
            "blue_wafer_ram", () -> new SimpleTooltipItem(new Item.Properties(), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> BLUE_WAFER_RAM_CHIP = ITEMS.register(
            "blue_wafer_ram_chip", () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> BLUE_WAFER_NOR = ITEMS.register(
            "blue_wafer_nor", () -> new SimpleTooltipItem(new Item.Properties(), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> BLUE_WAFER_NOR_CHIP = ITEMS.register(
            "blue_wafer_nor_chip", () -> new SimpleTooltipItem(new Item.Properties(), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> BLUE_WAFER_SOC = ITEMS.register(
            "blue_wafer_soc", () -> new SimpleTooltipItem(new Item.Properties(), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> BLUE_WAFER_SOC_CHIP = ITEMS.register(
            "blue_wafer_soc_chip", () -> new SimpleTooltipItem(new Item.Properties(), 1, null, null)
    );

    // Нано
    public static final DeferredHolder<Item, Item> NANO_WAFER = ITEMS.register(
            "nano_wafer", () -> new SimpleTooltipItem(new Item.Properties(), 1, AQUA, AQUA) // lore: «Підтримує 1.54 nm…»
    );
    public static final DeferredHolder<Item, Item> NANO_CHIP = ITEMS.register(
            "nano_chip", () -> new SimpleTooltipItem(new Item.Properties(), 0, AQUA, null)
    );

    // Програмовані плати
    public static final DeferredHolder<Item, Item> CIRCUIT_1 = ITEMS.register("1_circuit", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CIRCUIT_2 = ITEMS.register("2_circuit", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CIRCUIT_3 = ITEMS.register("3_circuit", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CIRCUIT_4 = ITEMS.register("4_circuit", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CIRCUIT_5 = ITEMS.register("5_circuit", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CIRCUIT_6 = ITEMS.register("6_circuit", () -> new Item(new Item.Properties()));

    // Пластмаси
    public static final DeferredHolder<Item, Item> PVC_INGOT = ITEMS.register("pvc_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PVC_PLATE = ITEMS.register("pvc_plate", () -> new Item(new Item.Properties()));

    // «Покращений MI»
    public static final DeferredHolder<Item, Item> ADV_CONVEYOR = ITEMS.register("advanced_conveyor", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ADV_PISTON = ITEMS.register("advanced_piston", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ADV_ROBOT_ARM = ITEMS.register("advanced_robot_arm", () -> new Item(new Item.Properties()));

    // Космос
    public static final DeferredHolder<Item, Item> MIXED_COSMIC_T2 = ITEMS.register("mixed_cosmic_t2", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NOSE_MK1 = ITEMS.register("nose_mk1", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NOSE_MK2 = ITEMS.register("nose_mk2", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NOSE_MK3 = ITEMS.register("nose_mk3", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NOSE_MK4 = ITEMS.register("nose_mk4", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TANK_MK1 = ITEMS.register("tank_mk1", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TANK_MK2 = ITEMS.register("tank_mk2", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TANK_MK3 = ITEMS.register("tank_mk3", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TANK_MK4 = ITEMS.register("tank_mk4", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENGINE_MK1 = ITEMS.register("engine_mk1", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENGINE_MK2 = ITEMS.register("engine_mk2", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENGINE_MK3 = ITEMS.register("engine_mk3", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENGINE_MK4 = ITEMS.register("engine_mk4", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FIN_MK1 = ITEMS.register("fin_mk1", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FIN_MK2 = ITEMS.register("fin_mk2", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FIN_MK3 = ITEMS.register("fin_mk3", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FIN_MK4 = ITEMS.register("fin_mk4", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> SENSOR = ITEMS.register("sensor", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SATELLITE = ITEMS.register(
            "satellite", () -> new TooltipItem(new Item.Properties(), 0, AQUA, null)
    );

    // «Дослідження»
    public static final DeferredHolder<Item, Item> RESEARCH_HET = ITEMS.register(
            "research_het", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_ROCKET_2 = ITEMS.register(
            "research_rocket_2", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_ROCKET_3 = ITEMS.register(
            "research_rocket_3", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_ROCKET_4 = ITEMS.register(
            "research_rocket_4", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );

    // Броня (заготовки, стак по 1, з тултіпом)
    public static final DeferredHolder<Item, Item> GRAVIK_CASING = ITEMS.register("gravik_casing", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_QUANTUM_HELMET = ITEMS.register(
            "raw_quantum_helmet", () -> new SimpleTooltipItem(new Item.Properties().stacksTo(1), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> RAW_QUANTUM_CHESTPLATE = ITEMS.register(
            "raw_quantum_chestplate", () -> new SimpleTooltipItem(new Item.Properties().stacksTo(1), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> RAW_QUANTUM_LEGGINGS = ITEMS.register(
            "raw_quantum_leggings", () -> new SimpleTooltipItem(new Item.Properties().stacksTo(1), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> RAW_QUANTUM_BOOTS = ITEMS.register(
            "raw_quantum_boots", () -> new SimpleTooltipItem(new Item.Properties().stacksTo(1), 1, null, null)
    );

    // Ін’єкції
    public static final DeferredHolder<Item, Item> SYRINGE = ITEMS.register("syringe", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> INJECTION_REGEN = ITEMS.register(
            "injection_regen", () -> new SimpleTooltipItem(new Item.Properties(), 1, GREEN, GREEN)
    );
    public static final DeferredHolder<Item, Item> INJECTION_RESISTANCE = ITEMS.register(
            "injection_resistance", () -> new SimpleTooltipItem(new Item.Properties(), 0, GREEN, null)
    );
    public static final DeferredHolder<Item, Item> INJECTION_SPEED = ITEMS.register(
            "injection_speed", () -> new SimpleTooltipItem(new Item.Properties(), 0, GREEN, null)
    );
    public static final DeferredHolder<Item, Item> INJECTION_FIRE_RES = ITEMS.register(
            "injection_fire_res", () -> new SimpleTooltipItem(new Item.Properties(), 0, GREEN, null)
    );

    // Інше / хімія
    public static final DeferredHolder<Item, Item> CALCIUM_OXIDE = ITEMS.register("calcium_oxide", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CALCIUM_HYDROXIDE = ITEMS.register("calcium_hydroxide", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENERGIUM_DUST = ITEMS.register("energium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENERGIUM_TINY_CRYSTAL = ITEMS.register("energium_tiny_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENERGIUM_SMALL_CRYSTAL = ITEMS.register("energium_small_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENERGIUM_NORMAL_CRYSTAL = ITEMS.register("energium_normal_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENERGIUM_LARGE_CRYSTAL = ITEMS.register("energium_large_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENERGIUM_SMALL_DIRTY_CRYSTAL = ITEMS.register("energium_small_dirty_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENERGIUM_NORMAL_DIRTY_CRYSTAL = ITEMS.register("energium_normal_dirty_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENERGIUM_LARGE_DIRTY_CRYSTAL = ITEMS.register("energium_large_dirty_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ALUMINA_DUST = ITEMS.register("alumina_dust", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> LAPOTRON_LARGE_CRYSTAL = ITEMS.register("lapotron_large_crystal", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LAPOTRON_LENS = ITEMS.register("lapotron_lens", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LAPOTRON_LASER = ITEMS.register("lapotron_laser", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> SUPERCONDUCTING_MAGNET_BASE = ITEMS.register("superconducting_magnet_base", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUPERCONDUCTING_MAGNET = ITEMS.register("superconducting_magnet", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUPERCONDUCTING_MODULE = ITEMS.register("superconducting_module", () -> new Item(new Item.Properties()));

    // Глоу (foil завжди)
    public static final DeferredHolder<Item, Item> CHUNKLOADER_CORE = ITEMS.register(
            "chunkloader_core",
            () -> new Item(new Item.Properties()) {
                @Override public boolean isFoil(ItemStack stack) { return true; }
            }
    );

    // Їжа — “Золотий батон”
    public static final DeferredHolder<Item, Item> GOLDEN_BATON = ITEMS.register(
            "golden_baton",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(5)
                            .saturationMod(3f)
                            .alwaysEat()
                            .fast()
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3000, 0), 1.0f)
                            .build()
            ))
    );

    // «Кінець» (синя назва)
    public static final DeferredHolder<Item, Item> NONUB = ITEMS.register(
            "nonub", () -> new SimpleTooltipItem(new Item.Properties(), 0, BLUE, null)
    );

    // Сонячні панелі (скло)
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK1 = ITEMS.register("sunnarium_glass_mk1", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK2 = ITEMS.register("sunnarium_glass_mk2", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK3 = ITEMS.register("sunnarium_glass_mk3", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK4 = ITEMS.register("sunnarium_glass_mk4", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK5 = ITEMS.register("sunnarium_glass_mk5", () -> new Item(new Item.Properties()));

    // Старі крафти / руда-хімія
    public static final DeferredHolder<Item, Item> RAW_FLUORITE = ITEMS.register("raw_fluorite", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FLUORITE_TINY_DUST = ITEMS.register("fluorite_tiny_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FLUORITE_DUST = ITEMS.register("fluorite_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SODIUM_BISULFATE = ITEMS.register("sodium_bisulfate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RUTILE_DUST = ITEMS.register("rutile_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RUTILE_DUST_SMALL = ITEMS.register("rutile_dust_small", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ILMINITE_DUST = ITEMS.register("ilminite_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RUTILE_IRON = ITEMS.register("rutile_iron", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> CALCIUM_DUST = ITEMS.register("calcium_dust", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> VANADIUM_DUST = ITEMS.register("vanadium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> VANADIUM_DUST_CLEAN = ITEMS.register("vanadium_dust_clean", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NETHER_STAR_DUST = ITEMS.register("nether_star_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_SHELDONITE = ITEMS.register("raw_sheldonite", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PLATINUM_GROUP_SLUDGE = ITEMS.register("platinum_group_sludge", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_PLATINUM_DUST = ITEMS.register("raw_platinum_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_PALLADIUM_DUST = ITEMS.register("raw_palladium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AMMONIUM_CHLORIDE_DUST = ITEMS.register("ammonium_chloride_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> INERT_METAL_MIXTURE = ITEMS.register("inert_metal_mixture", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RUTHENIUM_TETROXIDE_DUST = ITEMS.register("ruthenium_tetroxide_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAREST_METAL_DUST = ITEMS.register("rarest_metal_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRIDIUM_METAL_RESIDUE = ITEMS.register("iridium_metal_residue", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRIDIUM_CHLORIDE = ITEMS.register("iridium_chloride", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> PLATINUM_SLUDGE_RESIDUE = ITEMS.register("platinum_sludge_residue", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> GRAVI_ENGINE_MK_1 = ITEMS.register("gravi_engine_mk_1", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RUTHENIUM_COIN = ITEMS.register(
            "ruthenium_coin", () -> new SimpleTooltipItem(new Item.Properties(), 1, null, null)
    );
    public static final DeferredHolder<Item, Item> RAW_PYROCHLORE = ITEMS.register("raw_pyrochlore", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PYROCHLORE_DUST = ITEMS.register("pyrochlore_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS = ITEMS.register(
            "sunnarium_glass", () -> new SimpleTooltipItem(new Item.Properties(), 0, AQUA, null)
    );
    public static final DeferredHolder<Item, Item> MINESTAR_LOGO = ITEMS.register(
            "minestar_logo", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );
    public static final DeferredHolder<Item, Item> ENRICHED_SUNNARIUM = ITEMS.register("enriched_sunnarium", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENRICHED_SUNNARIUM_MK2 = ITEMS.register("enriched_sunnarium_mk2", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> RAW_CRYSTAL_CHIP = ITEMS.register(
            "raw_crystal_chip", () -> new SimpleTooltipItem(new Item.Properties(), 0, GREEN, null)
    );
    public static final DeferredHolder<Item, Item> RAW_CRYSTAL_CHIP_PARTS = ITEMS.register(
            "raw_crystal_chip_parts", () -> new SimpleTooltipItem(new Item.Properties(), 0, GREEN, null)
    );
    public static final DeferredHolder<Item, Item> ENGRAVED_CRYSTAL_CHIP = ITEMS.register(
            "engraved_crystal_chip", () -> new SimpleTooltipItem(new Item.Properties(), 0, GREEN, null)
    );
    public static final DeferredHolder<Item, Item> EMITTER = ITEMS.register("emitter", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> QUARTZ_LAMP = ITEMS.register("quartz_lamp", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> PETRI_DISH = ITEMS.register("petri_dish", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WETWERE_CIRCUIT_BOARD = ITEMS.register(
            "wetwere_circuit_board", () -> new SimpleTooltipItem(new Item.Properties(), 0, RED, null)
    );
    public static final DeferredHolder<Item, Item> WETWERE_PRINTED_CIRCUIT_BOARD = ITEMS.register(
            "wetwere_printed_circuit_board", () -> new SimpleTooltipItem(new Item.Properties(), 0, RED, null)
    );
    public static final DeferredHolder<Item, Item> NEURO_PROCESSING_UNIT = ITEMS.register(
            "neuro_processing_unit", () -> new SimpleTooltipItem(new Item.Properties(), 1, RED, RED)
    );
    public static final DeferredHolder<Item, Item> WETWERE_CIRCUIT = ITEMS.register(
            "wetwere_circuit", () -> new SimpleTooltipItem(new Item.Properties(), 1, RED, RED)
    );
    public static final DeferredHolder<Item, Item> IRIDIUM_DIOXIDE_DUST = ITEMS.register("iridium_dioxide_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRIDIUM_METAL_RESIDUE_DUST = ITEMS.register("iridium_metal_residue_dust", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> SKULK_SPORES = ITEMS.register(
            "skulk_spores", () -> new SimpleTooltipItem(new Item.Properties(), 0, 0xAA00AA, null)
    );
    public static final DeferredHolder<Item, Item> AGAR_GEL = ITEMS.register("agar_gel", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AGAR_DUST = ITEMS.register("agar_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PETRI_DISH_SKULK = ITEMS.register(
            "petri_dish_skulk", () -> new SimpleTooltipItem(new Item.Properties(), 1, LIGHT_PURPLE, LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> STEM_CELLS = ITEMS.register(
            "stem_cells", () -> new SimpleTooltipItem(new Item.Properties(), 0, AQUA, null)
    );
    public static final DeferredHolder<Item, Item> NEURON_CELLS = ITEMS.register(
            "neuron_cells", () -> new SimpleTooltipItem(new Item.Properties(), 0, LIGHT_PURPLE, null)
    );

    public static final DeferredHolder<Item, Item> SODIUM_SULFATE = ITEMS.register("sodium_sulfate", () -> new Item(new Item.Properties()));
    // Англомовний нейм лишив як є (з KubeJS)
    public static final DeferredHolder<Item, Item> MINING_DRONE = ITEMS.register("mining_drone", () -> new Item(new Item.Properties()));
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}