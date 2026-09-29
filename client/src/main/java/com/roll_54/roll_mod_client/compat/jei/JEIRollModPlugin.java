package com.roll_54.roll_mod_client.compat.jei;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.gui.menu.ResearchWorkbenchMenu;
import com.roll_54.roll_mod.minestar.hub.gui.HubReturn;
import com.roll_54.roll_mod.recipe.ItemResearchRecipe;
import com.roll_54.roll_mod.registry.BlockRegistry;
import com.roll_54.roll_mod.registry.MenuTypes;
import com.roll_54.roll_mod.registry.RecipeRegistry;
import com.roll_54.roll_mod_client.gui.screen.ResearchWorkbenchScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
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
                new ResearchTableRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CauldronWashingCategory(registration.getJeiHelpers().getGuiHelper())
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

        // generate cauldron washing recipes from OreDefinitions
        List<CauldronWashingRecipe> cauldronRecipes = new ArrayList<>();
        for (var ore : OreDefinitions.ORE_DEFINITIONS) {
            String id = ore.id();
            // impure_<id>_dust -> <id>_dust
            ResourceLocation inp = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "impure_" + id + "_dust");
            ResourceLocation out = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, id + "_dust");
            var inOpt = BuiltInRegistries.ITEM.getOptional(inp);
            var outOpt = BuiltInRegistries.ITEM.getOptional(out);
            if (inOpt.isPresent() && outOpt.isPresent()) {
                cauldronRecipes.add(new CauldronWashingRecipe(new ItemStack(inOpt.get()), new ItemStack(outOpt.get()), 0.2f));
            }

            // crushed_<id>_ore -> purified_<id>_ore
            ResourceLocation crushed = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "crushed_" + id + "_ore");
            ResourceLocation purified = ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "purified_" + id + "_ore");
            var crushedOpt = BuiltInRegistries.ITEM.getOptional(crushed);
            var purifiedOpt = BuiltInRegistries.ITEM.getOptional(purified);
            if (crushedOpt.isPresent() && purifiedOpt.isPresent()) {
                cauldronRecipes.add(new CauldronWashingRecipe(new ItemStack(crushedOpt.get()), new ItemStack(purifiedOpt.get()), 0.2f));
            }
        }

        registration.addRecipes(CauldronWashingCategory.TYPE, cauldronRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(BlockRegistry.RESEARCH_WORKBENCH),
                ResearchTableRecipeCategory.ITEM_RESEARCH_TYPE
        );

        // cauldron catalyst
        registration.addRecipeCatalyst(new ItemStack(net.minecraft.world.level.block.Blocks.CAULDRON), CauldronWashingCategory.TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(
                ResearchWorkbenchScreen.class,
                46, 19,
                68, 49,
                ResearchTableRecipeCategory.ITEM_RESEARCH_TYPE
        );
        registerHubBounds(registration, HubReturn.screenClass());
    }

    /**
     * Tells JEI where the hub window really is. The hub's root covers the whole screen now, with a
     * movable, resizable window floating on it, so JEI's default — the container's image rectangle —
     * would be the entire screen and leave its item list no room at all. Every other LdLib container
     * screen gets exactly what JEI would have worked out for it anyway.
     */
    private static <T extends AbstractContainerScreen<?>> void registerHubBounds(
            IGuiHandlerRegistration registration, Class<T> screenClass) {
        registration.addGuiScreenHandler(screenClass, screen -> {
            float[] rect = HubReturn.windowRect(screen);
            if (rect == null) {
                return properties(screenClass, screen.getGuiLeft(), screen.getGuiTop(),
                        screen.getXSize(), screen.getYSize(), screen.width, screen.height);
            }
            return properties(screenClass, Math.round(rect[0]), Math.round(rect[1]),
                    Math.round(rect[2]), Math.round(rect[3]), screen.width, screen.height);
        });
    }

    private static IGuiProperties properties(Class<? extends Screen> screenClass, int left, int top,
                                             int width, int height, int screenWidth, int screenHeight) {
        return new IGuiProperties() {
            @Override public Class<? extends Screen> screenClass() { return screenClass; }
            @Override public int guiLeft() { return left; }
            @Override public int guiTop() { return top; }
            @Override public int guiXSize() { return width; }
            @Override public int guiYSize() { return height; }
            @Override public int screenWidth() { return screenWidth; }
            @Override public int screenHeight() { return screenHeight; }
        };
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
