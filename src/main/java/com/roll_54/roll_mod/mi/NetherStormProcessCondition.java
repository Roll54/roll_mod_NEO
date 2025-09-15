package com.roll_54.roll_mod.mi;

import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.machines.recipe.condition.MachineProcessCondition;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.Roll_mod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.roll_54.roll_mod.netherstorm.StormHandler;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * true, коли активний Nether Storm (незалежно від виміру).
 */
public record NetherStormProcessCondition(ResourceKey<Level> levelKey) implements MachineProcessCondition {

    // JSON: { "type": "roll_mod:nether_storm_active_in_level", "level": "minecraft:the_nether" }
    public static final MapCodec<NetherStormProcessCondition> CODEC =
            ResourceKey.codec(Registries.DIMENSION)
                    .fieldOf("level")
                    .xmap(NetherStormProcessCondition::new, NetherStormProcessCondition::levelKey);

    public static final StreamCodec<RegistryFriendlyByteBuf, NetherStormProcessCondition> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(Registries.DIMENSION),
                    NetherStormProcessCondition::levelKey,
                    NetherStormProcessCondition::new
            );

    @Override
    public boolean canProcessRecipe(Context context, MachineRecipe recipe) {
        var lvl = context.getLevel();
        boolean ok = StormHandler.isStormActive() && lvl != null && lvl.dimension() == levelKey;
        // легкий дебаг раз у ~10 секунд (200 тік), без доступу до recipe.type()
        if (lvl != null && lvl.getGameTime() % 200 == 0) {
            Roll_mod.LOGGER.info(
                    "[MI] condition nether_storm_active_in_level -> storm={}, level={}, need={}, result={}",
                    StormHandler.isStormActive(),
                    lvl.dimension().location(),
                    levelKey.location(),
                    ok
            );
        }
        return ok;
    }

    @Override
    public void appendDescription(List<Component> list) {
        var loc = levelKey.location();
        list.add(Component.translatable(
                "mi.condition.roll_mod.nether_storm.active_in_level",
                Component.translatable("level.%s.%s".formatted(loc.getNamespace(), loc.getPath()))
        ));
    }

    @Override
    public ItemStack icon() {
        return new ItemStack(Items.WITHER_SKELETON_SKULL);
    }

    @Override
    public MapCodec<? extends MachineProcessCondition> codec() { return CODEC; }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, ? extends MachineProcessCondition> streamCodec() { return STREAM_CODEC; }
}