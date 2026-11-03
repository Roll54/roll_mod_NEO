package com.roll_54.roll_mod.gtmogs.core.mixins.neoforge;

import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = BlockStateProvider.ConfiguredModelList.class, remap = false)
public interface ConfiguredModelListAccessor {

    @Accessor("models")
    List<ConfiguredModel> gtmogs$getModels();
}
