package com.roll_54.roll_mod.compat.jei;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.recipe.ItemResearchRecipe;
import com.roll_54.roll_mod.registry.BlockRegistry;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResearchTableRecipeCategory implements IRecipeCategory<RecipeHolder<ItemResearchRecipe>> {


    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item_research");

    public static final RecipeType<RecipeHolder<ItemResearchRecipe>> ITEM_RESEARCH_TYPE =
            RecipeType.create(RollMod.MODID, "item_research", (Class) RecipeHolder.class);

    private final Component title = Component.translatable("gui.roll_mod.jei.research_workbench.title");
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;


    public ResearchTableRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(
                ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/gui/research_workbench/research_workbench_gui_pointed.png"),
                7, 14, 157, 50
        );
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(BlockRegistry.RESEARCH_WORKBENCH.get()));
        this.arrow = helper.drawableBuilder(
                        ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/gui/research_workbench/research_workbench_gui_pointed.png"), 0, 166, 68, 48
                )
                .buildAnimated(200, IDrawableAnimated.StartDirection.LEFT, false);
    }


    @Override
    public RecipeType<RecipeHolder<ItemResearchRecipe>> getRecipeType() {
        return ITEM_RESEARCH_TYPE;
    }

    @Override
    public Component getTitle() {
        return this.title;
                //Component.translatable("gui.roll_mod.jei.research_workbench.title");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }


    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          RecipeHolder<ItemResearchRecipe> recipeHolder,
                          IFocusGroup focuses) {

        ItemResearchRecipe recipe = recipeHolder.value();

        // --- Main input ---
        builder.addSlot(RecipeIngredientRole.INPUT, 2, 20)
                .addItemStack(recipe.mainInput().item().getDefaultInstance());

        // --- Catalysts (no cycling here) ---
        builder.addSlot(RecipeIngredientRole.INPUT, 20  , 20)
                .addIngredients(VanillaTypes.ITEM_STACK,
                        recipe.catalysts().stream()
                                .map(c -> new ItemStack(c.item()))
                                .toList()
                );

        // --- Output ---
        builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 20)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(RecipeHolder<ItemResearchRecipe> recipeHolder, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Draw animated arrow
        arrow.draw(guiGraphics, 39, 4);

        // Manual catalyst cycling and tooltip
        ItemResearchRecipe recipe = recipeHolder.value();
        List<ItemResearchRecipe.CatalystEntry> catalysts = recipe.catalysts();
        if (!catalysts.isEmpty()) {
            // Cycle through catalysts every second
            int catalystIndex = (int) ((System.currentTimeMillis() / 1000) % catalysts.size());
            ItemResearchRecipe.CatalystEntry currentCatalyst = catalysts.get(catalystIndex);
            ItemStack catalystStack = new ItemStack(currentCatalyst.item());

            // Draw the cycling catalyst item

           // guiGraphics.renderFakeItem(catalystStack, 51, 16);

            /** Draw tooltip if hovering
            if (mouseX >= 51 && mouseX < 51 + 16 && mouseY >= 16 && mouseY < 16 + 16) {
                float chance = currentCatalyst.successChance() * 100f;
                Component tooltip = Component.translatable(
                        "gui.roll_mod.jei.research_workbench.chance",
                        String.format("%.2f%%", chance)
                );
                guiGraphics.renderTooltip(Minecraft.getInstance().font, List.of(catalystStack.getHoverName(), tooltip), Optional.empty(), (int) mouseX, (int) mouseY);
            }
             */
        }
    }
}
