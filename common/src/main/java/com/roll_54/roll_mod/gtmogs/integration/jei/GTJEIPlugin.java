package com.roll_54.roll_mod.gtmogs.integration.jei;

import net.minecraft.resources.ResourceLocation;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.integration.jei.orevein.GTOreVeinInfoCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.registration.*;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class GTJEIPlugin implements IModPlugin {

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return GTMOGS.id("jei_plugin");
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registry) {
        if (!GTMOGS.Mods.isJEILoaded()) return;

        IJeiHelpers jeiHelpers = registry.getJeiHelpers();
        registry.addRecipeCategories(new GTOreVeinInfoCategory(jeiHelpers));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        if (!GTMOGS.Mods.isJEILoaded()) return;
        GTOreVeinInfoCategory.registerRecipes(registration);
    }
}
