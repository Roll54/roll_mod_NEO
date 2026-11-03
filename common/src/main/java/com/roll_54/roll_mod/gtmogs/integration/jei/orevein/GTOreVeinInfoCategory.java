package com.roll_54.roll_mod.gtmogs.integration.jei.orevein;

import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.integration.xei.widgets.GTOreVeinWidget;
import lombok.Getter;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class GTOreVeinInfoCategory extends ModularUIRecipeCategory<Holder<OreVeinDefinition>> {

    public final static RecipeType<Holder<OreVeinDefinition>> RECIPE_TYPE = new RecipeType(
            GTMOGS.id("ore_vein_diagram"),
            Holder.class);
    @Getter
    private final IDrawable background;
    @Getter
    private final IDrawable icon;

    public GTOreVeinInfoCategory(IJeiHelpers helpers) {
        super(GTOreVeinInfoWrapper::new);
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(GTOreVeinWidget.width, 120);
        this.icon = helpers.getGuiHelper()
                .createDrawableItemStack(
                        new ItemStack(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("minecraft:raw_iron"))));
    }

    public static void registerRecipes(IRecipeRegistration registry) {
        var ores = Minecraft.getInstance().level.registryAccess()
                .registryOrThrow(GTRegistries.ORE_VEIN_REGISTRY);
        registry.addRecipes(RECIPE_TYPE, ores.holders()
                .filter(ore -> ore.value().canGenerate())
                .<Holder<OreVeinDefinition>>map(Function.identity())
                .toList());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Holder<OreVeinDefinition> definition, IFocusGroup focuses) {
        super.setRecipe(builder, definition, focuses);
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                .addItemStacks(GTOreVeinWidget.getContainedOresAndBlocks(definition.value()));
    }

    @NotNull
    @Override
    public RecipeType<Holder<OreVeinDefinition>> getRecipeType() {
        return RECIPE_TYPE;
    }

    @NotNull
    @Override
    public Component getTitle() {
        return Component.translatable("gtmogs.jei.ore_vein_diagram");
    }
}
