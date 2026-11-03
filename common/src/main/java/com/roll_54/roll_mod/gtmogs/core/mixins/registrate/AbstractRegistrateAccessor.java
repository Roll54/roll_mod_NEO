package com.roll_54.roll_mod.gtmogs.core.mixins.registrate;

import net.neoforged.bus.api.IEventBus;

import com.google.common.collect.ListMultimap;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateProvider;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractRegistrate.class, remap = false)
public interface AbstractRegistrateAccessor {

    @Accessor
    ListMultimap<ProviderType<?>, @NotNull NonNullConsumer<? extends RegistrateProvider>> getDatagens();

    @Accessor
    NonNullSupplier<Boolean> getDoDatagen();

    @Accessor("modEventBus")
    IEventBus getModEventBus();

    @Accessor("modEventBus")
    void setModEventBus(IEventBus modEventBus);
}
