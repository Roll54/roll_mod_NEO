package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.modArmor.HazmatBootsItem;
import com.roll_54.roll_mod.modArmor.ModArmorMaterials;
import com.roll_54.roll_mod.modItems.*;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.util.TooltipOptions;
import com.roll_54.roll_mod.util.TooltipManager;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

import static com.roll_54.roll_mod.init.BlockRegistry.*;


public class ItemRegistry {


    public static final ResourceKey<MobEffect> NOURISHMENT =
            ResourceKey.create(Registries.MOB_EFFECT,

                    ResourceLocation.fromNamespaceAndPath("farmersdelight", "nourishment"));
    public static final Supplier<Holder<MobEffect>> NOURISHMENT_HOLDER =
            () -> BuiltInRegistries.MOB_EFFECT.getHolderOrThrow(NOURISHMENT);

    private ItemRegistry() {
    }
    private static final int AQUA = 0x55FFFF;// §b
    private static final int SKULK_BLUE = 0x19c8ff; // my own
    private static final int YELLOW = 0xFFFF55;     // §e
    private static final int LIGHT_PURPLE = 0xFF55FF; // §d
    private static final int RED = 0xFF5555;        // §c
    private static final int BLUE = 0x5555FF;       // §9
    private static final int GREEN = 0x55FF55;
    private static final int LIGHT_GREN = 0x73ff85;



    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, RollMod.MODID);


    private static DeferredHolder<Item, Item> registerSimple(String name) {
        return registerSimple(name, new Item.Properties());
    }

    private static DeferredHolder<Item, Item> registerSimple(String name, Item.Properties props) {
        return ITEMS.register(name, () -> new Item(props));
    }

    private static DeferredHolder<Item, Item> registerTooltip(String name, TooltipOptions opts) {
        return registerTooltip(name, new Item.Properties(), opts);
    }

    private static DeferredHolder<Item, Item> registerTooltip(String name, Item.Properties props, TooltipOptions opts) {
        return ITEMS.register(name, () -> new TooltipManager.TooltipItem(props, opts));
    }


    public static final DeferredHolder<Item, Item> COPPER_GEAR = registerSimple("copper_gear");
    public static final DeferredHolder<Item, Item> CHEMICAL_CORE = registerSimple("chemical_core", new Item.Properties().stacksTo(16));

    public static final DeferredHolder<Item, Item> SUPERSTEEL_GEAR = registerTooltip(
            "supersteel_gear",
            new TooltipOptions(2, 0xFF8C00, 0xE0B000, false)
    );
    public static final DeferredHolder<Item, Item> SUPER_CIRCUIT = registerTooltip(
            "super_circuit",
            new TooltipOptions(3, 0x00C8A0, 0x00C8A0, false)
    );
    public static final DeferredHolder<Item, Item> METEORITE_METAL_INGOT = registerTooltip(
            "meteorite_metal_ingot",
            new TooltipOptions(2, 0x3B2AB8, 0x005acf, false)
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
            () -> new HazmatBootsItem(
                    Holder.direct(ModArmorMaterials.HAZMAT_ARMOR.get()),
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1).durability(2400),
                    TooltipOptions.nameAndLore(0xe8c52a, 2, 0xc28400)
            )
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

    // === Плати та компоненти ===
    public static final DeferredHolder<Item, Item> FR2_SHEET = ITEMS.register(
            "fr2_sheet", () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> FR4_SHEET = ITEMS.register(
            "fr4_sheet", () -> new Item(new Item.Properties())
    );

    // SMD (тільки колір назви)
    public static final DeferredHolder<Item, Item> SMD_RESISTOR = registerTooltip(
            "smd_resistor", TooltipOptions.name(AQUA)
    );
    public static final DeferredHolder<Item, Item> SMD_CAPACITOR = registerTooltip(
            "smd_capacitor", TooltipOptions.name(AQUA)
    );
    public static final DeferredHolder<Item, Item> SMD_DIODE = registerTooltip(
            "smd_diode", TooltipOptions.name(AQUA)
    );
    public static final DeferredHolder<Item, Item> SMD_TRANSISTOR = registerTooltip(
            "smd_transistor", TooltipOptions.name(AQUA)
    );

    // Особливий — просто предмет (текстуру підкинеш у ресурси)
    public static final DeferredHolder<Item, Item> AMETHYST_OSCILLATOR = ITEMS.register(
            "amethyst_oscillator", () -> new Item(new Item.Properties())
    );

    // Дзеркало з жовтою назвою
    public static final DeferredHolder<Item, Item> MIRROR = registerTooltip(
            "mirror", TooltipOptions.name(LIGHT_GREN)
    );

    // === T3 (фіолетові) ===
    public static final DeferredHolder<Item, Item> PURPLE_BOULE = registerTooltip("purple_boule", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER = registerTooltip("purple_wafer", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_NOR = registerTooltip("purple_wafer_nor", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_NOR_CHIP = registerTooltip("purple_wafer_nor_chip", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_NOT = registerTooltip("purple_wafer_not", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_NOT_CHIP = registerTooltip("purple_wafer_not_chip", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_OR = registerTooltip("purple_wafer_or", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_OR_CHIP = registerTooltip("purple_wafer_or_chip", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_XNOR = registerTooltip("purple_wafer_xnor", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_XNOR_CHIP = registerTooltip("purple_wafer_xnor_chip", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_XOR = registerTooltip("purple_wafer_xor", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_XOR_CHIP = registerTooltip("purple_wafer_xor_chip", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_RAM = registerTooltip("purple_wafer_ram", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_RAM_CHIP = registerTooltip("purple_wafer_ram_chip", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_SOC = registerTooltip("purple_wafer_soc", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_SOC_CHIP = registerTooltip("purple_wafer_soc_chip", TooltipOptions.name(LIGHT_PURPLE));

    // === T2 (сині) ===
    public static final DeferredHolder<Item, Item> BLUE_BOULE = registerTooltip("blue_boule", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER = registerTooltip("blue_wafer", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_RAM = registerTooltip("blue_wafer_ram", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_RAM_CHIP = registerTooltip("blue_wafer_ram_chip", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_NOR = registerTooltip("blue_wafer_nor", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_NOR_CHIP = registerTooltip("blue_wafer_nor_chip", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_SOC = registerTooltip("blue_wafer_soc", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_SOC_CHIP = registerTooltip("blue_wafer_soc_chip", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_NOT = registerTooltip("blue_wafer_not", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_NOT_CHIP = registerTooltip("blue_wafer_not_chip", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_OR = registerTooltip("blue_wafer_or", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_OR_CHIP = registerTooltip("blue_wafer_or_chip", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_XOR = registerTooltip("blue_wafer_xor", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_XOR_CHIP = registerTooltip("blue_wafer_xor_chip", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_XNOR = registerTooltip("blue_wafer_xnor", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_XNOR_CHIP = registerTooltip("blue_wafer_xnor_chip", TooltipOptions.name(BLUE));

    // Нано
    public static final DeferredHolder<Item, Item> NANO_WAFER = registerTooltip("nano_wafer", TooltipOptions.nameAndLore(AQUA, 1, AQUA));
    public static final DeferredHolder<Item, Item> NANO_CHIP = registerTooltip("nano_chip", TooltipOptions.name(AQUA));
    //T1
    public static final DeferredHolder<Item, Item> STANDARD_WAFER_SOC = ITEMS.register("standard_wafer_soc", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_WAFER = ITEMS.register("standard_wafer", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_WAFER_NOT = ITEMS.register("standard_wafer_not", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_WAFER_OR = ITEMS.register("standard_wafer_or", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_WAFER_XOR = ITEMS.register("standard_wafer_xor", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_WAFER_RAM = ITEMS.register("standard_wafer_ram", () -> new Item(new Item.Properties()));

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
    public static final DeferredHolder<Item, Item> SATELLITE = registerTooltip(
            "satellite", TooltipOptions.name(AQUA)
    );

    // «Дослідження»
    public static final DeferredHolder<Item, Item> RESEARCH_PAPER = registerTooltip(
            "research_paper", TooltipOptions.name(LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_HET = registerTooltip(
            "research_het", TooltipOptions.name(LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_ROCKET_2 = registerTooltip(
            "research_rocket_2", TooltipOptions.name(LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_ROCKET_3 = registerTooltip(
            "research_rocket_3", TooltipOptions.name(LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_ROCKET_4 = registerTooltip(
            "research_rocket_4", TooltipOptions.name(LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_BETTER_PREDICTON = registerTooltip(
            "research_better_predicton", TooltipOptions.name(LIGHT_PURPLE)
    );


    // Броня (заготовки, стак по 1, з тултіпом)
    public static final DeferredHolder<Item, Item> GRAVIK_CASING = ITEMS.register("gravik_casing", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_QUANTUM_HELMET = registerTooltip(
            "raw_quantum_helmet", new Item.Properties().stacksTo(1), new TooltipOptions(1, null, null, false)
    );
    public static final DeferredHolder<Item, Item> RAW_QUANTUM_CHESTPLATE = registerTooltip(
            "raw_quantum_chestplate", new Item.Properties().stacksTo(1), new TooltipOptions(1, null, null, false)
    );
    public static final DeferredHolder<Item, Item> RAW_QUANTUM_LEGGINGS = registerTooltip(
            "raw_quantum_leggings", new Item.Properties().stacksTo(1), new TooltipOptions(1, null, null, false)
    );
    public static final DeferredHolder<Item, Item> RAW_QUANTUM_BOOTS = registerTooltip(
            "raw_quantum_boots", new Item.Properties().stacksTo(1), new TooltipOptions(1, null, null, false)
    );

    // Ін’єкції
    public static final DeferredHolder<Item, Item> SYRINGE = ITEMS.register("syringe", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> INJECTION_REGEN =
            ITEMS.register("injection_regen", () ->
                    new InjectionItem(
                            new Item.Properties(),
                            MobEffects.REGENERATION,
                            null,
                            null,
                            18000,
                            1
                    )
            );

    public static final DeferredHolder<Item, Item> INJECTION_RESISTANCE =
            ITEMS.register("injection_resistance", () ->
                    new InjectionItem(
                            new Item.Properties(),
                            MobEffects.DAMAGE_RESISTANCE,
                            null,
                            null,
                            10000,
                            0
                    )
            );

    public static final DeferredHolder<Item, Item> INJECTION_SPEED =
            ITEMS.register("injection_speed", () ->
                    new InjectionItem(
                            new Item.Properties(),
                            MobEffects.MOVEMENT_SPEED,
                            MobEffects.JUMP,
                            null,
                            18000,
                            1
                    )
            );

    public static final DeferredHolder<Item, Item> INJECTION_FIRE_RESISTANCE =
            ITEMS.register("injection_fire_resistance", () ->
                    new InjectionItem(
                            new Item.Properties(),
                            MobEffects.FIRE_RESISTANCE,
                            ModEffects.SULFUR_RESISTANCE,
                            null,
                            12000,
                            0
                    )
            );

    public static final DeferredHolder<Item, Item> INJECTION_HASTE =
            ITEMS.register("injection_haste", () ->
                    new InjectionItem(
                            new Item.Properties(),
                            MobEffects.DIG_SPEED,
                            null,
                            null,
                            12000,
                            2
                    )
            );


    public static final DeferredHolder<Item, Item> TEST_INST = ITEMS.register( "test_111", () -> new InjectionItem(new Item.Properties(), MobEffects.WITHER, MobEffects.BAD_OMEN, ModEffects.SULFUR_POISONING, 222, 1));

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

    public static final DeferredHolder<Item, Item> CHUNKLOADER_CORE = ITEMS.register(
            "chunkloader_core",
            () -> new Item(new Item.Properties().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))
    );

    public static final DeferredHolder<Item, Item> GOLDEN_BATON = ITEMS.register(
            "golden_baton",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(15)
                            .saturationModifier(3f)
                            .fast()
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 18000, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects. FIRE_RESISTANCE, 30000, 0), 1.0f)
                            .build()
            ))
    );
    public static final DeferredHolder<Item, Item> URANIUM_MUSH =
            ITEMS.register("uranium_mush",
                    () -> new Item(new Item.Properties().stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).component(DataComponents.RARITY, Rarity.EPIC).component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("tooltip.roll_mod.uranium_mush").withStyle(style -> style.withColor(0x33cc4f)))))
                            .food(new FoodProperties.Builder()
                                    .nutrition(12)
                                    .saturationModifier(0.5f)
                                    .effect(() -> new MobEffectInstance(NOURISHMENT_HOLDER.get(), 1200000, 0), 1)
                                    .build())

                    )
            );

    public static final DeferredHolder<Item, Item> HOT_TITANIUM_GUM =
            ITEMS.register("hot_titanium_gum",
                    () -> new Item(new Item.Properties()
                            .food(new FoodProperties.Builder()
                                    .nutrition(6)
                                    .saturationModifier(0.2f)
                                    .fast()
                                    .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 36000, 0), 1f)
                                    .build())
                    )
            );

    public static final DeferredHolder<Item, Item> GOLDEN_POTATO =
            ITEMS.register("golden_potato",
                    () -> new Item(new Item.Properties()
                            .food(new FoodProperties.Builder()
                                    .nutrition(4)
                                    .saturationModifier(0.8f)
                                    .build())
                    )
            );

    public static final DeferredHolder<Item, Item> GOLDEN_POTATO_MUSH =
            ITEMS.register("golden_potato_mush",
                    () -> new Item(new Item.Properties()
                            .food(new FoodProperties.Builder()
                                    .nutrition(12)
                                    .saturationModifier(0.5f)
                                    .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 2), 1)
                                    .effect(() -> new MobEffectInstance(NOURISHMENT_HOLDER.get(), 12000, 0), 1)
                                    .build())
                    )
            );

    public static final DeferredHolder<Item, Item> VERY_NUTRITIOUS_CANDY =
            ITEMS.register("very_nutritious_candy",
                    () -> new Item(new Item.Properties()
                            .stacksTo(64)
                            .component(DataComponents.RARITY, Rarity.RARE)
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                            .component(
                                    DataComponents.LORE,
                                    new ItemLore(List.of(
                                            Component.translatable(
                                                    "tooltip.roll_mod.very_nutritious_candy"
                                            ).withStyle(style -> style.withColor(0xFF66FF))
                                    ))
                            )
                            .food(new FoodProperties.Builder()
                                    .nutrition(12)
                                    .saturationModifier(1.2f)

                                    // Custom nourishment effect
                                    .effect(
                                            () -> new MobEffectInstance(
                                                    NOURISHMENT_HOLDER.get(),
                                                    24000,
                                                    0
                                            ),
                                            1.0f
                                    )

                                    // Vanilla saturation effect
                                    .effect(
                                            () -> new MobEffectInstance(
                                                    MobEffects.SATURATION,
                                                    24000,
                                                    0
                                            ),
                                            1.0f
                                    )
                                    .build())
                    )
            );

    // 🍯 Варення з сірчаних ягід
    public static final DeferredHolder<Item, Item> SULFUR_JAM = ITEMS.register(
            "sulfur_jam",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(6) // легке підживлення
                            .saturationModifier(0.6f)
                            .effect(() -> new MobEffectInstance(MobEffects.POISON, 200, 1), 0.25f) // 25% шанс отруїтись
                            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2000, 0), 1.0f)
                            .fast()
                            .build()
            ))
    );

    // 🥧 Пиріг із сірчаних ягід
    public static final DeferredHolder<Item, Item> SULFUR_BERRY_PIE = ITEMS.register(
            "sulfur_berry_pie",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(12)
                            .saturationModifier(1.2f)
                            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 2400, 2), 1.0f) // дає золоті серця
                            .effect(() -> new MobEffectInstance(ModEffects.SULFUR_POISONING, 600, 0), 0.2f) // 20% шанс отримати твою власну хворобу
                            .effect(() -> new MobEffectInstance(ModEffects.SULFUR_RESISTANCE, 2000, 0), 0.2f)
                            .build()
            ))
    );


    // «Кінець» (синя назва)
    public static final DeferredHolder<Item, Item> NONUB = registerTooltip(
            "nonub", TooltipOptions.name(BLUE)
    );

    // Сонячні панелі (скло)
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK1 = ITEMS.register("sunnarium_glass_mk1", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK2 = ITEMS.register("sunnarium_glass_mk2", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK3 = ITEMS.register("sunnarium_glass_mk3", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK4 = ITEMS.register("sunnarium_glass_mk4", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS_MK5 = ITEMS.register("sunnarium_glass_mk5", () -> new Item(new Item.Properties()));

    // Старі крафти / руда-хімія
    public static final DeferredHolder<Item, Item> SODIUM_BISULFATE = ITEMS.register("sodium_bisulfate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RUTILE_IRON = ITEMS.register("rutile_iron", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CALCIUM_DUST = ITEMS.register("calcium_dust", () -> new Item(new Item.Properties()));
    //old vanadium chemistry
    public static final DeferredHolder<Item, Item> VANADIUM_DUST = ITEMS.register("vanadium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> VANADIUM_DUST_CLEAN = ITEMS.register("vanadium_dust_clean", () -> new Item(new Item.Properties()));
    //New Vanadium chemistry
    public static final DeferredHolder<Item, Item> VANADIUM_SLAG_DUST = registerTooltip("vanadium_slag_dust", TooltipOptions.nameAndLore(0xe0761f, 1, 0xe0761f));

    public static final DeferredHolder<Item, Item> IRON_III_VANADATE = ITEMS.register("iron_iii_vanadate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CALCIUM_METAVANADATE = ITEMS.register("calcium_metavanadate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SODIUM_METAVANADATE = ITEMS.register("sodium_metavanadate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> VANADIUM_PENTOXIDE = ITEMS.register("vanadium_pentoxide", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> VANADIUM_TRIOXIDE = ITEMS.register("vanadium_trioxide", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> SODIUM_CARBONATE = ITEMS.register("sodium_carbonate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CALCIUM_CARBONATE = ITEMS.register("calcium_carbonate", () -> new Item(new Item.Properties()));

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
    public static final DeferredHolder<Item, Item> RUTHENIUM_COIN = registerTooltip(
            "ruthenium_coin", new TooltipOptions(1, null, null, false)
    );
    public static final DeferredHolder<Item, Item> SUNNARIUM_GLASS = registerTooltip(
            "sunnarium_glass", TooltipOptions.name(AQUA)
    );
    public static final DeferredHolder<Item, Item> MINESTAR_LOGO = registerTooltip(
            "minestar_logo", TooltipOptions.name(LIGHT_PURPLE)
    );
    public static final DeferredHolder<Item, Item> ENRICHED_SUNNARIUM = ITEMS.register("enriched_sunnarium", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ENRICHED_SUNNARIUM_MK2 = ITEMS.register("enriched_sunnarium_mk2", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> RAW_CRYSTAL_CHIP = registerTooltip(
            "raw_crystal_chip", TooltipOptions.name(GREEN)
    );
    public static final DeferredHolder<Item, Item> RAW_CRYSTAL_CHIP_PARTS = registerTooltip(
            "raw_crystal_chip_parts", TooltipOptions.name(GREEN)
    );
    public static final DeferredHolder<Item, Item> ENGRAVED_CRYSTAL_CHIP = registerTooltip(
            "engraved_crystal_chip", TooltipOptions.name(GREEN)
    );
    public static final DeferredHolder<Item, Item> EMITTER = ITEMS.register("emitter", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> QUARTZ_LAMP = ITEMS.register("quartz_lamp", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> PETRI_DISH = ITEMS.register("petri_dish", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> WETWERE_CIRCUIT_BOARD = registerTooltip(
            "wetwere_circuit_board", TooltipOptions.name(RED)
    );
    public static final DeferredHolder<Item, Item> WETWERE_PRINTED_CIRCUIT_BOARD = registerTooltip(
            "wetwere_printed_circuit_board", TooltipOptions.name(RED)
    );
    public static final DeferredHolder<Item, Item> NEURO_PROCESSING_UNIT = registerTooltip(
            "neuro_processing_unit", TooltipOptions.nameAndLore(RED, 1, RED)
    );
    public static final DeferredHolder<Item, Item> WETWERE_CIRCUIT = registerTooltip(
            "wetwere_circuit", TooltipOptions.nameAndLore(SKULK_BLUE, 1, RED)
    );
    public static final DeferredHolder<Item, Item> IRIDIUM_DIOXIDE_DUST = ITEMS.register("iridium_dioxide_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRIDIUM_METAL_RESIDUE_DUST = ITEMS.register("iridium_metal_residue_dust", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> SKULK_SPORES = registerTooltip("skulk_spores", TooltipOptions.name(SKULK_BLUE)
    );
    public static final DeferredHolder<Item, Item> AGAR_GEL = ITEMS.register("agar_gel", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> AGAR_DUST = ITEMS.register("agar_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> PETRI_DISH_SKULK = registerTooltip(
            "petri_dish_skulk", TooltipOptions.nameAndLore(SKULK_BLUE, 1, SKULK_BLUE)
    );
    public static final DeferredHolder<Item, Item> STEM_CELLS = registerTooltip(
            "stem_cells", TooltipOptions.name(SKULK_BLUE)
    );
    public static final DeferredHolder<Item, Item> NEURON_CELLS = registerTooltip(
            "neuron_cells", TooltipOptions.name(SKULK_BLUE)
    );

    public static final DeferredHolder<Item, Item> SODIUM_SULFATE = ITEMS.register("sodium_sulfate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MINING_DRONE = ITEMS.register("mining_drone", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> OBSIDIAN_DUST = ITEMS.register("obsidian_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> QUANTUM_STAR = registerTooltip(
            "quantum_star", TooltipOptions.nameAndLore(LIGHT_PURPLE, 1, LIGHT_PURPLE)
    );

    public static final DeferredHolder<Item, Item> CARBON_FIBER = ITEMS.register("carbon_fiber", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CARBON_MESH = ITEMS.register("carbon_mesh", () -> new Item(new Item.Properties()));

    // --- Матриці прогнозу ---
    public static final DeferredHolder<Item, Item> PREDICTION_MATRIX_ENDERIUM = registerTooltip("enderium_prediction_matrix", TooltipOptions.nameAndLore(0x0b4748, 1, 0x0b4748));
    public static final DeferredHolder<Item, Item> PREDICTION_MATRIX_NETHER_STAR = registerTooltip("nether_star_prediction_matrix", new TooltipOptions(1, 0xedd080, 0xedd080, true));
    public static final DeferredHolder<Item, Item> NETHER_STAR_PLATE = registerTooltip("nether_star_plate", TooltipOptions.nameAndGlow(0xedd080));
    public static final DeferredHolder<Item, Item> NETHER_STAR_LARGE_PLATE = registerTooltip("nether_star_large_plate", TooltipOptions.nameAndGlow(0xedd080));
    public static final DeferredHolder<Item, Item> NETHER_STAR_RING = registerTooltip("nether_star_ring", TooltipOptions.nameAndGlow(0xedd080));
    public static final DeferredHolder<Item, Item> NETHER_STAR_DUST = registerTooltip("nether_star_dust", TooltipOptions.nameAndGlow(0xedd080));

    //DANDELIONS YAMMI!
    public static final DeferredHolder<Item, Item> LATEX_DANDELION_SEED =
            ITEMS.register("latex_dandelion_seed",
                    () -> new ItemNameBlockItem(
                            BlockRegistry.LATEX_DANDELION.get(), // ← твій блок
                            new Item.Properties()
                    )
            );
    public static final DeferredHolder<Item, Item> LATEX_DANDELION_STEM = ITEMS.register("latex_dandelion_stem", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LATEX_DANDELION_FLOWER = ITEMS.register("latex_dandelion_flower", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_LATEX = ITEMS.register("raw_latex", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_RUBBER = ITEMS.register("raw_rubber", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RUBBER_INGOT = ITEMS.register("rubber_ingot", () -> new Item(new Item.Properties()));


    public static final DeferredHolder<Item, Item> SULFUR_BERRY =
            ITEMS.register("sulfur_berry",
                    () -> new BlockItem(
                            BlockRegistry.SULFUR_BERRY_BLOCK.get(),
                            new Item.Properties()
                                    .food(new FoodProperties.Builder()
                                            .nutrition(2)
                                            .saturationModifier(0.4f)
                                            .fast()
                                            .effect(() -> new MobEffectInstance(ModEffects.SULFUR_POISONING, 200, 0), 0.2f)
                                            .build())
                    )
            );

    public static final DeferredHolder<Item, Item> TRANSMISSION = registerTooltip("transmission", TooltipOptions.nameAndLore(0x8E9D7A, 1, 0x8E9D7A));
    public static final DeferredHolder<Item, Item> MAGNALIUM_ENGINE = registerTooltip("magnalium_engine", TooltipOptions.   nameAndLore(0x8E9D7A, 2, 0x8E9D7A));
    public static final DeferredHolder<Item, Item> SOAP_STONE_DUST = ITEMS.register("soap_stone_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MANGANESE_OXIDE = ITEMS.register("manganese_oxide", () -> new Item(new Item.Properties().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).component(DataComponents.RARITY, Rarity.RARE)));
    public static final DeferredHolder<Item, Item> TREATED_PLATE = ITEMS.register("treated_plate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ROLL_PLUSH_ITEM = ITEMS.register("roll_plush", () -> new BlockItem(ROLL_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> YAN_PLUSH_ITEM = ITEMS.register("yan_plush", () -> new BlockItem(YAN_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> LEDOK_PLUSH_ITEM = ITEMS.register("ledok_plush", () -> new BlockItem(LEDOK_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> LORP_OOO_PLUSH_ITEM = ITEMS.register("lorp_ooo_plush", () -> new BlockItem(LORP_OOO_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, StormScannerItem> LV_STORM_SCANNER = ITEMS.register("lv_storm_scanner", () -> new StormScannerItem(new Item.Properties().stacksTo(1), 1, 0xff1500, 1_000_000));
    public static final DeferredHolder<Item, StormScannerItem> HV_STORM_SCANNER = ITEMS.register("hv_storm_scanner", () -> new StormScannerItem(new Item.Properties().stacksTo(1), 2, 0xff1500, 10_000_000));
    public static final DeferredHolder<Item, EnergyBatteryItem> TEST_BATTERY = ITEMS.register("nano_battery", () -> new EnergyBatteryItem(new Item.Properties(), 20_000_000L, 20_000L, 200_000L, 0x00FFFF));
    // 🔋 REDSTONE BATTERY
    public static final DeferredHolder<Item, EnergyBatteryItem> REDSTONE_BATTERY =
            ITEMS.register("redstone_battery",
                    () -> new EnergyBatteryItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.UNCOMMON),
                            1_000_000L,   // 1M EU
                            75_000L,      // input
                            75_000L,      // output
                            0xb12e2e      // червоний
                    )
            );

    // 💎 ENERGIUM BATTERY
    public static final DeferredHolder<Item, EnergyBatteryItem> ENERGIUM_BATTERY =
            ITEMS.register("energium_battery",
                    () -> new EnergyBatteryItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.RARE),
                            10_000_000L,  // 10M EU
                            150_000L,     // input
                            150_000L,     // output
                            0xFF4444      // яскраво-червоний
                    )
            );

    // 💠 LAPOTRON T1
    public static final DeferredHolder<Item, EnergyBatteryItem> LAPOTRON_BATTERY_T1 =
            ITEMS.register("lapotron_battery_t1",
                    () -> new EnergyBatteryItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC),
                            1_000_000_000L, // 1G EU
                            20_000_000L,    // input
                            20_000_000L,    // output
                            0x3B6BFF        // синій
                    )
            );

    // 💠 LAPOTRON T2
    public static final DeferredHolder<Item, EnergyBatteryItem> LAPOTRON_BATTERY_T2 =
            ITEMS.register("lapotron_battery_t2",
                    () -> new EnergyBatteryItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC),
                            50_000_000_000L, // 50G EU
                            20_000_000L,     // input
                            20_000_000L,     // output
                            0x0077FF         // глибокий синій
                    )
            );

    // 💠 LAPOTRON T3
    public static final DeferredHolder<Item, EnergyBatteryItem> LAPOTRON_BATTERY_T3 =
            ITEMS.register("lapotron_battery_t3",
                    () -> new EnergyBatteryItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC),
                            500_000_000_000L, // 500G EU
                            200_000_000L,      // input
                            200_000_000L,      // output
                            0x00FFFF          // бірюзовий
                    )
            );

    // ⚡ CUSTOM ULTRA BATTERY (твоя 1T EU)
    public static final DeferredHolder<Item, EnergyBatteryItem> ULTRA_BATTERY =
            ITEMS.register("ultra_battery",
                    () -> new EnergyBatteryItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.EPIC),
                            10_000_000_000_000L, // 10T EU
                            500_000_000L,        // input
                            500_000_000L,        // output
                            0xFFD700            // золотий
                    )
            );

    public static final DeferredHolder<Item, LunarClockItem> LUNAR_PHASE_CLOCK = ITEMS.register("moon_phase_clock", () -> new LunarClockItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> POTASSIUM_DUST = ITEMS.register("potassium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STONE_DUST = ITEMS.register("stone_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CLAY_DUST = ITEMS.register("clay_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CALCIUM_CHLORIDE_DUST = ITEMS.register("calcium_chloride_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SODIUM_TUNGSTATE_DUST = ITEMS.register("sodium_tungstate_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TUNGSTIC_ACID_DUST = ITEMS.register("tungstic_acid_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TUNGSTEN_TRIOXIDE = ITEMS.register("tungsten_trioxide", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> ERROR_ITEM =
            registerTooltip("error_item",
                    TooltipOptions.nameAndLore(
                            0xff0032,
                            2,
                            0xDD3333
                    )
            );
    //lv
    public static final DeferredHolder<Item, EnergyDrillItem> LV_MINING_DRILL =
            ITEMS.register("lv_mining_drill",
                    () -> new EnergyDrillItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties(),
                            1_000_000L,
                            1,
                            1,
                            500
                    )
            );

    public static final DeferredHolder<Item, EnergyDrillItem> ADVANCED_LV_MINING_DRILL =
            ITEMS.register("advanced_lv_mining_drill",
                    () -> new EnergyDrillItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties(),
                            2_000_000L,
                            3,
                            1,
                            1500
                    )
            );

    // MV
    public static final DeferredHolder<Item, EnergyDrillItem> MV_MINING_DRILL =
            ITEMS.register("mv_mining_drill",
                    () -> new EnergyDrillItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties(),
                            10_000_000L,
                            1,
                            2,
                            1000
                    )
            );

    public static final DeferredHolder<Item, EnergyDrillItem> ADVANCED_MV_MINING_DRILL =
            ITEMS.register("advanced_mv_mining_drill",
                    () -> new EnergyDrillItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties(),
                            20_000_000L,
                            5,
                            2,
                            3000
                    )
            );

    // HV
    public static final DeferredHolder<Item, EnergyDrillItem> HV_MINING_DRILL =
            ITEMS.register("hv_mining_drill",
                    () -> new EnergyDrillItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties(),
                            1_000_000_000L,
                            1,
                            3,
                            2000
                    )
            );

    public static final DeferredHolder<Item, EnergyDrillItem> ADVANCED_HV_MINING_DRILL =
            ITEMS.register("advanced_hv_mining_drill",
                    () -> new EnergyDrillItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties(),
                            2_000_000_000L,
                            7,
                            3,
                            6000
                    )
            );

    // EV
    public static final DeferredHolder<Item, EnergyDrillItem> EV_MINING_DRILL =
            ITEMS.register("ev_mining_drill",
                    () -> new EnergyDrillItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties(),
                            50_000_000_000L,
                            1,
                            4,
                            4000

                    )
            );

    public static final DeferredHolder<Item, EnergyDrillItem> ADVANCED_EV_MINING_DRILL =
            ITEMS.register("advanced_ev_mining_drill",
                    () -> new EnergyDrillItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties(),
                            100_000_000_000L,
                            9,
                            4,
                            12000
                    )
            );


    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}