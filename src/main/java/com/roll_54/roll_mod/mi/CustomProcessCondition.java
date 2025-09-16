package com.roll_54.roll_mod.mi;

import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.machines.recipe.condition.MachineProcessCondition;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.Netherstorm.StormHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * A simple process condition that only succeeds while the custom storm is active.
 */
public record CustomProcessCondition() implements MachineProcessCondition {

    public static final CustomProcessCondition INSTANCE = new CustomProcessCondition();

    public static final MapCodec<CustomProcessCondition> CODEC = MapCodec.unit(INSTANCE);

    public static final StreamCodec<RegistryFriendlyByteBuf, CustomProcessCondition> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public boolean canProcessRecipe(Context context, MachineRecipe recipe) {
        return StormHandler.isStormActive();
    }

    @Override
    public void appendDescription(List<Component> list) {
        list.add(Component.translatable("mi.condition.roll_mod.nether_storm.active"));
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.WITHER_SKELETON_SKULL);
    }

    @Override
    public MapCodec<? extends MachineProcessCondition> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ? extends MachineProcessCondition> streamCodec() {
        return STREAM_CODEC;
    }
}

