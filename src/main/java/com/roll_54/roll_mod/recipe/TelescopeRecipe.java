package com.roll_54.roll_mod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.roll_54.roll_mod.registry.RecipeRegistry;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Nonnull;
import java.util.List;

@ParametersAreNonnullByDefault
public class TelescopeRecipe implements Recipe<RecipeInput> {
    private final ItemStack input;
    private final ItemStack output;
    private final List<TelescopeButton> buttons;

    public TelescopeRecipe(ItemStack input, ItemStack output, List<TelescopeButton> buttons) {
        this.input = input;
        this.output = output;
        this.buttons = buttons;
    }

    @Override
    public boolean matches(RecipeInput pInput, Level pLevel) {
        return false;
    }

    @Override
    @Nonnull
    public ItemStack assemble(RecipeInput pInput, net.minecraft.core.HolderLookup.Provider pRegistries) {
        return this.output.copy();
    }

    @Override
    @Nonnull
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider pRegistries) {
        return this.output.copy();
    }

    @Override
    @Nonnull
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.TELESCOPE_RECIPE_SERIALIZER.get();
    }

    @Override
    @Nonnull
    public RecipeType<?> getType() {
        return RecipeRegistry.TELESCOPE_RECIPE_TYPE.get();
    }

    @Override
    @Nonnull
    public String getGroup() {
        return "telescope";
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public List<TelescopeButton> getButtons() {
        return buttons;
    }

    public static class TelescopeButton {
        private final ResourceLocation texture;
        private final float successChance;

        public TelescopeButton(ResourceLocation texture, float successChance) {
            this.texture = texture;
            this.successChance = successChance;
        }

        public static final Codec<TelescopeButton> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(TelescopeButton::getTexture),
                Codec.FLOAT.fieldOf("chance").forGetter(TelescopeButton::getSuccessChance)
            ).apply(instance, TelescopeButton::new)
        );

        public ResourceLocation getTexture() {
            return texture;
        }

        public float getSuccessChance() {
            return successChance;
        }
    }

    public static final Codec<TelescopeRecipe> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ItemStack.CODEC.fieldOf("input").forGetter(TelescopeRecipe::getInput),
            ItemStack.CODEC.fieldOf("output").forGetter(TelescopeRecipe::getOutput),
            TelescopeButton.CODEC.listOf().fieldOf("buttons").forGetter(TelescopeRecipe::getButtons)
        ).apply(instance, TelescopeRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TelescopeRecipe> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC,
        TelescopeRecipe::getInput,
        ItemStack.STREAM_CODEC,
        TelescopeRecipe::getOutput,
        StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            TelescopeButton::getTexture,
            net.minecraft.network.codec.ByteBufCodecs.FLOAT,
            TelescopeButton::getSuccessChance,
            TelescopeButton::new
        ).apply(net.minecraft.network.codec.ByteBufCodecs.list()),
        TelescopeRecipe::getButtons,
        TelescopeRecipe::new
    );
}
