package com.roll_54.roll_mod.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class TelescopeRecipeSerializer implements RecipeSerializer<TelescopeRecipe> {

    @Override
    public MapCodec<TelescopeRecipe> codec() {
        return TelescopeRecipe.CODEC.fieldOf("recipe");
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, TelescopeRecipe> streamCodec() {
        return TelescopeRecipe.STREAM_CODEC;
    }
}
