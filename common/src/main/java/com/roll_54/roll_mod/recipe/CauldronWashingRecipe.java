package com.roll_54.roll_mod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.roll_54.roll_mod.registry.RecipeRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import com.roll_54.roll_mod.util.RollCauldronInteractions;

import java.util.List;
import java.util.Optional;

/**
 * Washing in a water cauldron: right-click it with a stack the ingredient matches and the whole stack turns into
 * {@code result} ({@code result.count} per item), for one level of water. For every item washed, one of
 * {@code byproducts} (picked evenly; stone and clay dust when the recipe names none) drops with
 * {@code byproduct_chance}.
 *
 * <pre>{@code
 * {
 *   "type": "roll_mod:cauldron_washing",
 *   "ingredient": { "item": "roll_mod:crushed_hematite_ore" },
 *   "result": { "id": "roll_mod:purified_hematite_ore" },
 *   "byproducts": [ "roll_mod:stone_dust", "roll_mod:clay_dust" ],
 *   "byproduct_chance": 0.5,
 *   "uses_water": true
 * }
 * }</pre>
 *
 * <p>Data-driven, so datapacks and KubeJS can add, change and remove them: in KubeJS
 * {@code event.recipes.roll_mod.cauldron_washing(result, ingredient)} (schema in
 * {@code data/roll_mod/kubejs/recipe_schema/cauldron_washing.json}), {@code event.custom({...})} for byproducts,
 * and {@code event.remove({type: 'roll_mod:cauldron_washing'})}. roll_mod's own come from datagen.
 */
public record CauldronWashingRecipe(Ingredient ingredient, ItemStack result, Optional<List<Item>> byproductList,
                                    float byproductChance, boolean usesWater) implements Recipe<SingleRecipeInput> {

    public static final float DEFAULT_BYPRODUCT_CHANCE = 0.5f;

    /** A recipe with its byproducts spelled out. */
    public CauldronWashingRecipe(Ingredient ingredient, ItemStack result, List<Item> byproducts, float byproductChance,
                                 boolean usesWater) {
        this(ingredient, result, Optional.of(List.copyOf(byproducts)), byproductChance, usesWater);
    }

    /** The byproducts: the recipe's own list, or stone and clay dust when it names none ({@code []} for none). */
    public List<Item> byproducts() {
        return byproductList.orElseGet(() -> List.of(RollCauldronInteractions.getDustItems()));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    /** The result for one item; the caller multiplies by the stack's count. */
    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return result;
    }

    @Override
    public boolean isSpecial() {
        // Not a crafting-book recipe.
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.CAULDRON_WASHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegistry.CAULDRON_WASHING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CauldronWashingRecipe> {
        public static final MapCodec<CauldronWashingRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CauldronWashingRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CauldronWashingRecipe::result),
                BuiltInRegistries.ITEM.byNameCodec().listOf().optionalFieldOf("byproducts")
                        .forGetter(CauldronWashingRecipe::byproductList),
                Codec.floatRange(0f, 1f).optionalFieldOf("byproduct_chance", DEFAULT_BYPRODUCT_CHANCE)
                        .forGetter(CauldronWashingRecipe::byproductChance),
                Codec.BOOL.optionalFieldOf("uses_water", true).forGetter(CauldronWashingRecipe::usesWater)
        ).apply(inst, CauldronWashingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CauldronWashingRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        Ingredient.CONTENTS_STREAM_CODEC, CauldronWashingRecipe::ingredient,
                        ItemStack.STREAM_CODEC, CauldronWashingRecipe::result,
                        ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list())),
                        CauldronWashingRecipe::byproductList,
                        ByteBufCodecs.FLOAT, CauldronWashingRecipe::byproductChance,
                        ByteBufCodecs.BOOL, CauldronWashingRecipe::usesWater,
                        CauldronWashingRecipe::new);

        @Override
        public MapCodec<CauldronWashingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CauldronWashingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
