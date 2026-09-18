package com.roll_54.roll_mod.items.armor;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, RollMod.MODID);

    // ===== HAZMAT =====
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> HAZMAT_ARMOR =
            ARMOR_MATERIALS.register("hazmat_armor", () -> createMaterial(
                    "hazmat_armor",
                    // defense per slot (helmet, chest, legs, boots)
                    mapDefense(3, 8, 6, 4),
                    10,                              // enchantability
                    SoundEvents.ARMOR_EQUIP_LEATHER, // Holder<SoundEvent>
                    () -> Ingredient.EMPTY,          // no repair
                    0.0f,                            // toughness
                    0.0f                             // knockback resistance
            ));

    // ===== METEORITE =====
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> METEORITE_ARMOR =
            ARMOR_MATERIALS.register("meteorite", () -> createMaterial(
                    "meteorite",
                    mapDefense(3, 8, 6, 3),
                    18,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ItemRegistry.METEORITE_METAL_INGOT.get()),
                    2.0f,
                    0.1f

            ));


    // ===== TOOL-TIER COUNTERPARTS =====
    // Each of the sets below mirrors the matching tier in {@link com.roll_54.roll_mod.items.ModToolTiers},
    // so a material that makes better tools also makes better armour.

    /** Weakest bronze: iron-tier tools, so slightly-under-iron plating with the tier's high enchantability. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BISMUTH_BRONZE_ARMOR =
            ARMOR_MATERIALS.register("bismuth_bronze", () -> createMaterial(
                    "bismuth_bronze",
                    mapDefense(2, 5, 4, 2),
                    16,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ingotTag("bismuth_bronze")),
                    0.0f,
                    0.0f
            ));

    /** The better bronze: iron-tier tools with a longer-lived head, so full iron plating plus a little toughness. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BLACK_BRONZE_ARMOR =
            ARMOR_MATERIALS.register("black_bronze", () -> createMaterial(
                    "black_bronze",
                    mapDefense(2, 6, 5, 2),
                    18,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ingotTag("black_bronze")),
                    0.5f,
                    0.0f
            ));

    /** Diamond-tier tools built for endurance rather than bite: durable, plainly protective, low enchantability. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> STEEL_ARMOR =
            ARMOR_MATERIALS.register("steel", () -> createMaterial(
                    "steel",
                    mapDefense(3, 6, 5, 2),
                    10,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ingotTag("steel")),
                    1.0f,
                    0.0f
            ));

    /** Diamond's mining level on a shorter-lived head: better plating than steel, less of it before it wears out. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BLACK_STEEL_ARMOR =
            ARMOR_MATERIALS.register("black_steel", () -> createMaterial(
                    "black_steel",
                    mapDefense(3, 7, 6, 3),
                    10,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ingotTag("black_steel")),
                    1.5f,
                    0.0f
            ));

    /** Call in your mod constructor: ModArmorMaterials.register(modBus); */
    public static void register(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
    }

    /**
     * The bronze and steel alloys come from Modern Industrialization materials registered by KubeJS at runtime,
     * so their repair ingredient has to go through the material's {@code c:ingots/...} tag rather than an item
     * reference that would not resolve at class-load time. Mirrors {@code ModToolTiers.ingotTag}.
     */
    private static TagKey<Item> ingotTag(String material) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/" + material));
    }

    /** Helper: build defense map in order (helmet, chest, legs, boots). */
    private static Map<ArmorItem.Type, Integer> mapDefense(int helm, int chest, int legs, int boots) {
        var map = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.HELMET, helm);
        map.put(ArmorItem.Type.CHESTPLATE, chest);
        map.put(ArmorItem.Type.LEGGINGS, legs);
        map.put(ArmorItem.Type.BOOTS, boots);
        return map;
    }

    /**
     * 1.21.1 ArmorMaterial(record) constructor:
     *    new ArmorMaterial(Map<Type,Integer> defenseByType,
     *                      int enchantmentValue,
     *                      Holder<SoundEvent> equipSound,
     *                      Supplier<Ingredient> repairIngredient,
     *                      List<ArmorMaterial.Layer> layers,
     *                      float toughness,
     *                      float knockbackResistance)
     * NOTE: Per-slot durability bases are now vanilla constants; you can't pass custom per-slot multipliers via API.
     */
    private static ArmorMaterial createMaterial(String name,
                                                Map<ArmorItem.Type, Integer> defenseByType,
                                                int enchantability,
                                                Holder<SoundEvent> equipSound,
                                                Supplier<Ingredient> repairSupplier,
                                                float toughness,
                                                float knockbackRes) {
        // One layer; texture paths expected at:
        // assets/roll_mod/textures/models/armor/<name>_layer_1.png
        // assets/roll_mod/textures/models/armor/<name>_layer_2.png
        var textureId = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, name);
        var layer = new ArmorMaterial.Layer(textureId);

        return new ArmorMaterial(
                defenseByType,
                enchantability,
                equipSound,
                repairSupplier,
                List.of(layer),
                toughness,
                knockbackRes
        );
    }
}
