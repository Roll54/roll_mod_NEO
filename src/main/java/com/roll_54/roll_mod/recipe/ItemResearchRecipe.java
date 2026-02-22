package com.roll_54.roll_mod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.roll_54.roll_mod.init.RecipeRegister;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Research recipe:
 *  - output: the blueprint item to produce on success
 *  - mainInput: the "expensive" item consumed ONLY on success  (with optional flag)
 *  - catalysts: list of {item, successChance}, one of which the player inserts into the catalyst slot
 *  - duration: how many ticks one research attempt takes
 */
public record ItemResearchRecipe(
        ItemStack output,
        MainInput mainInput,
        List<CatalystEntry> catalysts,
        int duration
) implements Recipe<ItemResearchRecipeInput> {

    // ── inner records ──────────────────────────────────────────────

    public record MainInput(Item item, boolean consumeOnSuccess) {
        public static final Codec<MainInput> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(MainInput::item),
                Codec.BOOL.optionalFieldOf("consume_on_success", true).forGetter(MainInput::consumeOnSuccess)
        ).apply(inst, MainInput::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, MainInput> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM), MainInput::item,
                        ByteBufCodecs.BOOL, MainInput::consumeOnSuccess,
                        MainInput::new);
    }

    public record CatalystEntry(Item item, float successChance) {
        public static final Codec<CatalystEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(CatalystEntry::item),
                Codec.FLOAT.fieldOf("success_chance").forGetter(CatalystEntry::successChance)
        ).apply(inst, CatalystEntry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CatalystEntry> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM), CatalystEntry::item,
                        ByteBufCodecs.FLOAT, CatalystEntry::successChance,
                        CatalystEntry::new);
    }

    // ── Recipe implementation ──────────────────────────────────────

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create(); // not used by standard crafting UI
    }

    /** Returns the CatalystEntry that matches the given stack, or null if none. */
    public CatalystEntry findCatalyst(ItemStack stack) {
        for (CatalystEntry entry : catalysts) {
            if (stack.is(entry.item())) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public boolean matches(ItemResearchRecipeInput input, Level level) {
        if (level.isClientSide()) return false;
        return findCatalyst(input.catalyst()) != null;
    }

    @Override
    public ItemStack assemble(ItemResearchRecipeInput input, HolderLookup.Provider provider) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegister.ITEM_RESEARCH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeRegister.ITEM_RESEARCH_TYPE.get();
    }

    // ── Serializer ─────────────────────────────────────────────────

    public static class Serializer implements RecipeSerializer<ItemResearchRecipe> {

        public static final MapCodec<ItemResearchRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ItemStack.CODEC.fieldOf("output").forGetter(ItemResearchRecipe::output),
                MainInput.CODEC.fieldOf("main_input").forGetter(ItemResearchRecipe::mainInput),
                CatalystEntry.CODEC.listOf().fieldOf("catalysts").forGetter(ItemResearchRecipe::catalysts),
                Codec.INT.optionalFieldOf("duration", 100).forGetter(ItemResearchRecipe::duration)
        ).apply(inst, ItemResearchRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ItemResearchRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ItemStack.STREAM_CODEC, ItemResearchRecipe::output,
                        MainInput.STREAM_CODEC, ItemResearchRecipe::mainInput,
                        CatalystEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), ItemResearchRecipe::catalysts,
                        ByteBufCodecs.INT, ItemResearchRecipe::duration,
                        ItemResearchRecipe::new);

        @Override
        public MapCodec<ItemResearchRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ItemResearchRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
