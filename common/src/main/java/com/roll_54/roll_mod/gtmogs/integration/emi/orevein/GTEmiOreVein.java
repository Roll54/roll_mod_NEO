package com.roll_54.roll_mod.gtmogs.integration.emi.orevein;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;


import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.integration.xei.widgets.GTOreVeinWidget;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GTEmiOreVein extends ModularEmiRecipe<WidgetGroup> {

    private final Holder<OreVeinDefinition> oreDefinition;

    public GTEmiOreVein(Holder<OreVeinDefinition> oreDefinition) {
        super(() -> new GTOreVeinWidget(oreDefinition));
        this.oreDefinition = oreDefinition;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return GTOreVeinEmiCategory.CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return oreDefinition.getKey().location().withPrefix("/ore_vein_diagram/");
    }

    @Override
    public List<EmiStack> getOutputs() {
        // roll_mod: everything the page's slots cycle through — ores in every rock, raw ores, samples — so
        // looking up raw_hematite, granite_hematite or a hematite sample finds its veins (see OreVeinLookups).
        List<EmiStack> outputs = new ArrayList<>();
        GTOreVeinWidget.getContainedOresAndBlocks(oreDefinition.value())
                .forEach(item -> outputs.add(EmiStack.of(item)));
        return outputs;
    }
}
