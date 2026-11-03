package com.roll_54.roll_mod.gtmogs.integration.rei;

import com.roll_54.roll_mod.gtmogs.integration.rei.orevein.GTOreVeinDisplayCategory;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.forge.REIPluginClient;

@REIPluginClient
public class GTREIPlugin implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        // Categories
        registry.add(new GTOreVeinDisplayCategory());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        GTOreVeinDisplayCategory.registerDisplays(registry);
    }
}
