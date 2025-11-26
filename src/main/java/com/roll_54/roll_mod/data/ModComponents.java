package com.roll_54.roll_mod.data;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModComponents {
    public static final DeferredRegister.DataComponents COMPONENTS;
    public static final Supplier<DataComponentType<Boolean>> ACTIVATED;

    private static <D> DeferredHolder<DataComponentType<?>, DataComponentType<D>> create(String name, Codec<D> codec, StreamCodec<? super RegistryFriendlyByteBuf, D> streamCodec) {
        return COMPONENTS.registerComponentType(name, (b) -> b.persistent(codec).networkSynchronized(streamCodec));
    }

    static {
        COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "roll_mod");
        ACTIVATED = create("activated", Codec.BOOL, ByteBufCodecs.BOOL);

    }

    public static void init(IEventBus bus) {
        COMPONENTS.register(bus);
    }


}