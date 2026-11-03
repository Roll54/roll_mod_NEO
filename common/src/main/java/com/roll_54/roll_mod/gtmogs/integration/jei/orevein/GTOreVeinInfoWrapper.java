package com.roll_54.roll_mod.gtmogs.integration.jei.orevein;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;

import net.minecraft.core.Holder;

import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.integration.xei.widgets.GTOreVeinWidget;

public class GTOreVeinInfoWrapper extends ModularWrapper<GTOreVeinWidget> {

    public final Holder<OreVeinDefinition> oreDefinition;

    public GTOreVeinInfoWrapper(Holder<OreVeinDefinition> oreDefinition) {
        super(new GTOreVeinWidget(oreDefinition));
        this.oreDefinition = oreDefinition;
    }
}
