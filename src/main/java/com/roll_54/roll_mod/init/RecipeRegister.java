package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.recipe.GrowthChamberRecipe;
import com.roll_54.roll_mod.recipe.ItemResearchRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RecipeRegister {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, RollMod.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, RollMod.MODID);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);
    }

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GrowthChamberRecipe>> GROWTH_CHAMBER_SERIALIZER =
            RECIPE_SERIALIZERS.register("growth_chamber", GrowthChamberRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GrowthChamberRecipe>> GROWTH_CHAMBER_TYPE =
            RECIPE_TYPES.register("growth_chamber", () -> new RecipeType<GrowthChamberRecipe>() {
                @Override
                public String toString() {
                    return "growth_chamber";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemResearchRecipe>> ITEM_RESEARCH_SERIALIZER =
            RECIPE_SERIALIZERS.register("item_research", ItemResearchRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemResearchRecipe>> ITEM_RESEARCH_TYPE =
            RECIPE_TYPES.register("item_research", () -> new RecipeType<ItemResearchRecipe>() {
                @Override
                public String toString() {
                    return "item_research";
                }
            });

}
