package com.roll_54.roll_mod.compat.jei;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.gui.menu.ResearchWorkbenchMenu;
import com.roll_54.roll_mod.recipe.ItemResearchRecipe;
import com.roll_54.roll_mod.registry.BlockRegistry;
import com.roll_54.roll_mod.registry.MenuTypes;
import com.roll_54.roll_mod.registry.RecipeRegistry;
import com.roll_54.roll_mod.gui.screen.ResearchWorkbenchScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class JEIRollModPlugin implements IModPlugin {
    /**
     * The unique ID for this mod plugin.
     * The namespace should be your mod's modId.
     */
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "roll_jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ResearchTableRecipeCategory(registration.getJeiHelpers().getGuiHelper())//,
                //new GraftingTableRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;

        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<RecipeHolder<ItemResearchRecipe>> engineeringTableRecipes =
                recipeManager.getAllRecipesFor(RecipeRegistry.ITEM_RESEARCH_RECIPE_TYPE.get());

        registration.addRecipes(
                ResearchTableRecipeCategory.ITEM_RESEARCH_TYPE,
                engineeringTableRecipes
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(BlockRegistry.RESEARCH_WORKBENCH),
                ResearchTableRecipeCategory.ITEM_RESEARCH_TYPE
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(
                ResearchWorkbenchScreen.class,
                46, 19,
                68, 49,
                ResearchTableRecipeCategory.ITEM_RESEARCH_TYPE
        );
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                ResearchWorkbenchMenu.class,
                MenuTypes.RESEARCH_WORKBENCH_MENU.get(),
                ResearchTableRecipeCategory.ITEM_RESEARCH_TYPE,
                1, 25,
                26, 36);
        }
}
