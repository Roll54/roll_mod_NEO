package com.roll_54.roll_mod.compat.mi;

import aztech.modern_industrialization.machines.recipe.MachineRecipeType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.swedz.tesseract.neoforge.compat.mi.hook.MIHookEntrypoint;
import net.swedz.tesseract.neoforge.compat.mi.hook.MIHookRegistry;
import net.swedz.tesseract.neoforge.registry.SortOrder;
import net.swedz.tesseract.neoforge.registry.holder.BlockHolder;
import net.swedz.tesseract.neoforge.registry.holder.ItemHolder;

@MIHookEntrypoint
public final class RMMIHookRegistry implements MIHookRegistry {
    @Override
    public DeferredRegister.Blocks blockRegistry() {
        return null;
    }

    @Override
    public DeferredRegister<BlockEntityType<?>> blockEntityRegistry() {
        return null;
    }

    @Override
    public DeferredRegister.Items itemRegistry() {
        return null;
    }

    @Override
    public DeferredRegister<RecipeSerializer<?>> recipeSerializerRegistry() {
        return null;
    }

    @Override
    public DeferredRegister<RecipeType<?>> recipeTypeRegistry() {
        return null;
    }

    @Override
    public void onBlockRegister(BlockHolder blockHolder) {

    }

    @Override
    public void onBlockEntityRegister(BlockEntityType<?> blockEntityType) {

    }

    @Override
    public void onItemRegister(ItemHolder itemHolder) {

    }

    @Override
    public void onMachineRecipeTypeRegister(MachineRecipeType machineRecipeType) {

    }

    @Override
    public SortOrder sortOrderMachines() {
        return null;
    }
}
