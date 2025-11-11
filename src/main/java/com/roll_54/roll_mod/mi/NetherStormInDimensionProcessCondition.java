package com.roll_54.roll_mod.mi;

import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.machines.recipe.condition.MachineProcessCondition;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.netherstorm.StormHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

public record NetherStormInDimensionProcessCondition(ResourceKey<Level> dimension) implements MachineProcessCondition {
    public static final MapCodec<NetherStormInDimensionProcessCondition> CODEC =
            ResourceKey.codec(Registries.DIMENSION)
                    .fieldOf("dimension")
                    .xmap(NetherStormInDimensionProcessCondition::new, NetherStormInDimensionProcessCondition::dimension);

    public static final StreamCodec<RegistryFriendlyByteBuf, NetherStormInDimensionProcessCondition> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(Registries.DIMENSION),
                    NetherStormInDimensionProcessCondition::dimension,
                    NetherStormInDimensionProcessCondition::new
            );

    @Override
    public boolean canProcessRecipe(Context context, MachineRecipe recipe) {
        var lvl = context.getLevel();
        return StormHandler.isStormActive() && lvl != null && lvl.dimension() == dimension;
    }

    @Override
    public void appendDescription(List<Component> list) {
        var loc = dimension.location();
        list.add(Component.translatable("mi.condition.roll_mod.nether_storm.active_in_nether",
                Component.translatable("dimension.%s.%s".formatted(loc.getNamespace(), loc.getPath()))));
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