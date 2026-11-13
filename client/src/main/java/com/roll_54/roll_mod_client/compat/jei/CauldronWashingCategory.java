package com.roll_54.roll_mod_client.compat.jei;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.util.RollCauldronInteractions;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

/**
 * Cauldron washing, in one row of plain slots the way Fruits Delight lays out its cauldron recipes:
 * [dirty stack] → [washed stack], plus the stone and clay dust each washed item may
 * drop, with their chance in the tooltip.
 */
public class CauldronWashingCategory implements IRecipeCategory<CauldronWashingRecipe> {
    public static final RecipeType<CauldronWashingRecipe> TYPE = RecipeType.create(RollMod.MODID, "cauldron_washing", CauldronWashingRecipe.class);

    private static final int SLOT = 18;
    private static final int ARROW_X = 22;
    private static final int OUT_X = 52;
    private static final int WIDTH = OUT_X + 3 * SLOT;
    private static final int HEIGHT = SLOT + 2;

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public CauldronWashingCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.CAULDRON));
        this.arrow = gui.getRecipeArrow();
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
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull CauldronWashingRecipe recipe, @NotNull IFocusGroup focuses) {
        // In a water cauldron; how much water it takes goes in the tooltip.
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
                .setStandardSlotBackground()
                .addItemStack(recipe.getInput())
                .addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.translatable("jei.roll_mod.cauldron_washing.water").withStyle(ChatFormatting.GRAY)));

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 1, 1)
                .setStandardSlotBackground()
                .addItemStack(recipe.getOutput());

        Item[] dusts = RollCauldronInteractions.getDustItems();
        float chance = dustChance(recipe);
        for (int i = 0; i < dusts.length; i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, OUT_X + 1 + (1 + i) * SLOT, 1)
                    .setStandardSlotBackground()
                    .addItemStack(new ItemStack(dusts[i]))
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                            "jei.roll_mod.cauldron_washing.chance", percent(chance)).withStyle(ChatFormatting.GOLD)));
        }
    }

    @Override
    public void draw(@NotNull CauldronWashingRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics gui, double mouseX, double mouseY) {
        // The dusts' chances are in their tooltips.
        arrow.draw(gui, ARROW_X, 1);
    }

    /** Each dust's own chance per washed item: one dust drops at most, picked evenly. */
    static float dustChance(CauldronWashingRecipe recipe) {
        return recipe.getDustDropChance() / RollCauldronInteractions.getDustItems().length;
    }

    static String percent(float chance) {
        float value = chance * 100f;
        return value == Math.rint(value) ? String.format("%d%%", (int) value) : String.format("%.1f%%", value);
    }
}
