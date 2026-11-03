package com.roll_54.roll_mod.gtmogs.core.mixins.neoforge;

import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.function.Function;

@Mixin(value = ConfiguredModel.Builder.class, remap = false)
public interface ConfiguredModelBuilderAccessor {

    @Invoker("<init>")
    static <T> ConfiguredModel.Builder<T> builder(@Nullable Function<ConfiguredModel[], T> callback,
                                                  List<ConfiguredModel> otherModels) {
        throw new AssertionError();
    }
}
