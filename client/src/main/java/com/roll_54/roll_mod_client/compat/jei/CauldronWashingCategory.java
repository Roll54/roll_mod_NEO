package com.roll_54.roll_mod_client.compat.jei;

import com.roll_54.roll_mod.RollMod;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Category to show cauldron washing: left = impure input, right = cleaned output.
 */
public class CauldronWashingCategory implements IRecipeCategory<CauldronWashingRecipe> {
    public static final RecipeType<CauldronWashingRecipe> TYPE = RecipeType.create(RollMod.MODID, "cauldron_washing", CauldronWashingRecipe.class);
    private final IDrawable background;
    private final IDrawable icon;

    public CauldronWashingCategory(IGuiHelper gui) {
        this.background = gui.createBlankDrawable(140, 60);
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(net.minecraft.world.level.block.Blocks.CAULDRON));
    }

    @Override
    public @NotNull RecipeType<CauldronWashingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.roll_mod.cauldron_washing");
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 140;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull CauldronWashingRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 20).addItemStack(recipe.getInput());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 110, 20).addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(@NotNull CauldronWashingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics gui, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        gui.drawString(font, "→", 70, 26, 0xFFFFFF);
        gui.drawString(font, String.format("Bonus: %.0f%%", recipe.getDustDropChance() * 100f), 70 - 30, 40, 0xAAAAAA);
    }
}
