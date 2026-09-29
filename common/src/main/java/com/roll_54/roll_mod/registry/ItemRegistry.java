package com.roll_54.roll_mod.registry;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.perigrine3.createcybernetics.item.cyberware.arm.CyberarmItem;
import com.perigrine3.createcybernetics.item.cyberware.leg.CyberlegItem;
import com.perigrine3.createcybernetics.util.CyberwareAttributeHelper;
import com.roll_54.roll_mod.items.armor.HazmatBootsItem;
import com.roll_54.roll_mod.items.armor.HazmatChestplateItem;
import com.roll_54.roll_mod.items.armor.HazmatLeggingsItem;
import com.roll_54.roll_mod.items.armor.ModArmorMaterials;
import com.roll_54.roll_mod.items.*;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.armor.geckolib.ClownHatArmorItem;
import com.roll_54.roll_mod.items.armor.geckolib.ExampleArmorItem;
import com.roll_54.roll_mod.items.armor.geckolib.HazmatHelmetItem;
import com.roll_54.roll_mod.items.armor.geckolib.MultiProtectingGraviChestItem;
import com.roll_54.roll_mod.items.armor.geckolib.SkinAnchorItem;
import com.roll_54.roll_mod.items.cyberware.CropLenseItem;
import com.roll_54.roll_mod.items.cyberware.CropModuleItem;
import com.roll_54.roll_mod.items.cyberware.SulfurResistantLungsItem;
import com.roll_54.roll_mod.items.electricItems.*;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleItem;
import com.roll_54.roll_mod.items.modulardrill.DrillVoltage;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import com.roll_54.roll_mod.items.modularsaber.ModularSaberItem;
import com.roll_54.roll_mod.items.modularsaber.SaberVoltage;
import com.roll_54.roll_mod.items.modulardrill.ModuleType;
import com.roll_54.roll_mod.items.spaceModule.DimensionCartridgeCItem;
import com.roll_54.roll_mod.items.spaceModule.RocketItem;
import com.roll_54.roll_mod.util.TooltipOptions;
import com.roll_54.roll_mod.util.TooltipManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

import static com.roll_54.roll_mod.items.ModToolTiers.*;
import static com.roll_54.roll_mod.registry.BlockRegistry.*;
import static net.minecraft.world.item.Tiers.IRON;
import static net.minecraft.world.item.Tiers.NETHERITE;

@SuppressWarnings("unused")
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
    private static final int LIGHT_GRAY = 0xAAAAAA;       // §7
    private static final int DARK_GRAY = 0x555555;       // §8
    private static final int SILVER_GRAY = 0x333333;       // my own

    private static final int GREEN = 0x55FF55;
    private static final int LIGHT_GREN = 0x73ff85;
    private static final int WHITE = 0xFFFFFF;
    private static final int BLACK_BRONZE_COLOR = 0x9B4F98; //my own
    private static final int BISMUTH_BRONZE_COLOR = 0xC08A4A;//my own
    private static final int MALACHITE_GREEEN = 0x1a9e74;//my own
    private static final int METEORITE_DARK_BLUE = 0x3B2AB8;//my own
    private static final int METEORITE_LIGHT_BLUE = 0x005acf;//my own
    private static final int BLACK_STEEL_COLOR = 0x8C8C90; //my own



    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, RollMod.MODID);
    public static final DeferredRegister<Item> CROPS = DeferredRegister.create(Registries.ITEM, RollMod.MODID);


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

    /**
     * One piece of a tool-tier armour set. Durability comes from the vanilla per-slot bases times
     * {@code durabilityMultiplier}, the same way vanilla materials scale, since 1.21.1 ArmorMaterial
     * no longer carries durability itself.
     */
    private static DeferredHolder<Item, Item> registerTierArmor(String name,
                                                                DeferredHolder<ArmorMaterial, ArmorMaterial> material,
                                                                ArmorItem.Type type,
                                                                int durabilityMultiplier,
                                                                int nameColor) {
        return ITEMS.register(name, () -> new TooltipArmorItem.Builder(
                Holder.direct(material.get()),
                type,
                new Item.Properties().stacksTo(1).durability(type.getDurability(durabilityMultiplier))
        )
                .nameColor(nameColor)
                .build());
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
            new TooltipOptions(1, METEORITE_DARK_BLUE, METEORITE_LIGHT_BLUE, false)
    );

    public static final DeferredHolder<Item, Item> METEORITE_METAL_PLATE = registerTooltip(
            "meteorite_metal_plate",
            new Item.Properties(), TooltipOptions.name(METEORITE_DARK_BLUE)
    );

    public static final DeferredHolder<Item, Item> METEORITE_METAL_ROD = registerTooltip(
            "meteorite_metal_rod",
            new Item.Properties(), TooltipOptions.name(METEORITE_DARK_BLUE)
    );

    public static final DeferredHolder<Item, Item> METEORITE_METAL_LARGE_PLATE = registerTooltip(
            "meteorite_metal_large_plate",
            new Item.Properties(), TooltipOptions.name(METEORITE_DARK_BLUE)
    );

    public static final DeferredHolder<Item, BlockogrizItem> DIAMOND_BLOCKOGRIZ =
            ITEMS.register(
                    "diamond_blockogriz",
                    () -> new TooltipManager.TooltipBlockogrizItem(
                            DIAMOND_ALLOY,
                            15.0F,
                            -2.8F,
                            new Item.Properties(),
                            TooltipOptions.nameAndLore(MALACHITE_GREEEN, 2, AQUA)
                    )
            );

    public static final DeferredHolder<Item, BlockogrizItem> METEORITE_METAL_BLOCKOGRIZ =
            ITEMS.register(
                    "meteorite_metal_blockogriz",
                    () -> new TooltipManager.TooltipBlockogrizItem(
                            METEORITE_METAL,
                            15.0F,
                            -2.8F,
                            new Item.Properties(),
                            TooltipOptions.nameAndLore(METEORITE_DARK_BLUE, 2, METEORITE_LIGHT_BLUE)
                    )
            );

    public static final DeferredHolder<Item, BlockogrizItem> IRON_BLOCKOGRIZ =
            ITEMS.register(
                    "iron_blockogriz",
                    () -> new TooltipManager.TooltipBlockogrizItem(
                            IRON,
                            6.0F,
                            -2.8F,
                            new Item.Properties(),
                            TooltipOptions.nameAndLore(WHITE, 1,LIGHT_GRAY)
                    )
            );

    public static final DeferredHolder<Item, BlockogrizItem> STEEL_BLOCKOGRIZ =
            ITEMS.register(
                    "steel_blockogriz",
                    () -> new TooltipManager.TooltipBlockogrizItem(
                            STEEL,
                            11.0F,
                            -2.8F,
                            new Item.Properties(),
                            TooltipOptions.nameAndLore(BLACK_STEEL_COLOR, 1,LIGHT_GRAY)
                    )
            );

    public static final DeferredHolder<Item, BlockogrizItem> NETHERITE_BLOCKOGRIZ =
            ITEMS.register(
                    "netherite_blockogriz",
                    () -> new TooltipManager.TooltipBlockogrizItem(
                            NETHERITE,
                            12.5F,
                            -2.8F,
                            new Item.Properties(),
                            TooltipOptions.nameAndLore(WHITE, 1,LIGHT_GRAY)
                    )
            );

    public static final DeferredHolder<Item, BlockogrizItem> BLACK_STEEL_BLOCKOGRIZ =
            ITEMS.register(
                    "black_steel_blockogriz",
                    () -> new TooltipManager.TooltipBlockogrizItem(
                            BLACK_STEEL,
                            11.5F,
                            -2.8F,
                            new Item.Properties(),
                            TooltipOptions.nameAndLore(BLACK_STEEL_COLOR, 1,LIGHT_GRAY)
                    )
            );

    public static final DeferredHolder<Item, BlockogrizItem> BISMUTH_BRONZE_BLOCKOGRIZ =
            ITEMS.register(
                    "bismuth_bronze_blockogriz",
                    () -> new TooltipManager.TooltipBlockogrizItem(
                            BISMUTH_BRONZE,
                            9.0F,
                            -2.8F,
                            new Item.Properties(),
                            TooltipOptions.nameAndLore(BISMUTH_BRONZE_COLOR, 1,LIGHT_GRAY)
                    )
            );

    public static final DeferredHolder<Item, BlockogrizItem> BLACK_BRONZE_BLOCKOGRIZ =
            ITEMS.register(
                    "black_bronze_blockogriz",
                    () -> new TooltipManager.TooltipBlockogrizItem(
                            BLACK_BRONZE,
                            9.0F,
                            -2.8F,
                            new Item.Properties(),
                            TooltipOptions.nameAndLore(BLACK_BRONZE_COLOR, 1,LIGHT_GRAY)
                    )
            );

    public static final DeferredHolder<Item, Item> STEEL_SWORD = ITEMS.register(
            "steel_sword",
            () -> new TooltipManager.TooltipSwordItem(
                    ModToolTiers.STEEL, 3, -2.4f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR)
            )
    );

    public static final DeferredHolder<Item, Item> STEEL_PICKAXE = ITEMS.register(
            "steel_pickaxe",
            () -> new TooltipManager.TooltipPickaxeItem(
                    ModToolTiers.STEEL, 1f, -2.8f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR)
            )
    );

    public static final DeferredHolder<Item, Item> STEEL_AXE = ITEMS.register(
            "steel_axe",
            () -> new TooltipManager.TooltipAxeItem(
                    ModToolTiers.STEEL, 6.0f, -3.1f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR)
            )
    );

    public static final DeferredHolder<Item, Item> STEEL_SHOVEL = ITEMS.register(
            "steel_shovel",
            () -> new TooltipManager.TooltipShovelItem(
                    ModToolTiers.STEEL, 1.5f, -3.0f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR)
            )
    );

    public static final DeferredHolder<Item, Item> STEEL_HOE = ITEMS.register(
            "steel_hoe",
            () -> new TooltipManager.TooltipHoeItem(
                    ModToolTiers.STEEL, -2, -1.0f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR)
            )
    );

    // БРОНІКИ!!!
    public static final DeferredHolder<Item, HazmatHelmetItem> HAZMAT_HELMET = ITEMS.register(
            "hazmat_helmet",
            () -> new HazmatHelmetItem(
                    new HazmatHelmetItem.Properties().stacksTo(1).durability(2400)
                            .component(ComponentsRegistry.RADIATION_RESISTANCE.get(), 0.25f),
                    TooltipOptions.nameAndLore(0xe8c52a, 2, 0xc28400)

            )

    );

    public static final DeferredHolder<Item, Item> HAZMAT_CHESTPLATE = ITEMS.register(
            "hazmat_chestplate",
            () -> new HazmatChestplateItem(
                    ArmorItem.Type.CHESTPLATE,
                    new HazmatChestplateItem.Properties().stacksTo(1).durability(2400)
                            .component(ComponentsRegistry.RADIATION_RESISTANCE.get(), 0.25f),
                    TooltipOptions.nameAndLore(0xe8c52a, 2, 0xc28400)
            )
    );

    public static final DeferredHolder<Item, Item> HAZMAT_LEGGINGS = ITEMS.register(
            "hazmat_leggings",
            () -> new HazmatLeggingsItem(
                    ArmorItem.Type.LEGGINGS,
                    new Item.Properties().stacksTo(1).durability(2400)
                            .component(ComponentsRegistry.RADIATION_RESISTANCE.get(), 0.25f),
                    TooltipOptions.nameAndLore(0xe8c52a, 2, 0xc28400)
            )
    );

    public static final DeferredHolder<Item, Item> HAZMAT_BOOTS = ITEMS.register(
            "hazmat_boots",
            () -> new HazmatBootsItem(
                    Holder.direct(ModArmorMaterials.HAZMAT_ARMOR.get()),
                    ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1).durability(2400)
                            .component(ComponentsRegistry.RADIATION_RESISTANCE.get(), 0.25f),
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
                    .nameColor(METEORITE_DARK_BLUE)  // глибокий синій
                    .loreColor(METEORITE_LIGHT_BLUE)  // світло-біло-блакитний
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
                    .nameColor(METEORITE_DARK_BLUE)
                    .loreColor(METEORITE_LIGHT_BLUE)
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
                    .nameColor(METEORITE_DARK_BLUE)
                    .loreColor(METEORITE_LIGHT_BLUE)
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
                    .nameColor(METEORITE_DARK_BLUE)
                    .loreColor(METEORITE_LIGHT_BLUE)
                    .build()
    );

    public static final DeferredHolder<Item, Item> METEORITE_SWORD = ITEMS.register(
            "meteorite_sword",
            () -> new TooltipManager.TooltipSwordItem(
                    ModToolTiers.METEORITE_METAL, 12f, -2.4f,
                    new Item.Properties().durability(3400),
                    TooltipOptions.nameAndLore(METEORITE_DARK_BLUE, 1, METEORITE_LIGHT_BLUE)
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_PICKAXE = ITEMS.register(
            "meteorite_pickaxe",
            () -> new TooltipManager.TooltipPickaxeItem(
                    ModToolTiers.METEORITE_METAL, 6f, -2.8f,
                    new Item.Properties(),
                    TooltipOptions.nameAndLore(METEORITE_DARK_BLUE, 1, METEORITE_LIGHT_BLUE)
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_AXE = ITEMS.register(
            "meteorite_axe",
            () -> new TooltipManager.TooltipAxeItem(
                    ModToolTiers.METEORITE_METAL, 11f, -3.0f,
                    new Item.Properties(),
                    TooltipOptions.nameAndLore(METEORITE_DARK_BLUE, 1, METEORITE_LIGHT_BLUE)
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_SHOVEL = ITEMS.register(
            "meteorite_shovel",
            () -> new TooltipManager.TooltipShovelItem(
                    ModToolTiers.METEORITE_METAL, 5f, -3.0f,
                    new Item.Properties(),
                    TooltipOptions.nameAndLore(METEORITE_DARK_BLUE, 1, METEORITE_LIGHT_BLUE)
            )
    );

    public static final DeferredHolder<Item, Item> METEORITE_HOE = ITEMS.register(
            "meteorite_hoe",
            () -> new TooltipManager.TooltipHoeItem(
                    ModToolTiers.METEORITE_METAL, 2f, -1.0f,
                    new Item.Properties(),
                    TooltipOptions.nameAndLore(METEORITE_DARK_BLUE, 1, METEORITE_LIGHT_BLUE)
            )
    );

    // Armour sets matching the tool tiers in ModToolTiers: the same material that makes better tools
    // makes better armour. Durability multipliers track each tier's tool durability.
    public static final DeferredHolder<Item, Item> BISMUTH_BRONZE_HELMET = registerTierArmor(
            "bismuth_bronze_helmet", ModArmorMaterials.BISMUTH_BRONZE_ARMOR, ArmorItem.Type.HELMET, 12, BISMUTH_BRONZE_COLOR);
    public static final DeferredHolder<Item, Item> BISMUTH_BRONZE_CHESTPLATE = registerTierArmor(
            "bismuth_bronze_chestplate", ModArmorMaterials.BISMUTH_BRONZE_ARMOR, ArmorItem.Type.CHESTPLATE, 12, BISMUTH_BRONZE_COLOR);
    public static final DeferredHolder<Item, Item> BISMUTH_BRONZE_LEGGINGS = registerTierArmor(
            "bismuth_bronze_leggings", ModArmorMaterials.BISMUTH_BRONZE_ARMOR, ArmorItem.Type.LEGGINGS, 12, BISMUTH_BRONZE_COLOR);
    public static final DeferredHolder<Item, Item> BISMUTH_BRONZE_BOOTS = registerTierArmor(
            "bismuth_bronze_boots", ModArmorMaterials.BISMUTH_BRONZE_ARMOR, ArmorItem.Type.BOOTS, 12, BISMUTH_BRONZE_COLOR);

    public static final DeferredHolder<Item, Item> BLACK_BRONZE_HELMET = registerTierArmor(
            "black_bronze_helmet", ModArmorMaterials.BLACK_BRONZE_ARMOR, ArmorItem.Type.HELMET, 16, BLACK_BRONZE_COLOR);
    public static final DeferredHolder<Item, Item> BLACK_BRONZE_CHESTPLATE = registerTierArmor(
            "black_bronze_chestplate", ModArmorMaterials.BLACK_BRONZE_ARMOR, ArmorItem.Type.CHESTPLATE, 16, BLACK_BRONZE_COLOR);
    public static final DeferredHolder<Item, Item> BLACK_BRONZE_LEGGINGS = registerTierArmor(
            "black_bronze_leggings", ModArmorMaterials.BLACK_BRONZE_ARMOR, ArmorItem.Type.LEGGINGS, 16, BLACK_BRONZE_COLOR);
    public static final DeferredHolder<Item, Item> BLACK_BRONZE_BOOTS = registerTierArmor(
            "black_bronze_boots", ModArmorMaterials.BLACK_BRONZE_ARMOR, ArmorItem.Type.BOOTS, 16, BLACK_BRONZE_COLOR);

    public static final DeferredHolder<Item, Item> STEEL_HELMET = registerTierArmor(
            "steel_helmet", ModArmorMaterials.STEEL_ARMOR, ArmorItem.Type.HELMET, 26, BLACK_STEEL_COLOR);
    public static final DeferredHolder<Item, Item> STEEL_CHESTPLATE = registerTierArmor(
            "steel_chestplate", ModArmorMaterials.STEEL_ARMOR, ArmorItem.Type.CHESTPLATE, 26, BLACK_STEEL_COLOR);
    public static final DeferredHolder<Item, Item> STEEL_LEGGINGS = registerTierArmor(
            "steel_leggings", ModArmorMaterials.STEEL_ARMOR, ArmorItem.Type.LEGGINGS, 26, BLACK_STEEL_COLOR);
    public static final DeferredHolder<Item, Item> STEEL_BOOTS = registerTierArmor(
            "steel_boots", ModArmorMaterials.STEEL_ARMOR, ArmorItem.Type.BOOTS, 26, BLACK_STEEL_COLOR);

    public static final DeferredHolder<Item, Item> BLACK_STEEL_HELMET = registerTierArmor(
            "black_steel_helmet", ModArmorMaterials.BLACK_STEEL_ARMOR, ArmorItem.Type.HELMET, 22, BLACK_STEEL_COLOR);
    public static final DeferredHolder<Item, Item> BLACK_STEEL_CHESTPLATE = registerTierArmor(
            "black_steel_chestplate", ModArmorMaterials.BLACK_STEEL_ARMOR, ArmorItem.Type.CHESTPLATE, 22, BLACK_STEEL_COLOR);
    public static final DeferredHolder<Item, Item> BLACK_STEEL_LEGGINGS = registerTierArmor(
            "black_steel_leggings", ModArmorMaterials.BLACK_STEEL_ARMOR, ArmorItem.Type.LEGGINGS, 22, BLACK_STEEL_COLOR);
    public static final DeferredHolder<Item, Item> BLACK_STEEL_BOOTS = registerTierArmor(
            "black_steel_boots", ModArmorMaterials.BLACK_STEEL_ARMOR, ArmorItem.Type.BOOTS, 22, BLACK_STEEL_COLOR);

    public static final DeferredHolder<Item, Item> DIAMOND_ALLOY_DUST = registerTooltip(
            "diamond_alloy_dust",
            new Item.Properties(), TooltipOptions.name(MALACHITE_GREEEN)
    );

    public static final DeferredHolder<Item, Item> DIAMOND_ALLOY_INGOT = registerTooltip(
            "diamond_alloy_ingot",
            new TooltipOptions(1, MALACHITE_GREEEN, AQUA, false)
    );

    public static final DeferredHolder<Item, Item> DIAMOND_ALLOY_PLATE = registerTooltip(
            "diamond_alloy_plate",
            new Item.Properties(), TooltipOptions.name(MALACHITE_GREEEN)
    );

    public static final DeferredHolder<Item, Item> DIAMOND_ALLOY_LARGE_PLATE = registerTooltip(
            "diamond_alloy_large_plate",
            new Item.Properties(), TooltipOptions.name(MALACHITE_GREEEN)
    );




    /** Fortune I on the digging tools, Looting I on the ones that are swung at mobs. */
    private static final InnateEnchantments BLACK_BRONZE_FORTUNE =
            InnateEnchantments.of(Enchantments.FORTUNE, 1);
    private static final InnateEnchantments BLACK_BRONZE_LOOTING =
            InnateEnchantments.of(Enchantments.LOOTING, 1);

    /** Efficiency II + Unbreaking I while mining, Sharpness II + Unbreaking I while fighting. */
    private static final InnateEnchantments BISMUTH_BRONZE_MINING =
            InnateEnchantments.of(Enchantments.EFFICIENCY, 2, Enchantments.UNBREAKING, 1);
    private static final InnateEnchantments BISMUTH_BRONZE_COMBAT =
            InnateEnchantments.of(Enchantments.SHARPNESS, 2, Enchantments.UNBREAKING, 1);

    public static final DeferredHolder<Item, Item> BLACK_BRONZE_SWORD = ITEMS.register(
            "black_bronze_sword",
            () -> new InnateEnchantedTools.InnateSwordItem(
                    ModToolTiers.BLACK_BRONZE, 3f, -2.4f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_BRONZE_COLOR),
                    BLACK_BRONZE_LOOTING
            )
    );

    public static final DeferredHolder<Item, Item> BLACK_BRONZE_PICKAXE = ITEMS.register(
            "black_bronze_pickaxe",
            () -> new InnateEnchantedTools.InnatePickaxeItem(
                    ModToolTiers.BLACK_BRONZE, 1f, -2.8f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_BRONZE_COLOR),
                    BLACK_BRONZE_FORTUNE
            )
    );

    public static final DeferredHolder<Item, Item> BLACK_BRONZE_AXE = ITEMS.register(
            "black_bronze_axe",
            () -> new InnateEnchantedTools.InnateAxeItem(
                    ModToolTiers.BLACK_BRONZE, 6f, -3.1f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_BRONZE_COLOR),
                    BLACK_BRONZE_LOOTING
            )
    );

    public static final DeferredHolder<Item, Item> BLACK_BRONZE_SHOVEL = ITEMS.register(
            "black_bronze_shovel",
            () -> new InnateEnchantedTools.InnateShovelItem(
                    ModToolTiers.BLACK_BRONZE, 1.5f, -3.0f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_BRONZE_COLOR),
                    BLACK_BRONZE_FORTUNE
            )
    );

    public static final DeferredHolder<Item, Item> BLACK_BRONZE_HOE = ITEMS.register(
            "black_bronze_hoe",
            () -> new InnateEnchantedTools.InnateHoeItem(
                    ModToolTiers.BLACK_BRONZE, -2f, -1.0f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_BRONZE_COLOR),
                    BLACK_BRONZE_FORTUNE
            )
    );

    public static final DeferredHolder<Item, Item> BISMUTH_BRONZE_SWORD = ITEMS.register(
            "bismuth_bronze_sword",
            () -> new InnateEnchantedTools.InnateSwordItem(
                    ModToolTiers.BISMUTH_BRONZE, 3f, -2.4f,
                    new Item.Properties(),
                    TooltipOptions.name(BISMUTH_BRONZE_COLOR),
                    BISMUTH_BRONZE_COMBAT
            )
    );

    public static final DeferredHolder<Item, Item> BISMUTH_BRONZE_PICKAXE = ITEMS.register(
            "bismuth_bronze_pickaxe",
            () -> new InnateEnchantedTools.InnatePickaxeItem(
                    ModToolTiers.BISMUTH_BRONZE, 1f, -2.8f,
                    new Item.Properties(),
                    TooltipOptions.name(BISMUTH_BRONZE_COLOR),
                    BISMUTH_BRONZE_MINING
            )
    );

    public static final DeferredHolder<Item, Item> BISMUTH_BRONZE_AXE = ITEMS.register(
            "bismuth_bronze_axe",
            () -> new InnateEnchantedTools.InnateAxeItem(
                    ModToolTiers.BISMUTH_BRONZE, 6f, -3.1f,
                    new Item.Properties(),
                    TooltipOptions.name(BISMUTH_BRONZE_COLOR),
                    BISMUTH_BRONZE_COMBAT
            )
    );

    public static final DeferredHolder<Item, Item> BISMUTH_BRONZE_SHOVEL = ITEMS.register(
            "bismuth_bronze_shovel",
            () -> new InnateEnchantedTools.InnateShovelItem(
                    ModToolTiers.BISMUTH_BRONZE, 1.5f, -3.0f,
                    new Item.Properties(),
                    TooltipOptions.name(BISMUTH_BRONZE_COLOR),
                    BISMUTH_BRONZE_MINING
            )
    );

    public static final DeferredHolder<Item, Item> BISMUTH_BRONZE_HOE = ITEMS.register(
            "bismuth_bronze_hoe",
            () -> new InnateEnchantedTools.InnateHoeItem(
                    ModToolTiers.BISMUTH_BRONZE, -2f, -1.0f,
                    new Item.Properties(),
                    TooltipOptions.name(BISMUTH_BRONZE_COLOR),
                    BISMUTH_BRONZE_MINING
            )
    );


    /** Efficiency IV while mining, Sharpness III + Efficiency III on the axe, Sharpness IV on the sword. */
    private static final InnateEnchantments BLACK_STEEL_MINING =
            InnateEnchantments.of(Enchantments.EFFICIENCY, 4);
    private static final InnateEnchantments BLACK_STEEL_AXE_ENCHANTS =
            InnateEnchantments.of(Enchantments.SHARPNESS, 3, Enchantments.EFFICIENCY, 3);
    private static final InnateEnchantments BLACK_STEEL_SWORD_ENCHANTS =
            InnateEnchantments.of(Enchantments.SHARPNESS, 4);

    public static final DeferredHolder<Item, Item> BLACK_STEEL_SWORD = ITEMS.register(
            "black_steel_sword",
            () -> new InnateEnchantedTools.InnateSwordItem(
                    ModToolTiers.BLACK_STEEL, 3f, -2.4f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR),
                    BLACK_STEEL_SWORD_ENCHANTS
            )
    );

    public static final DeferredHolder<Item, Item> BLACK_STEEL_PICKAXE = ITEMS.register(
            "black_steel_pickaxe",
            () -> new InnateEnchantedTools.InnatePickaxeItem(
                    ModToolTiers.BLACK_STEEL, 1f, -2.8f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR),
                    BLACK_STEEL_MINING
            )
    );

    public static final DeferredHolder<Item, Item> BLACK_STEEL_AXE = ITEMS.register(
            "black_steel_axe",
            () -> new InnateEnchantedTools.InnateAxeItem(
                    ModToolTiers.BLACK_STEEL, 5f, -3.0f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR),
                    BLACK_STEEL_AXE_ENCHANTS
            )
    );

    public static final DeferredHolder<Item, Item> BLACK_STEEL_SHOVEL = ITEMS.register(
            "black_steel_shovel",
            () -> new InnateEnchantedTools.InnateShovelItem(
                    ModToolTiers.BLACK_STEEL, 1.5f, -3.0f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR),
                    BLACK_STEEL_MINING
            )
    );

    public static final DeferredHolder<Item, Item> BLACK_STEEL_HOE = ITEMS.register(
            "black_steel_hoe",
            () -> new InnateEnchantedTools.InnateHoeItem(
                    ModToolTiers.BLACK_STEEL, -3f, 0.0f,
                    new Item.Properties(),
                    TooltipOptions.name(BLACK_STEEL_COLOR),
                    BLACK_STEEL_MINING
            )
    );

    public static final DeferredHolder<Item, Item> FR2_SHEET = ITEMS.register(
            "fr2_sheet", () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> FR4_SHEET = ITEMS.register(
            "fr4_sheet", () -> new Item(new Item.Properties())
    );

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

    public static final DeferredHolder<Item, Item> AMETHYST_OSCILLATOR = ITEMS.register(
            "amethyst_oscillator", () -> new Item(new Item.Properties())
    );
    public static final DeferredHolder<Item, Item> REDSTONE_TUBE = registerTooltip(
            "redstone_tube", TooltipOptions.nameAndLore(RED, 1, WHITE)
    );

    public static final DeferredHolder<Item, Item> MIRROR = registerTooltip(
            "mirror", TooltipOptions.name(LIGHT_GREN)
    );

    // === T3 (PURPLE) ===
    public static final DeferredHolder<Item, Item> PURPLE_BOULE = registerTooltip("purple_boule", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER = registerTooltip("purple_wafer", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_ACCUMULATION = registerTooltip("purple_wafer_accumulation", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_CHIP_ACCUMULATION = registerTooltip("purple_chip_accumulation", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_QUBIT = registerTooltip("purple_wafer_qubit", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_CHIP_QUBIT = registerTooltip("purple_chip_qubit", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_SOC = registerTooltip("purple_wafer_soc", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_CHIP_SOC = registerTooltip("purple_chip_soc", TooltipOptions.name(LIGHT_PURPLE));
    //previous level logic
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_RAM = registerTooltip("purple_wafer_ram", TooltipOptions.name(LIGHT_PURPLE));
    public static final DeferredHolder<Item, Item> PURPLE_WAFER_CONCURRENT = registerTooltip("purple_wafer_concurrent", TooltipOptions.name(LIGHT_PURPLE));


    // === T2 (BLUE) ===
    public static final DeferredHolder<Item, Item> BLUE_BOULE = registerTooltip("blue_boule", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER = registerTooltip("blue_wafer", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_RAM = registerTooltip("blue_wafer_ram", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_CHIP_RAM = registerTooltip("blue_chip_ram", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_CHIP_CPU = registerTooltip("blue_chip_cpu", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_CONCURRENT = registerTooltip("blue_wafer_concurrent", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_CHIP_CONCURRENT = registerTooltip("blue_chip_concurrent", TooltipOptions.name(BLUE));

    //previous level logic
    public static final DeferredHolder<Item, Item> BLUE_WAFER_NOT = registerTooltip("blue_wafer_not", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_OR = registerTooltip("blue_wafer_or", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_CPU = registerTooltip("blue_wafer_cpu", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_AND = registerTooltip("blue_wafer_and", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_NAND = registerTooltip("blue_wafer_nand", TooltipOptions.name(BLUE));
    public static final DeferredHolder<Item, Item> BLUE_WAFER_PMIC = registerTooltip("blue_wafer_pmic", TooltipOptions.name(BLUE));

    //T1
    public static final DeferredHolder<Item, Item> STANDARD_WAFER_NOT = ITEMS.register("standard_wafer_not", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_CHIP_NOT = ITEMS.register("standard_chip_not", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_WAFER_AND = ITEMS.register("standard_wafer_and", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_CHIP_AND = ITEMS.register("standard_chip_and", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_WAFER_NAND = ITEMS.register("standard_wafer_nand", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_CHIP_NAND = ITEMS.register("standard_chip_nand", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_WAFER_PMIC = ITEMS.register("standard_wafer_pmic", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STANDARD_CHIP_PMIC = ITEMS.register("standard_chip_pmic", () -> new Item(new Item.Properties()));


    public static final DeferredHolder<Item, Item> RED_LENS = registerTooltip("red_lens", TooltipOptions.name(0xD32F2F));
    public static final DeferredHolder<Item, Item> GREEN_LENS = registerTooltip("green_lens", TooltipOptions.name(0x388E3C));
    public static final DeferredHolder<Item, Item> YELLOW_LENS = registerTooltip("yellow_lens", TooltipOptions.name(0xFBC02D));
    public static final DeferredHolder<Item, Item> WHITE_LENS = registerTooltip("white_lens", TooltipOptions.name(0xEDEDED));
    public static final DeferredHolder<Item, Item> PURPLE_LENS = registerTooltip("purple_lens", TooltipOptions.name(0x7B1FA2));
    public static final DeferredHolder<Item, Item> LIGHT_BLUE_LENS = registerTooltip("light_blue_lens", TooltipOptions.name(0x027ea7));
    public static final DeferredHolder<Item, Item> PALE_LENS = registerTooltip("pale_lens", TooltipOptions.name(0xedd080));
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

    // Бісмут: злиток і прокат до нього. Руда й пил ідуть з GeneratedOreRegistry.
    public static final DeferredHolder<Item, Item> BISMUTH_INGOT = registerSimple("bismuth_ingot");
    public static final DeferredHolder<Item, Item> BISMUTH_PLATE = registerSimple("bismuth_plate");
    public static final DeferredHolder<Item, Item> BISMUTH_CURVED_PLATE = registerSimple("bismuth_curved_plate");

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
    public static final DeferredHolder<Item, Item> QUBIT_SENSOR = ITEMS.register("qubit_sensor", () -> new Item(new Item.Properties()));
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


    // New research blueprints
    public static final DeferredHolder<Item, Item> RESEARCH_ROCKET_CONTROLLER = registerTooltip(
            "research_rocket_controller",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_ROCKET_T2 = registerTooltip(
            "research_rocket_t2",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_ROCKET_T3 = registerTooltip(
            "research_rocket_t3",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_LITHOGRAPHY = registerTooltip(
            "research_lithography",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_HIGH_EFFICIENCY_STEAM_TURBINE = registerTooltip(
            "research_high_efficiency_steam_turbine",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_BETTER_CIRCUITS_LV = registerTooltip(
            "research_better_circuits_lv",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_BETTER_CIRCUITS_MV = registerTooltip(
            "research_better_circuits_mv",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_BETTER_CIRCUITS_HV = registerTooltip(
            "research_better_circuits_hv",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_BETTER_CIRCUITS_EV = registerTooltip(
            "research_better_circuits_ev",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_BETTER_CIRCUITS_IV = registerTooltip(
            "research_better_circuits_iv",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
    );
    public static final DeferredHolder<Item, Item> RESEARCH_ENERGY_CRYSTALS = registerTooltip(
            "research_energy_crystals",
            new TooltipOptions(2, LIGHT_PURPLE, WHITE, false)
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
                            List.of(
                                    new InjectionEffect(
                                            MobEffects.REGENERATION,
                                            12000,
                                            2
                                    )
                            )
                    )
            );
    public static final DeferredHolder<Item, Item> INJECTION_RESISTANCE =
            ITEMS.register("injection_resistance", () ->
                    new InjectionItem(
                            new Item.Properties(),
                            List.of(
                                    new InjectionEffect(
                                            MobEffects.DAMAGE_RESISTANCE,
                                            10000,
                                            0
                                    )
                            )
                    )
            );

    public static final DeferredHolder<Item, Item> INJECTION_SPEED =
            ITEMS.register("injection_speed", () ->
                    new InjectionItem(
                            new Item.Properties(),
                            List.of(
                                    new InjectionEffect(
                                            MobEffects.MOVEMENT_SPEED,
                                            18000,
                                            1
                                    ),
                                    new InjectionEffect(
                                            MobEffects.JUMP,
                                            18000,
                                            1
                                    )
                            )
                    )
            );

    public static final DeferredHolder<Item, Item> INJECTION_FIRE_RESISTANCE =
            ITEMS.register("injection_fire_resistance", () ->
                    new InjectionItem(
                            new Item.Properties(),
                            List.of(
                                    new InjectionEffect(
                                            MobEffects.FIRE_RESISTANCE,
                                            12000,
                                            0
                                    ),
                                    new InjectionEffect(ModEffects.SULFUR_RESISTANCE,
                                            12000,
                                            0
                                    )
                            )
                    )
            );

    public static final DeferredHolder<Item, Item> INJECTION_HASTE =
            ITEMS.register("injection_haste", () ->
                    new InjectionItem(
                            new Item.Properties(),
                            List.of(
                                    new InjectionEffect(MobEffects.DIG_SPEED,
                                            12000,
                                            1
                                    ),
                                    new InjectionEffect(MobEffects.MOVEMENT_SPEED,
                                            6000,
                                            0
                                    )
                            )
                    )
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
    public static final DeferredHolder<Item, Item> ENERGIUM_LASER = ITEMS.register("energium_laser", () -> new Item(new Item.Properties()));
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
                            .effect(() -> new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30000, 0), 1.0f)
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

    // 🍍 Скибка фторитового ананаса
    public static final DeferredHolder<Item, Item> FLUORITE_PINEAPPLE_SLICE = ITEMS.register(
            "fluorite_pineapple_slice",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(3)
                            .saturationModifier(0.3f)
                            .fast()
                            .build()
            ))
    );

    // 🍕 Скибка піци з фторитовим ананасом
    public static final DeferredHolder<Item, Item> FLUORITE_PINEAPPLE_PIZZA_SLICE = ITEMS.register(
            "fluorite_pineapple_pizza_slice",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationModifier(0.8f)
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0), 1.0f)
                            .build()
            ))
    );

    // 🫑 Скибка рутилового перцю
    public static final DeferredHolder<Item, Item> BELL_PEPPER_SLICE_RUTILE = ITEMS.register(
            "bell_pepper_slice_rutile",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2)
                            .saturationModifier(0.3f)
                            .fast()
                            .build()
            ))
    );

    // 🫑 Запечена скибка рутилового перця
    public static final DeferredHolder<Item, Item> ROASTED_BELL_PEPPER_RUTILE_SLICE = ITEMS.register(
            "roasted_bell_pepper_rutile_slice",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(2)
                            .saturationModifier(0.5f)
                            .build()
            ))
    );

    // 🫑 Запечений рутиловий перець
    public static final DeferredHolder<Item, Item> ROASTED_BELL_PEPPER_RUTILE = ITEMS.register(
            "roasted_bell_pepper_rutile",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(6)
                            .saturationModifier(0.7f)
                            .build()
            ))
    );

    // 🫑 Фарширований рутиловий перець
    public static final DeferredHolder<Item, Item> STUFFED_BELL_PEPPER_RUTILE = ITEMS.register(
            "stuffed_bell_pepper_rutile",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder()
                            .nutrition(9)
                            .saturationModifier(0.9f)
                            .effect(() -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0), 1.0f)
                            .build()
            ))
    );

    // 🍹 Мохіто
    public static final DeferredHolder<Item, Item> MOJITO = ITEMS.register(
            "mojito",
            () -> new Item(new Item.Properties()
                    .stacksTo(16)
                    .food(new FoodProperties.Builder()
                            .nutrition(4)
                            .saturationModifier(0.3f)
                            .alwaysEdible()
                            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 0), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 1800, 0), 1.0f)
                            .build())
            )
    );

    // 🍔 XP-бургер (дає досвід при поїданні)
    public static final DeferredHolder<Item, Item> XP_BURGER = ITEMS.register(
            "xp_burger",
            () -> new XpFoodItem(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(8)
                            .saturationModifier(0.8f)
                            .build()),
                    50
            )
    );

    // 🍔 Золотий XP-бургер (багато досвіду + ефекти)
    public static final DeferredHolder<Item, Item> GOLDEN_XP_BURGER = ITEMS.register(
            "golden_xp_burger",
            () -> new XpFoodItem(new Item.Properties()
                    .stacksTo(16)
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                    .component(DataComponents.RARITY, Rarity.RARE)
                    .food(new FoodProperties.Builder()
                            .nutrition(12)
                            .saturationModifier(1.2f)
                            .alwaysEdible()
                            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 600, 1), 1.0f)
                            .effect(() -> new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1), 1.0f)
                            .build()),
                    250
            )
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
            CROPS.register("latex_dandelion_seed",
                    () -> new ItemNameBlockItem(
                            BlockRegistry.LATEX_DANDELION.get(), // ← твій блок
                            new Item.Properties()
                    )
            );
    public static final DeferredHolder<Item, Item> LATEX_DANDELION_STEM = CROPS.register("latex_dandelion_stem", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LATEX_DANDELION_FLOWER = CROPS.register("latex_dandelion_flower", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_LATEX = ITEMS.register("raw_latex", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAW_RUBBER = ITEMS.register("raw_rubber", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RUBBER_INGOT = ITEMS.register("rubber_ingot", () -> new Item(new Item.Properties()));


    public static final DeferredHolder<Item, Item> SULFUR_BERRY =
            CROPS.register("sulfur_berry",
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

    public static final DeferredHolder<Item, Item> HERBICIDE = ITEMS.register("herbicide", () -> new HerbicideItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BIOMASS = ITEMS.register("biomass", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> TRANSMISSION = registerTooltip("transmission", TooltipOptions.nameAndLore(0x8E9D7A, 1, 0x8E9D7A));
    public static final DeferredHolder<Item, Item> MAGNALIUM_ENGINE = registerTooltip("magnalium_engine", TooltipOptions.nameAndLore(0x8E9D7A, 2, 0x8E9D7A));
    public static final DeferredHolder<Item, Item> SOAP_STONE_DUST = ITEMS.register("soap_stone_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MANGANESE_OXIDE = ITEMS.register("manganese_oxide", () -> new Item(new Item.Properties().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true).component(DataComponents.RARITY, Rarity.RARE)));
    public static final DeferredHolder<Item, Item> TREATED_PLATE = ITEMS.register("treated_plate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ROLL_PLUSH_ITEM = ITEMS.register("roll_plush", () -> new BlockItem(ROLL_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> YAN_PLUSH_ITEM = ITEMS.register("yan_plush", () -> new BlockItem(YAN_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> LEDOK_PLUSH_ITEM = ITEMS.register("ledok_plush", () -> new BlockItem(LEDOK_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> LORP_OOO_PLUSH_ITEM = ITEMS.register("lorp_ooo_plush", () -> new BlockItem(LORP_OOO_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> LAURELIN_PLUSH_ITEM = ITEMS.register("laurelin_plush", () -> new BlockItem(LAURELIN_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> EVORA_PLUSH_ITEM = ITEMS.register("evora_plush", () -> new BlockItem(EVORA_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> KIBER_KABACHOK_PLUSH_ITEM = ITEMS.register("kiber_kabachok_plush", () -> new BlockItem(KIBER_KABACHOK_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> DENCHEE_PLUSH_ITEM = ITEMS.register("denchee_plush", () -> new BlockItem(DENCHEE_PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> TENWOC_PLUSH_ITEM = ITEMS.register("tenwoc__plush", () -> new BlockItem(TENWOC__PLUSH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, StormScannerItem> LV_STORM_SCANNER = ITEMS.register("lv_storm_scanner", () -> new StormScannerItem(new Item.Properties().stacksTo(1), 1, 0xff1500, 1_000_000));
    public static final DeferredHolder<Item, StormScannerItem> HV_STORM_SCANNER = ITEMS.register("hv_storm_scanner", () -> new StormScannerItem(new Item.Properties().stacksTo(1), 2, 0xff1500, 10_000_000));
    public static final DeferredHolder<Item, EnergyBatteryItem> TEST_BATTERY = ITEMS.register("nano_battery", () -> new EnergyBatteryItem(new Item.Properties().stacksTo(1), 20_000_000L, 20_000L, 200_000L, 0x00FFFF));
    // 🔋 REDSTONE BATTERY
    public static final DeferredHolder<Item, EnergyBatteryItem> REDSTONE_BATTERY =
            ITEMS.register("redstone_battery",
                    () -> new EnergyBatteryItem(
                            new Item.Properties()
                                    .stacksTo(1)
                                    .rarity(Rarity.UNCOMMON),
                            1_000_000L,   // 1M EU
                            75_000L,      // inputItem
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
                            150_000L,     // inputItem
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
                            20_000_000L,    // inputItem
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
                            20_000_000L,     // inputItem
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
                            200_000_000L,      // inputItem
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
                            500_000_000L,        // inputItem
                            500_000_000L,        // output
                            0xFFD700            // золотий
                    )
            );

    public static final DeferredHolder<Item, LunarClockItem> LUNAR_PHASE_CLOCK = ITEMS.register("moon_phase_clock", () -> new LunarClockItem(new Item.Properties()));

    public static final DeferredHolder<Item, Item> POTASSIUM_DUST = ITEMS.register("potassium_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STONE_DUST = ITEMS.register("stone_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DEEPSLATE_DUST = ITEMS.register("deepslate_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NETHERRACK_DUST = ITEMS.register("netherrack_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> END_STONE_DUST = ITEMS.register("end_stone_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MOON_STONE_DUST = ITEMS.register("moon_stone_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MARS_STONE_DUST = ITEMS.register("mars_stone_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> VENUS_STONE_DUST = ITEMS.register("venus_stone_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MERCURY_STONE_DUST = ITEMS.register("mercury_stone_dust", () -> new Item(new Item.Properties()));
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
    // TODO(drill-modules): the 9 fixed drills below are deprecated in favor of the modular drills.
    //  Kept registered so existing items in worlds survive; delete after the migration window.
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


    public static final DeferredHolder<Item, EnergyDrillItem> IV_ELECTRIC_PICKAXE = // IV БУР
            ITEMS.register("iv_electric_pickaxe",
                    () -> new EnergyDrillItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties(),
                            1_000_000_000_000L,
                            11,
                            5,
                            36000
                    )
            );

    /* ------------------------------------- modular drills ------------------------------------- */
    // The replacement for the fixed drills above: bare drills whose area, loot, speed and energy
    // behavior come from modules installed at the Module Installation Table.

    public static final DeferredHolder<Item, ModularDrillItem> LV_MODULAR_DRILL =
            ITEMS.register("lv_modular_drill", () -> new ModularDrillItem(DrillVoltage.LV, new Item.Properties()));
    public static final DeferredHolder<Item, ModularDrillItem> MV_MODULAR_DRILL =
            ITEMS.register("mv_modular_drill", () -> new ModularDrillItem(DrillVoltage.MV, new Item.Properties()));
    public static final DeferredHolder<Item, ModularDrillItem> HV_MODULAR_DRILL =
            ITEMS.register("hv_modular_drill", () -> new ModularDrillItem(DrillVoltage.HV, new Item.Properties()));
    public static final DeferredHolder<Item, ModularDrillItem> EV_MODULAR_DRILL =
            ITEMS.register("ev_modular_drill", () -> new ModularDrillItem(DrillVoltage.EV, new Item.Properties()));
    public static final DeferredHolder<Item, ModularDrillItem> IV_MODULAR_DRILL =
            ITEMS.register("iv_modular_drill", () -> new ModularDrillItem(DrillVoltage.IV, new Item.Properties()));

    /** What the strongest single-tier modules take from the complexity budget; others take 3 × tier. */
    private static final int STRONG_COMPLEXITY = 6;

    // Drill modules. value/costFactor semantics are in DrillModuleItem's javadoc; all numbers are
    // balance knobs. No voltage requirements — progression is gated by the crafting recipes.
    // Speed fits drills and sabers: a mining multiplier in a drill (cost per block as before), added
    // attack speed in a saber. The saber side keeps the old Attack Speed ceiling (+1.2, cost x1.3)
    // spread over five tiers instead of three.
    public static final DeferredHolder<Item, DrillModuleItem> SPEED_MODULE_I =
            speedModule("speed_module_i", 1, 1.5, 0.24, 1.5, 1.06);
    public static final DeferredHolder<Item, DrillModuleItem> SPEED_MODULE_II =
            speedModule("speed_module_ii", 2, 2.0, 0.48, 2.0, 1.12);
    public static final DeferredHolder<Item, DrillModuleItem> SPEED_MODULE_III =
            speedModule("speed_module_iii", 3, 3.0, 0.72, 3.0, 1.18);
    public static final DeferredHolder<Item, DrillModuleItem> SPEED_MODULE_IV =
            speedModule("speed_module_iv", 4, 4.0, 0.96, 4.0, 1.24);

    public static final DeferredHolder<Item, DrillModuleItem> SILK_TOUCH_MODULE =
            module("silk_touch_module", ModuleType.SILK_TOUCH, 1, 0, 2.0, 5);

    // Fortune in a drill, Looting in a saber. Each tool keeps its own energy balance: the first cost
    // factor is per block in a drill, the second per hit in a saber (the old Looting modules').
    public static final DeferredHolder<Item, DrillModuleItem> FORTUNE_MODULE_I =
            sharedModule("fortune_module_i", ModuleType.FORTUNE, 1, 1, 1.5, 1.1);
    public static final DeferredHolder<Item, DrillModuleItem> FORTUNE_MODULE_II =
            sharedModule("fortune_module_ii", ModuleType.FORTUNE, 2, 2, 2.0, 1.2);
    public static final DeferredHolder<Item, DrillModuleItem> FORTUNE_MODULE_III =
            sharedModule("fortune_module_iii", ModuleType.FORTUNE, 3, 3, 2.5, 1.3);
    public static final DeferredHolder<Item, DrillModuleItem> FORTUNE_MODULE_IV =
            sharedModule("fortune_module_iv", ModuleType.FORTUNE, 4, 4, 3.0, 1.4);
    public static final DeferredHolder<Item, DrillModuleItem> FORTUNE_MODULE_V =
            sharedModule("fortune_module_v", ModuleType.FORTUNE, 5, 5, 4.0, 1.5);

    public static final DeferredHolder<Item, DrillModuleItem> XP_REACTOR_MODULE =
            module("xp_reactor_module", ModuleType.XP_REACTOR, 1, 0, 1.0, 5);
    public static final DeferredHolder<Item, DrillModuleItem> OVERCHARGE_MODULE =
            module("overcharge_module", ModuleType.OVERCHARGE, 1, 0, 1.0, STRONG_COMPLEXITY);

    public static final DeferredHolder<Item, DrillModuleItem> REACH_MODULE_I =
            module("reach_module_i", ModuleType.REACH, 1, 2, 1.0);
    public static final DeferredHolder<Item, DrillModuleItem> REACH_MODULE_II =
            module("reach_module_ii", ModuleType.REACH, 2, 4, 1.0);
    public static final DeferredHolder<Item, DrillModuleItem> REACH_MODULE_III =
            module("reach_module_iii", ModuleType.REACH, 3, 8, 1.0);

    public static final DeferredHolder<Item, DrillModuleItem> BURN_MODULE =
            module("burn_module", ModuleType.BURN, 1, 0, 0.9);

    public static final DeferredHolder<Item, DrillModuleItem> BATTERY_MODULE_I =
            module("battery_module_i", ModuleType.BATTERY, 1, 1.10, 1.0);
    public static final DeferredHolder<Item, DrillModuleItem> BATTERY_MODULE_II =
            module("battery_module_ii", ModuleType.BATTERY, 2, 1.25, 1.0);
    public static final DeferredHolder<Item, DrillModuleItem> BATTERY_MODULE_III =
            module("battery_module_iii", ModuleType.BATTERY, 3, 1.50, 1.0);
    public static final DeferredHolder<Item, DrillModuleItem> BATTERY_MODULE_IV =
            module("battery_module_iv", ModuleType.BATTERY, 4, 2.00, 1.0);

    public static final DeferredHolder<Item, DrillModuleItem> SPEED_MODULE_V =
            speedModule("speed_module_v", 5, 6.0, 1.2, 6.0, 1.3);
    public static final DeferredHolder<Item, DrillModuleItem> BATTERY_MODULE_V =
            module("battery_module_v", ModuleType.BATTERY, 5, 3.00, 1.0);

    public static final DeferredHolder<Item, DrillModuleItem> AUTO_SMELT_MODULE =
            module("auto_smelt_module", ModuleType.SMELT, 1, 0, 1.5);
    public static final DeferredHolder<Item, DrillModuleItem> TRASH_FILTER_MODULE =
            module("trash_filter_module", ModuleType.TRASH_FILTER, 1, 0, 0.95);
    public static final DeferredHolder<Item, DrillModuleItem> ECO_MODULE_I =
            module("eco_module_i", ModuleType.ECO, 1, 0, 0.85);
    public static final DeferredHolder<Item, DrillModuleItem> ECO_MODULE_II =
            module("eco_module_ii", ModuleType.ECO, 2, 0, 0.70);
    // Cost factor 1.0: it charges per solidified fluid block instead, in mineArea.
    public static final DeferredHolder<Item, DrillModuleItem> LAVA_SOLIDIFIER_MODULE =
            module("lava_solidifier_module", ModuleType.LAVA_SOLIDIFIER, 1, 0, 1.0);

    // Talks in chat every 10k mined blocks; no effect on mining, so no cost — and it gives 5
    // complexity back, the one module that does.
    public static final DeferredHolder<Item, DrillModuleItem> SPEAK_MODULE =
            module("speak_module", ModuleType.SPEAK, 1, 0, 1.0, -5);

    // Areas taken from the retired advanced drills: 3³, 5³, 7³, 9³ — and the IV pickaxe's 11³.
    public static final DeferredHolder<Item, DrillModuleItem> AOE_MODULE_I =
            module("aoe_module_i", ModuleType.AOE, 1, 1, 1.5);
    public static final DeferredHolder<Item, DrillModuleItem> AOE_MODULE_II =
            module("aoe_module_ii", ModuleType.AOE, 2, 2, 2.0);
    public static final DeferredHolder<Item, DrillModuleItem> AOE_MODULE_III =
            module("aoe_module_iii", ModuleType.AOE, 3, 3, 2.5);
    public static final DeferredHolder<Item, DrillModuleItem> AOE_MODULE_IV =
            module("aoe_module_iv", ModuleType.AOE, 4, 4, 3.0);
    public static final DeferredHolder<Item, DrillModuleItem> AOE_MODULE_V =
            module("aoe_module_v", ModuleType.AOE, 5, 5, 3.5);

    /* ------------------------------------- modular sabers ------------------------------------- */
    // The replacement for the fixed nano sabers below: switchable blades whose damage, energy use and
    // on-hit effects come from modules installed at the same Module Installation Table as the drills.
    // Battery, Eco and the XP Reactor above fit both tools; the modules below are saber-only.

    public static final DeferredHolder<Item, ModularSaberItem> LV_MODULAR_SABER =
            ITEMS.register("lv_modular_saber", () -> new ModularSaberItem(SaberVoltage.LV, new Item.Properties()));
    public static final DeferredHolder<Item, ModularSaberItem> MV_MODULAR_SABER =
            ITEMS.register("mv_modular_saber", () -> new ModularSaberItem(SaberVoltage.MV, new Item.Properties()));
    public static final DeferredHolder<Item, ModularSaberItem> HV_MODULAR_SABER =
            ITEMS.register("hv_modular_saber", () -> new ModularSaberItem(SaberVoltage.HV, new Item.Properties()));
    public static final DeferredHolder<Item, ModularSaberItem> EV_MODULAR_SABER =
            ITEMS.register("ev_modular_saber", () -> new ModularSaberItem(SaberVoltage.EV, new Item.Properties()));
    public static final DeferredHolder<Item, ModularSaberItem> IV_MODULAR_SABER =
            ITEMS.register("iv_modular_saber", () -> new ModularSaberItem(SaberVoltage.IV, new Item.Properties()));

    // Saber modules. value/costFactor semantics are in DrillModuleItem's javadoc; all numbers are
    // balance knobs, and docs/drill-modules.txt lists them.
    public static final DeferredHolder<Item, DrillModuleItem> DAMAGE_MODULE_I =
            module("damage_module_i", ModuleType.DAMAGE, 1, 1.25, 1.25);
    public static final DeferredHolder<Item, DrillModuleItem> DAMAGE_MODULE_II =
            module("damage_module_ii", ModuleType.DAMAGE, 2, 1.50, 1.50);
    public static final DeferredHolder<Item, DrillModuleItem> DAMAGE_MODULE_III =
            module("damage_module_iii", ModuleType.DAMAGE, 3, 2.00, 2.00);
    public static final DeferredHolder<Item, DrillModuleItem> DAMAGE_MODULE_IV =
            module("damage_module_iv", ModuleType.DAMAGE, 4, 2.50, 2.50);
    public static final DeferredHolder<Item, DrillModuleItem> DAMAGE_MODULE_V =
            module("damage_module_v", ModuleType.DAMAGE, 5, 3.00, 3.00);

    // Deprecated: merged into the Speed modules. Kept registered so existing items and installed
    // modules still load and work; left out of the creative tab, and never installable beside Speed.
    public static final DeferredHolder<Item, DrillModuleItem> ATTACK_SPEED_MODULE_I =
            module("attack_speed_module_i", ModuleType.ATTACK_SPEED, 1, 0.4, 1.1);
    public static final DeferredHolder<Item, DrillModuleItem> ATTACK_SPEED_MODULE_II =
            module("attack_speed_module_ii", ModuleType.ATTACK_SPEED, 2, 0.8, 1.2);
    public static final DeferredHolder<Item, DrillModuleItem> ATTACK_SPEED_MODULE_III =
            module("attack_speed_module_iii", ModuleType.ATTACK_SPEED, 3, 1.2, 1.3);

    // Cost factor 1.0: each extra mob a sweep reaches is charged half a hit on its own.
    public static final DeferredHolder<Item, DrillModuleItem> SWEEP_MODULE_I =
            module("sweep_module_i", ModuleType.SWEEP, 1, 1.5, 1.0);
    public static final DeferredHolder<Item, DrillModuleItem> SWEEP_MODULE_II =
            module("sweep_module_ii", ModuleType.SWEEP, 2, 2.5, 1.0);
    public static final DeferredHolder<Item, DrillModuleItem> SWEEP_MODULE_III =
            module("sweep_module_iii", ModuleType.SWEEP, 3, 3.5, 1.0);

    // DEPRECATED: merged into the Fortune/Looting modules above. Kept registered so existing stacks
    // and already-installed modules load and keep working; no longer in the creative tab.
    public static final DeferredHolder<Item, DrillModuleItem> LOOTING_MODULE_I =
            module("looting_module_i", ModuleType.LOOTING, 1, 1, 1.1);
    public static final DeferredHolder<Item, DrillModuleItem> LOOTING_MODULE_II =
            module("looting_module_ii", ModuleType.LOOTING, 2, 2, 1.2);
    public static final DeferredHolder<Item, DrillModuleItem> LOOTING_MODULE_III =
            module("looting_module_iii", ModuleType.LOOTING, 3, 3, 1.3);
    public static final DeferredHolder<Item, DrillModuleItem> LOOTING_MODULE_IV =
            module("looting_module_iv", ModuleType.LOOTING, 4, 4, 1.4);
    public static final DeferredHolder<Item, DrillModuleItem> LOOTING_MODULE_V =
            module("looting_module_v", ModuleType.LOOTING, 5, 5, 1.5);

    public static final DeferredHolder<Item, DrillModuleItem> XP_MULTIPLIER_MODULE_I =
            module("xp_multiplier_module_i", ModuleType.XP_MULTIPLIER, 1, 1.5, 1.1);
    public static final DeferredHolder<Item, DrillModuleItem> XP_MULTIPLIER_MODULE_II =
            module("xp_multiplier_module_ii", ModuleType.XP_MULTIPLIER, 2, 2.0, 1.2);
    public static final DeferredHolder<Item, DrillModuleItem> XP_MULTIPLIER_MODULE_III =
            module("xp_multiplier_module_iii", ModuleType.XP_MULTIPLIER, 3, 3.0, 1.4);
    public static final DeferredHolder<Item, DrillModuleItem> XP_MULTIPLIER_MODULE_IV =
            module("xp_multiplier_module_iv", ModuleType.XP_MULTIPLIER, 4, 4.0, 1.6);
    public static final DeferredHolder<Item, DrillModuleItem> XP_MULTIPLIER_MODULE_V =
            module("xp_multiplier_module_v", ModuleType.XP_MULTIPLIER, 5, 5.0, 1.8);

    public static final DeferredHolder<Item, DrillModuleItem> DROP_COLLECTOR_MODULE =
            module("drop_collector_module", ModuleType.DROP_COLLECTOR, 1, 0, 1.05);

    public static final DeferredHolder<Item, DrillModuleItem> VAMPIRISM_MODULE_I =
            module("vampirism_module_i", ModuleType.VAMPIRISM, 1, 0.05, 1.1);
    public static final DeferredHolder<Item, DrillModuleItem> VAMPIRISM_MODULE_II =
            module("vampirism_module_ii", ModuleType.VAMPIRISM, 2, 0.10, 1.2);
    public static final DeferredHolder<Item, DrillModuleItem> VAMPIRISM_MODULE_III =
            module("vampirism_module_iii", ModuleType.VAMPIRISM, 3, 0.15, 1.3);

    public static final DeferredHolder<Item, DrillModuleItem> BEHEADING_MODULE_I =
            module("beheading_module_i", ModuleType.BEHEADING, 1, 0.05, 1.0);
    public static final DeferredHolder<Item, DrillModuleItem> BEHEADING_MODULE_II =
            module("beheading_module_ii", ModuleType.BEHEADING, 2, 0.15, 1.0);
    public static final DeferredHolder<Item, DrillModuleItem> BEHEADING_MODULE_III =
            module("beheading_module_iii", ModuleType.BEHEADING, 3, 0.30, 1.0);

    public static final DeferredHolder<Item, DrillModuleItem> IRRADIATION_MODULE_I =
            module("irradiation_module_i", ModuleType.IRRADIATION, 1, 1, 1.1);
    public static final DeferredHolder<Item, DrillModuleItem> IRRADIATION_MODULE_II =
            module("irradiation_module_ii", ModuleType.IRRADIATION, 2, 2, 1.2);
    public static final DeferredHolder<Item, DrillModuleItem> IRRADIATION_MODULE_III =
            module("irradiation_module_iii", ModuleType.IRRADIATION, 3, 3, 1.3);

    // Looks only: the meteorite blade (and its skins) on any modular saber. No cost.
    public static final DeferredHolder<Item, DrillModuleItem> METEORITE_MODULE =
            module("meteorite_module", ModuleType.METEORITE, 1, 0, 1.0, STRONG_COMPLEXITY);

    private static DeferredHolder<Item, DrillModuleItem> module(
            String id, ModuleType type, int tier, double value, double costFactor, int complexity) {
        return ITEMS.register(id, () -> new DrillModuleItem(type, tier, value, costFactor, complexity,
                new Item.Properties()));
    }

    /** A module shared by drills and sabers whose energy cost differs per tool. */
    private static DeferredHolder<Item, DrillModuleItem> sharedModule(
            String id, ModuleType type, int tier, double value, double drillCostFactor, double saberCostFactor) {
        return ITEMS.register(id, () -> new DrillModuleItem(type, tier, value, drillCostFactor, saberCostFactor,
                DrillModuleItem.COMPLEXITY_PER_TIER * tier, new Item.Properties()));
    }

    /** A Speed module: its effect and its energy cost both differ between drill and saber. */
    private static DeferredHolder<Item, DrillModuleItem> speedModule(String id, int tier,
            double drillSpeed, double saberAttackSpeed, double drillCostFactor, double saberCostFactor) {
        return ITEMS.register(id, () -> new DrillModuleItem(ModuleType.SPEED, tier, drillSpeed, saberAttackSpeed,
                drillCostFactor, saberCostFactor, DrillModuleItem.COMPLEXITY_PER_TIER * tier, new Item.Properties()));
    }

    private static DeferredHolder<Item, DrillModuleItem> module(
            String id, ModuleType type, int tier, double value, double costFactor) {
        return ITEMS.register(id, () -> new DrillModuleItem(type, tier, value, costFactor, new Item.Properties()));
    }

    /**
     * MI's steam mining drill in a netherite frame: fireproof, and {@value
     * com.roll_54.roll_mod.items.NetheriteSteamDrillItem#SPEED_BONUS_PERCENT}% faster to mine with.
     * Everything else about it — water, fuel, 3x3, silk touch — is MI's.
     */
    public static final DeferredHolder<Item, NetheriteSteamDrillItem> NETHERITE_STEAM_MINING_DRILL =
            ITEMS.register("netherite_steam_mining_drill",
                    () -> new NetheriteSteamDrillItem(new Item.Properties())
            );

    public static final DeferredHolder<Item, ProspectorPickItem> PROSPECTOR_PICK_ITEM =
            ITEMS.register("prospector_pickaxe",
                    () -> new ProspectorPickItem(
                            Tiers.IRON,
                            new Item.Properties().durability(512).rarity(Rarity.UNCOMMON)
                    )

            );

    public static final DeferredHolder<Item, ProspectorPickItem> METEORITE_METAL_PROSPECTOR_PICKAXE =
            ITEMS.register(
                    "meteorite_metal_prospector_pickaxe",
                    () -> new ProspectorPickItem(
                            ModToolTiers.METEORITE_METAL,
                            new Item.Properties()
                                    .durability(4096)
                                    .rarity(Rarity.UNCOMMON)
                                    .component(
                                            DataComponents.CUSTOM_NAME,
                                            Component.translatable(
                                                            "item.roll_mod.meteorite_metal_prospector_pickaxe"
                                                    )
                                                    .withStyle(style ->
                                                            style.withColor(METEORITE_DARK_BLUE)
                                                                    .withItalic(false)
                                                    )
                                    )
                    )
            );

    public static final DeferredHolder<Item, Item> SHCHEDRYK_MUSIC_DISC =
            ITEMS.register(
                    "shchedryk_music_disc",
                    () -> new Item(
                            new Item.Properties()
                                    .jukeboxPlayable(SoundRegistry.SHCHEDRYK_KEY)
                                    .stacksTo(1)
                                    .rarity(Rarity.RARE)
                    )
            );

    public static final DeferredHolder<Item, EnergySwordItem> ENERGY_SWORD =
            ITEMS.register(
                    "energy_sword",
                    () -> new EnergySwordItem(
                            new Item.Properties(),
                            1_000_000L,
                            40_000L,
                            0.5,
                            10.0
                    )
            );

    public static final DeferredHolder<Item, EnergySwordItem> METEORITE_METAL_NANO_SABER =
            ITEMS.register(
                    "meteorite_metal_nano_saber",
                    () -> new EnergySwordItem(
                            new Item.Properties()
                                    .component(
                                            DataComponents.LORE,
                                            new ItemLore(
                                                    List.of(
                                                            Component.translatable(
                                                                            "tooltip.roll_mod.meteorite_metal_nano_saber"
                                                                    )
                                                                    .withStyle(style ->
                                                                            style.withColor(METEORITE_LIGHT_BLUE)
                                                                                    .withItalic(false)
                                                                    )

                                                    )
                                            )
                                    )
                                    .component(
                                            DataComponents.CUSTOM_NAME,
                                            Component.translatable(
                                                            "item.roll_mod.meteorite_metal_nano_saber"
                                                    )
                                                    .withStyle(style ->
                                                            style.withColor(METEORITE_DARK_BLUE)
                                                                    .withItalic(false)
                                                    )
                                    ),
                            54_000_000L,
                            100_000L,
                            0.5,
                            25.0
                    )
            );

    public static final DeferredHolder<Item, EnergySwordItem> MV_ELECTRIC_SABER =
            ITEMS.register(
                    "mv_electric_saber",
                    () -> new EnergySwordItem(
                            new Item.Properties(),
                            1_000_000L,
                            50_000L,
                            0.5,
                            15.0
                    )
            );

    public static final DeferredHolder<Item, EnergySwordItem> HV_ELECTRIC_SABER =
            ITEMS.register(
                    "hv_electric_saber",
                    () -> new EnergySwordItem(
                            new Item.Properties(),
                            1_000_000_000L,
                            1_000_000L,
                            0.5,
                            35.0
                    )
            );

    public static final DeferredHolder<Item, EnergySwordItem> EV_ELECTRIC_SABER =
            ITEMS.register(
                    "ev_electric_saber",
                    () -> new EnergySwordItem(
                            new Item.Properties(),
                            10_000_000_000L,
                            10_000_000L,
                            0.5,
                            55.0
                    )
            );

    public static final DeferredHolder<Item, EnergySwordItem> IV_ELECTRIC_SABER =
            ITEMS.register(
                    "iv_electric_saber",
                    () -> new EnergySwordItem(
                            new Item.Properties(),
                            1_000_000_000_000L,
                            100_000_000L,
                            0.5,
                            154.0
                    )
            );

    public static final DeferredHolder<Item, Item> SKIN_APPLICATOR = ITEMS.register("skin_applicator", () -> new ComponentApplicatorItem(new Item.Properties()));

    // ── Research Blueprints ────────────────────────────────────────────────────
    public static final DeferredHolder<Item, Item> BLUEPRINT_FIRE_RESISTANCE = registerSimple("blueprint_fire_resistance", new Item.Properties().stacksTo(1));

    // Rocket Cartridges — each carries its target dimension via CartridgeData component
    public static final DeferredHolder<Item, Item> OVERWORLD_CARTRIDGE = ITEMS.register("overworld_cartridge",
            () -> new DimensionCartridgeCItem(new Item.Properties().stacksTo(1),
                    "minecraft:overworld", "dimension.minecraft.overworld", 5, 1));

    public static final DeferredHolder<Item, Item> NETHER_CARTRIDGE = ITEMS.register("nether_cartridge",
            () -> new DimensionCartridgeCItem(new Item.Properties().stacksTo(1),
                    "minecraft:the_nether", "dimension.minecraft.the_nether", 5, 1));

    public static final DeferredHolder<Item, Item> END_CARTRIDGE = ITEMS.register("end_cartridge",
            () -> new DimensionCartridgeCItem(new Item.Properties().stacksTo(1),
                    "minecraft:the_end", "dimension.minecraft.the_end", 15, 1));

    public static final DeferredHolder<Item, Item> MOON_CARTRIDGE = ITEMS.register("moon_cartridge",
            () -> new DimensionCartridgeCItem(new Item.Properties().stacksTo(1),
                    "stellaris:moon", "dimension.ad_astra.moon", 10, 1));

    public static final DeferredHolder<Item, Item> MARS_CARTRIDGE = ITEMS.register("mars_cartridge",
            () -> new DimensionCartridgeCItem(new Item.Properties().stacksTo(1),
                    "stellaris:mars", "dimension.ad_astra.mars", 10, 2));

    public static final DeferredHolder<Item, Item> VENUS_CARTRIDGE = ITEMS.register("venus_cartridge",
            () -> new DimensionCartridgeCItem(new Item.Properties().stacksTo(1),
                    "stellaris:venus", "dimension.ad_astra.venus", 50, 3));

    public static final DeferredHolder<Item, Item> MERCURY_CARTRIDGE = ITEMS.register("mercury_cartridge",
            () -> new DimensionCartridgeCItem(new Item.Properties().stacksTo(1),
                    "stellaris:mercury", "dimension.ad_astra.mercury", 10, 3));

    public static final DeferredHolder<Item, Item> EMPTY_CARTRIDGE =
            ITEMS.register("empty_cartridge", () -> new Item(new Item.Properties()));

    // ── Rockets ───────────────────────────────────────────────────────────────
    public static final DeferredHolder<Item, RocketItem> TIER_1_ROCKET =
            ITEMS.register("tier_1_rocket", () -> new RocketItem(
                    new Item.Properties().stacksTo(1), 1));
    public static final DeferredHolder<Item, RocketItem> TIER_2_ROCKET =
            ITEMS.register("tier_2_rocket", () -> new RocketItem(
                    new Item.Properties().stacksTo(1), 2));
    public static final DeferredHolder<Item, RocketItem> TIER_3_ROCKET =
            ITEMS.register("tier_3_rocket", () -> new RocketItem(
                    new Item.Properties().stacksTo(1), 3));

    // ── Rocket Fuel ───────────────────────────────────────────────────────────
    // Stack up to 64 so players can fill the fuel slot easily
    public static final DeferredHolder<Item, Item> ROCKET_FUEL_CANISTER =
            ITEMS.register("rocket_fuel_canister", () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, Item> FUEL_CANISTER =
            ITEMS.register("fuel_canister", () -> new Item(new Item.Properties().stacksTo(64)));


    public static final DeferredHolder<Item, Item> LITHIUM_SOAP =
            ITEMS.register("lithium_soap", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> POTASSIUM_SOAP =
            ITEMS.register("potassium_soap", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> SODIUM_SOAP =
            ITEMS.register("sodium_soap", () -> new Item(new Item.Properties()));


    public static final DeferredHolder<Item, ExampleArmorItem> EXAMPLE_ARMOR_HELMET = ITEMS.register("example_armor_helmet", () -> new ExampleArmorItem(ModArmorMaterials.METEORITE_ARMOR, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final DeferredHolder<Item, ClownHatArmorItem> CLOWN_HAT =
            ITEMS.register("clown_hat", () -> new ClownHatArmorItem(
                    ArmorMaterials.NETHERITE,
                    ArmorItem.Type.HELMET,
                    new Item.Properties()
            ) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.roll_mod.clown_hat.line1")
                            .withStyle(style -> style.withColor(0xFF5555)));

                    tooltip.add(Component.translatable("tooltip.roll_mod.clown_hat.line2")
                            .withStyle(style -> style.withColor(0xFFAA00)));
                }
            });

    public static final DeferredHolder<Item, MultiProtectingGraviChestItem> MULTI_PROTECTING_GRAVI_CHESTPLATE =
            ITEMS.register("multi_protecting_gravi_chestplate", () -> new MultiProtectingGraviChestItem(ModArmorMaterials.METEORITE_ARMOR, new Item.Properties()));

    /**
     * Not a real item — the animatable a cosmetic helmet skin renders as. See {@link SkinAnchorItem}
     * for why it has to be registered rather than constructed on demand. Deliberately absent from
     * every creative tab: {@code ItemGroups} lists its contents explicitly, so omission is enough.
     */
    public static final DeferredHolder<Item, SkinAnchorItem> SKIN_ANCHOR =
            ITEMS.register("skin_anchor", () -> new SkinAnchorItem(new Item.Properties().stacksTo(1)));


    public static final DeferredHolder<Item, Item> PRIMITIVE_BATTERY = ITEMS.register("primitive_battery", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> EMPTY_PRIMITIVE_BATTERY = ITEMS.register("empty_primitive_battery", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> POTASSIUM_NITRITE_DUST = ITEMS.register("potassium_nitrite_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SODIUM_NITRITE_DUST = ITEMS.register("sodium_nitrite_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FLINT_DUST = ITEMS.register("flint_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DIORITE_DUST = ITEMS.register("diorite_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ANDESITE_DUST = ITEMS.register("andesite_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> GRANITE_DUST = ITEMS.register("granite_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FELDSPARS_DUST = ITEMS.register("feldspars_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> METAL_MIXTURE_DUST = ITEMS.register("metal_mixture_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MARBLE_DUST = ITEMS.register("marble_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CALCITE_DUST = ITEMS.register("calcite_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BASALT_DUST = ITEMS.register("basalt_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> TUFF_DUST = ITEMS.register("tuff_dust", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SULFUR_SALTPETER_MIXTURE = ITEMS.register("sulfur_saltpeter_mixture", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ZINC_SULFUR_MIXTURE = ITEMS.register("zinc_sulfur_mixture", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> UNACTIVATED_ZINC_SULFIDE_POWDER = ITEMS.register("unactivated_zinc_sulfide_powder", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> LUMINESCENT_MIXTURE = ITEMS.register("luminescent_mixture", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SYNTHETIC_GLOWSTONE_DUST = ITEMS.register("synthetic_glowstone_dust", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> ICEBERG_MINT_SEEDS = CROPS.register("iceberg_mint_seeds", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> ICEBERG_MINT_LEAF = CROPS.register("iceberg_mint_leaf", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> NIKELIA_FLOWERS = CROPS.register("nikelia_flowers", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NIKELIA_SEEDS = CROPS.register("nikelia_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STONELIA_FLOWERS = CROPS.register("stonelia_flowers", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> STONELIA_SEEDS = CROPS.register("stonelia_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RED_BELL_PEPPER_SEEDS = CROPS.register("red_bell_pepper_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> YELLOW_BELL_PEPPER_SEEDS = CROPS.register("yellow_bell_pepper_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> GREEN_BELL_PEPPER_SEEDS = CROPS.register("green_bell_pepper_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SULFUR_BERRY_COFFEE_BEANS = CROPS.register("sulfur_berry_coffee_beans", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> SULFUR_BERRY_COFFEE_SEEDS = CROPS.register("sulfur_berry_coffee_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FLUORITE_PINEAPPLE = CROPS.register("fluorite_pineapple", () -> new Item(new Item.Properties().food(
            new FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(0.4f)
                    .build()
    )));
    public static final DeferredHolder<Item, Item> FLUORITE_PINEAPPLE_SEEDS = CROPS.register("fluorite_pineapple_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> HOPS_LEAF = CROPS.register("hops_leaf", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> HOPS_SEEDS = CROPS.register("hops_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RUTILE_BELL_PEPPER = CROPS.register("rutile_bell_pepper", () -> new Item(new Item.Properties().food(
            new FoodProperties.Builder()
                    .nutrition(4)
                    .saturationModifier(0.4f)
                    .build()
    )));
    public static final DeferredHolder<Item, Item> RUTILE_BELL_PEPPER_SEEDS = CROPS.register("rutile_bell_pepper_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> COFFEE_BEANS = CROPS.register("coffee_beans", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> COFFEE_SEEDS = CROPS.register("coffee_seeds", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MAGNETITE_SPINACH = CROPS.register("magnetite_spinach", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MAGNETITE_SPINACH_SEEDS = CROPS.register("magnetite_spinach_seeds", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> HERBICIDE_TIER_1 = ITEMS.register(
            "herbicide_tier_1",
            () -> new HerbicideItem(
                    new Item.Properties()
                            .component(
                                    DataComponents.LORE,
                                    new ItemLore(List.of(
                                            Component.translatable(
                                                    "tooltip.roll_mod.herbicide_tier_1"
                                            ).withStyle(style -> style.withColor(0x44ff8e))
                                    ))
                            )
                            .component(
                                    ComponentsRegistry.HERBICIDE.get(),
                                    200
                            )
            )
    );
    public static final DeferredHolder<Item, Item> HERBICIDE_TIER_2 = ITEMS.register("herbicide_tier_2", () -> new HerbicideItem(new Item.Properties()
            .component(
                    DataComponents.LORE,
                    new ItemLore(List.of(
                            Component.translatable(
                                    "tooltip.roll_mod.herbicide_tier_2"
                            ).withStyle(style -> style.withColor(0xff4b41))
                    ))
            )
            .component(
                    ComponentsRegistry.HERBICIDE.get(),
                    1000
            )
    ));
    public static final DeferredHolder<Item, Item> HERBICIDE_TIER_3 = ITEMS.register("herbicide_tier_3", () -> new HerbicideItem(new Item.Properties()
            .component(
                    DataComponents.LORE,
                    new ItemLore(List.of(
                            Component.translatable(
                                    "tooltip.roll_mod.herbicide_tier_3"
                            ).withStyle(style -> style.withColor(0x632cff))
                    ))
            )
            .component(
                    ComponentsRegistry.HERBICIDE.get(),
                    4000
            )));

    public static final DeferredHolder<Item, Item> ALUMINA_CERAMIC_PLATE = ITEMS.register("alumina_ceramic_plate", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> ALUMINA_SMALL_CERAMIC_PLATE = ITEMS.register("alumina_small_ceramic_plate", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> IRIDIUM_BASED_BOARD = ITEMS.register("iridium_based_board", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> IRIDIUM_BASED_BOARD_ASSEMBLY = ITEMS.register("iridium_based_board_assembly", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> NYLON_STRING = ITEMS.register("nylon_string", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> NYLON_FABRIC = ITEMS.register("nylon_fabric", () -> new Item(new Item.Properties()));

    /** Armor granted by each meteorite metal cyberlimb (full set of 4 = +32). Shown in each tooltip. */
    private static final int METEORITE_LIMB_ARMOR = 8;

    public static final DeferredHolder<Item, CyberarmItem> BASECYBERWARE_RIGHTARM_METEORITE_METAL = ITEMS.register("basecyberware_rightarm_meteorite_metal",
            () -> new CyberarmItem(new Item.Properties().stacksTo(1).component(
                    DataComponents.CUSTOM_NAME,
                    Component.translatable(
                                    "item.roll_mod.basecyberware_rightarm_meteorite_metal"
                            )
                            .withStyle(style ->
                                    style.withColor(METEORITE_DARK_BLUE)
                                            .withItalic(false)
                            )
            ), 5, CyberwareSlot.RARM) {
                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.createcybernetics.basecyberware_tooltip"));
                    if (Screen.hasShiftDown()) {
                        tooltipComponents.add(Component.translatable("tooltip.roll_mod.basecyberware_meteorite_metal.desc").withStyle(ChatFormatting.GRAY));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.createcybernetics.hold_shift_down"));
                    }
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }

                @Override
                public void onInstalled(LivingEntity entity) {
                    CyberwareAttributeHelper.applyModifier(entity, "basecyberware_rightarm_meteorite_metal");
                }
                @Override
                public void onRemoved(LivingEntity entity) {
                    CyberwareAttributeHelper.removeModifier(entity, "basecyberware_rightarm_meteorite_metal");
                }

                static {
                    CyberwareAttributeHelper.registerModifierDynamicAttribute(
                            "basecyberware_rightarm_meteorite_metal",
                            Attributes.ARMOR.unwrapKey().orElseThrow().location(),
                            ResourceLocation.fromNamespaceAndPath("roll_mod", "basecyberware_rightarm_meteorite_metal_armor"),
                            METEORITE_LIMB_ARMOR,
                            AttributeModifier.Operation.ADD_VALUE);
                }
            });

    public static final DeferredHolder<Item, CyberarmItem> BASECYBERWARE_LEFTARM_METEORITE_METAL = ITEMS.register("basecyberware_leftarm_meteorite_metal",
            () -> new CyberarmItem(new Item.Properties().stacksTo(1).component(
                    DataComponents.CUSTOM_NAME,
                    Component.translatable(
                                    "item.roll_mod.basecyberware_leftarm_meteorite_metal"
                            )
                            .withStyle(style ->
                                    style.withColor(METEORITE_DARK_BLUE)
                                            .withItalic(false)
                            )
            ), 5, CyberwareSlot.LARM) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.createcybernetics.basecyberware_tooltip"));
                    if (Screen.hasShiftDown()) {
                        tooltipComponents.add(Component.translatable("tooltip.roll_mod.basecyberware_meteorite_metal.desc").withStyle(ChatFormatting.GRAY));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.createcybernetics.hold_shift_down"));
                    }
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }

                @Override
                public void onInstalled(LivingEntity entity) {
                    CyberwareAttributeHelper.applyModifier(entity, "basecyberware_leftarm_meteorite_metal");
                }
                @Override
                public void onRemoved(LivingEntity entity) {
                    CyberwareAttributeHelper.removeModifier(entity, "basecyberware_leftarm_meteorite_metal");
                }



                static {
                    CyberwareAttributeHelper.registerModifierDynamicAttribute(
                            "basecyberware_leftarm_meteorite_metal",
                            Attributes.ARMOR.unwrapKey().orElseThrow().location(),
                            ResourceLocation.fromNamespaceAndPath("roll_mod", "basecyberware_leftarm_meteorite_metal_armor"),
                            METEORITE_LIMB_ARMOR,
                            AttributeModifier.Operation.ADD_VALUE);
                }
            });

    public static final DeferredHolder<Item, CyberlegItem> BASECYBERWARE_RIGHTLEG_METEORITE_METAL = ITEMS.register("basecyberware_rightleg_meteorite_metal",
            () -> new CyberlegItem(new Item.Properties().stacksTo(1).component(
                    DataComponents.CUSTOM_NAME,
                    Component.translatable(
                                    "item.roll_mod.basecyberware_rightleg_meteorite_metal"
                            )
                            .withStyle(style ->
                                    style.withColor(METEORITE_DARK_BLUE)
                                            .withItalic(false)
                            )
            ), 5, CyberwareSlot.RLEG) {
                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.createcybernetics.basecyberware_tooltip"));
                    if (Screen.hasShiftDown()) {
                        tooltipComponents.add(Component.translatable("tooltip.roll_mod.basecyberware_meteorite_metal.desc").withStyle(ChatFormatting.GRAY));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.createcybernetics.hold_shift_down"));
                    }
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }

                @Override
                public void onInstalled(LivingEntity entity) {
                    CyberwareAttributeHelper.applyModifier(entity, "basecyberware_rightleg_meteorite_metal");
                }
                @Override
                public void onRemoved(LivingEntity entity) {
                    CyberwareAttributeHelper.removeModifier(entity, "basecyberware_rightleg_meteorite_metal");
                }




                static {
                    CyberwareAttributeHelper.registerModifierDynamicAttribute(
                            "basecyberware_rightleg_meteorite_metal",
                            Attributes.ARMOR.unwrapKey().orElseThrow().location(),
                            ResourceLocation.fromNamespaceAndPath("roll_mod", "basecyberware_rightleg_meteorite_metal_armor"),
                            METEORITE_LIMB_ARMOR,
                            AttributeModifier.Operation.ADD_VALUE);
                }
            });
    public static final DeferredHolder<Item, CyberlegItem> BASECYBERWARE_LEFTLEG_METEORITE_METAL = ITEMS.register("basecyberware_leftleg_meteorite_metal",
            () -> new CyberlegItem(new Item.Properties().stacksTo(1).component(
                    DataComponents.CUSTOM_NAME,
                    Component.translatable(
                                    "item.roll_mod.basecyberware_leftleg_meteorite_metal"
                            )
                            .withStyle(style ->
                                    style.withColor(METEORITE_DARK_BLUE)
                                            .withItalic(false)
                            )
            ), 5, CyberwareSlot.LLEG) {
                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.createcybernetics.basecyberware_tooltip"));
                    if (Screen.hasShiftDown()) {
                        tooltipComponents.add(Component.translatable("tooltip.roll_mod.basecyberware_meteorite_metal.desc").withStyle(ChatFormatting.GRAY));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.createcybernetics.hold_shift_down"));
                    }
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }

                @Override
                public void onInstalled(LivingEntity entity) {
                    CyberwareAttributeHelper.applyModifier(entity, "basecyberware_leftleg_meteorite_metal");
                }
                @Override
                public void onRemoved(LivingEntity entity) {
                    CyberwareAttributeHelper.removeModifier(entity, "basecyberware_leftleg_meteorite_metal");
                }

                static {
                    CyberwareAttributeHelper.registerModifierDynamicAttribute(
                            "basecyberware_leftleg_meteorite_metal",
                            Attributes.ARMOR.unwrapKey().orElseThrow().location(),
                            ResourceLocation.fromNamespaceAndPath("roll_mod", "basecyberware_leftleg_meteorite_metal_armor"),
                            METEORITE_LIMB_ARMOR,
                            AttributeModifier.Operation.ADD_VALUE);
                }
            });

    public static final DeferredHolder<Item, SulfurResistantLungsItem> LUNGSUPGRADE_SULFUR_RESISTANCE =
            ITEMS.register("lungsupgrade_sulfur_resistance",
                    () -> new SulfurResistantLungsItem(new Item.Properties().stacksTo(1), 5) {
                        @Override
                        public void appendHoverText(
                                ItemStack stack,
                                Item.TooltipContext context,
                                List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag
                        ) {
                            tooltipComponents.add(Component.translatable(
                                    "tooltip.createcybernetics.basecyberware_tooltip"
                            ));

                            if (Screen.hasShiftDown()) {
                                tooltipComponents.add(Component.translatable(
                                        "tooltip.roll_mod.lungsupgrade_sulfur_resistance.desc.1"
                                ).withStyle(ChatFormatting.GRAY));

                                tooltipComponents.add(Component.translatable(
                                        "tooltip.createcybernetics.humanity",
                                        getHumanityCost()
                                ).withStyle(ChatFormatting.GOLD));

                                tooltipComponents.add(Component.translatable(
                                        "tooltip.roll_mod.lungsupgrade_sulfur_resistance.desc.2"
                                ).withStyle(ChatFormatting.RED));


                            } else {
                                tooltipComponents.add(Component.translatable(
                                        "tooltip.createcybernetics.hold_shift_down"
                                ));
                            }

                            super.appendHoverText(
                                    stack,
                                    context,
                                    tooltipComponents,
                                    tooltipFlag
                            );
                        }
                    });

    public static final DeferredHolder<Item, CropModuleItem> CROP_ANALYZER_MODULE = ITEMS.register("crop_analyzer_module",
            () -> new CropModuleItem(new Item.Properties().stacksTo(1)) {
                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.createcybernetics.eyeupgrades_tooltip"));
                    if (Screen.hasShiftDown()) {
                        tooltipComponents.add(Component.translatable("tooltip.roll_mod.crop_analyzer.desc").withStyle(ChatFormatting.GRAY));
                        tooltipComponents.add(Component.translatable("tooltip.createcybernetics.humanity", getHumanityCost()).withStyle(ChatFormatting.GOLD));
                        tooltipComponents.add(Component.translatable("tooltip.roll_mod.crop_analyzer.energy", 5).withStyle(ChatFormatting.RED));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.createcybernetics.hold_shift_down"));
                    }
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            });

    public static final DeferredHolder<Item, CropLenseItem> CROP_ANALYZER_LENSE = ITEMS.register("crop_analyzer_lense",
            () -> new CropLenseItem(new Item.Properties().stacksTo(1)) {
                @Override
                public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                    tooltipComponents.add(Component.translatable("tooltip.createcybernetics.eyeupgrades_tooltip"));
                    if (Screen.hasShiftDown()) {
                        tooltipComponents.add(Component.translatable("tooltip.roll_mod.crop_analyzer.desc").withStyle(ChatFormatting.GRAY));
                        tooltipComponents.add(Component.translatable("tooltip.createcybernetics.humanity", getHumanityCost()).withStyle(ChatFormatting.GOLD));
                        tooltipComponents.add(Component.translatable("tooltip.roll_mod.crop_analyzer.energy", 10).withStyle(ChatFormatting.RED));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.createcybernetics.hold_shift_down"));
                    }
                    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
                }
            });

    public static final DeferredHolder<Item, Item> LETTER_OO = registerTooltip("letter_oo", TooltipOptions.nameAndGlow(0x0186FF));
    public static final DeferredHolder<Item, Item> LETTER_N = registerTooltip("letter_n", TooltipOptions.nameAndGlow(0x0186FF));
    public static final DeferredHolder<Item, Item> LETTER_B = registerTooltip("letter_b", TooltipOptions.nameAndGlow(0x0186FF));



    public static final DeferredHolder<Item, Item> BISMUTH_LITHIUM_BATTERY = registerSimple("bismuth_lithium_battery", new Item.Properties() );
    public static final DeferredHolder<Item, Item> LITHIUM_SULFATE_DUST = registerSimple("lithium_sulfate_dust", new Item.Properties() );


    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        CROPS.register(modBus);
    }

}
