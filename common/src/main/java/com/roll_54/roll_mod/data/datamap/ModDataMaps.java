package com.roll_54.roll_mod.data.datamap;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The mod's own data maps, so reagents for the Hydroponic Garden Bed can be declared in datapacks
 * instead of in code. Files live at {@code data/<namespace>/data_maps/{item,fluid}/{acidity,fertilizer}.json};
 * see {@code docs/hydroponic-data-maps.md} for the format.
 *
 * <p>{@code roll_mod:npk} ({@link NutrientValue}) feeds crops' N/P/K through the MV/HV Crop Managers;
 * see {@code docs/crops-wiki.md} §11.7.
 *
 * <p>All of them are synced to clients: the bed's input slots have to decide whether they accept a
 * stack, and {@code Slot#mayPlace} runs on the client too.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ModDataMaps {

    /** Acidity units contributed by one item. Positive = acid, negative = base. */
    public static final DataMapType<Item, AcidityValue> ACIDITY_ITEM =
            DataMapType.builder(RollMod.id("acidity"), Registries.ITEM, AcidityValue.CODEC)
                    .synced(AcidityValue.CODEC, false)
                    .build();

    /** Acidity units contributed by one bucket (1000 mB) of this fluid. Positive = acid, negative = base. */
    public static final DataMapType<Fluid, AcidityValue> ACIDITY_FLUID =
            DataMapType.builder(RollMod.id("acidity"), Registries.FLUID, AcidityValue.CODEC)
                    .synced(AcidityValue.CODEC, false)
                    .build();

    /** Fertilizer millibuckets contributed by one item. */
    public static final DataMapType<Item, FertilizerValue> FERTILIZER_ITEM =
            DataMapType.builder(RollMod.id("fertilizer"), Registries.ITEM, FertilizerValue.CODEC)
                    .synced(FertilizerValue.CODEC, false)
                    .build();

    /** Fertilizer millibuckets contributed by one bucket (1000 mB) of this fluid. */
    public static final DataMapType<Fluid, FertilizerValue> FERTILIZER_FLUID =
            DataMapType.builder(RollMod.id("fertilizer"), Registries.FLUID, FertilizerValue.CODEC)
                    .synced(FertilizerValue.CODEC, false)
                    .build();

    /** N/P/K contributed by one item. */
    public static final DataMapType<Item, NutrientValue> NPK_ITEM =
            DataMapType.builder(RollMod.id("npk"), Registries.ITEM, NutrientValue.CODEC)
                    .synced(NutrientValue.CODEC, false)
                    .build();

    /** N/P/K contributed by one bucket (1000 mB) of this fluid. */
    public static final DataMapType<Fluid, NutrientValue> NPK_FLUID =
            DataMapType.builder(RollMod.id("npk"), Registries.FLUID, NutrientValue.CODEC)
                    .synced(NutrientValue.CODEC, false)
                    .build();

    private ModDataMaps() {
    }

    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(ACIDITY_ITEM);
        event.register(ACIDITY_FLUID);
        event.register(FERTILIZER_ITEM);
        event.register(FERTILIZER_FLUID);
        event.register(NPK_ITEM);
        event.register(NPK_FLUID);
    }

    @Nullable
    public static AcidityValue acidity(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getItem().builtInRegistryHolder().getData(ACIDITY_ITEM);
    }

    @Nullable
    public static AcidityValue acidity(Fluid fluid) {
        return fluid.builtInRegistryHolder().getData(ACIDITY_FLUID);
    }

    @Nullable
    public static FertilizerValue fertilizer(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getItem().builtInRegistryHolder().getData(FERTILIZER_ITEM);
    }

    @Nullable
    public static FertilizerValue fertilizer(Fluid fluid) {
        return fluid.builtInRegistryHolder().getData(FERTILIZER_FLUID);
    }

    @Nullable
    public static NutrientValue nutrients(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getItem().builtInRegistryHolder().getData(NPK_ITEM);
    }

    @Nullable
    public static NutrientValue nutrients(Fluid fluid) {
        return fluid.builtInRegistryHolder().getData(NPK_FLUID);
    }
}
