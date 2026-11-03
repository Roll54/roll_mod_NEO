package com.roll_54.roll_mod.gtmogs.integration.rei.orevein;

import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.rei.IGui2Renderer;
import com.lowdragmc.lowdraglib.rei.ModularUIDisplayCategory;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.integration.xei.widgets.GTOreVeinWidget;
import lombok.Getter;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import org.jetbrains.annotations.NotNull;

@Getter
public class GTOreVeinDisplayCategory extends ModularUIDisplayCategory<GTOreVeinDisplay> {

    public static final CategoryIdentifier<GTOreVeinDisplay> CATEGORY = CategoryIdentifier
            .of(GTMOGS.id("ore_vein_diagram"));

    private final Renderer icon;

    private final Size size;

    public GTOreVeinDisplayCategory() {
        this.icon = IGui2Renderer.toDrawable(new ItemStackTexture(Items.IRON_INGOT.asItem()));
        this.size = new Size(10 + GTOreVeinWidget.width, 140);
    }

    @Override
    public CategoryIdentifier<? extends GTOreVeinDisplay> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public int getDisplayHeight() {
        return getSize().height;
    }

    @Override
    public int getDisplayWidth(GTOreVeinDisplay display) {
        return getSize().width;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.translatable("gtmogs.jei.ore_vein_diagram");
    }

    public static void registerDisplays(DisplayRegistry registry) {
        var fluids = Minecraft.getInstance().level.registryAccess()
                .registryOrThrow(GTRegistries.ORE_VEIN_REGISTRY);
        fluids.holders()
                .filter(ore -> ore.value().canGenerate())
                .forEach(ore -> registry.add(new GTOreVeinDisplay(ore)));
    }
}
