package com.roll_54.roll_mod.gtmogs.integration.emi;

import com.roll_54.roll_mod.gtmogs.integration.emi.orevein.GTOreVeinEmiCategory;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class GTEMIPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(GTOreVeinEmiCategory.CATEGORY);
        GTOreVeinEmiCategory.registerDisplays(registry);
    }
}
