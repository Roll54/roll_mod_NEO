package com.roll_54.roll_mod.ModArmor;

import com.roll_54.roll_mod.Roll_mod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
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
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Roll_mod.MODID);

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
                    // TODO: replace with your actual ingot holder: () -> Ingredient.of(ModItems.METEORITE_METAL_INGOT.get())
                    () -> Ingredient.of(Items.IRON_INGOT),
                    2.0f,
                    0.1f
            ));

    /** Call in your mod constructor: ModArmorMaterials.register(modBus); */
    public static void register(IEventBus modBus) {
        ARMOR_MATERIALS.register(modBus);
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
     *
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
        var textureId = ResourceLocation.fromNamespaceAndPath(Roll_mod.MODID, name);
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
