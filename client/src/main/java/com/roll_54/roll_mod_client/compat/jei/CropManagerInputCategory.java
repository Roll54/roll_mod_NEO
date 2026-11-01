package com.roll_54.roll_mod_client.compat.jei;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.MBD2.machine.RollMBD2Machines.CropManagerTier;
import com.roll_54.roll_mod.data.datamap.NutrientValue;
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
 * What goes into the LV/MV/HV Crop Managers and where: herbicide slot / tank, N/P/K slot / tank and
 * the mutagen tank, each with the tiers that have it. The machines that accept the entry are shown
 * as a third slot, so looking up a Crop Manager's uses also lands here.
 */
public class CropManagerInputCategory implements IRecipeCategory<CropManagerInputRecipe> {
    public static final RecipeType<CropManagerInputRecipe> TYPE =
            RecipeType.create(RollMod.MODID, "crop_manager_inputs", CropManagerInputRecipe.class);

    public static final int WIDTH = 160;
    public static final int HEIGHT = 30;
    public static final int MACHINE_X = 140;
    public static final int TEXT_X = 46;
    public static final int COLOR_SLOT = 0xFF2E7D32;
    public static final int COLOR_INFO = 0xFF555555;

    private final IDrawable icon;

    public CropManagerInputCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, CropManagerInputRecipe.machine(CropManagerTier.LV));
    }

    @Override
    public @NotNull RecipeType<CropManagerInputRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.roll_mod.crop_manager_inputs");
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
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull CropManagerInputRecipe recipe, @NotNull IFocusGroup focuses) {
        if (recipe.item() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 4, 7)
                    .setStandardSlotBackground()
                    .addItemStack(new ItemStack(recipe.item()));
        } else {
            builder.addSlot(RecipeIngredientRole.INPUT, 4, 7)
                    .setStandardSlotBackground()
                    .setFluidRenderer(1000, false, 16, 16)
                    .addFluidStack(recipe.fluid(), 1000);
            ItemStack bucket = recipe.bucket();
            if (!bucket.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT, 24, 7)
                        .setStandardSlotBackground()
                        .addItemStack(bucket);
            }
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, MACHINE_X, 7)
                .addItemStacks(CropManagerInputRecipe.machines(recipe.input()));
    }

    @Override
    public void draw(@NotNull CropManagerInputRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView,
                     @NotNull GuiGraphics gui, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        gui.drawString(font, slotLine(recipe), TEXT_X, 6, COLOR_SLOT, false);
        gui.drawString(font, infoLine(recipe), TEXT_X, 17, COLOR_INFO, false);
    }

    public static Component slotLine(CropManagerInputRecipe recipe) {
        return Component.translatable(recipe.input().key());
    }

    /** Tiers, plus the N/P/K the entry feeds for nutrient inputs (per item / per bucket). */
    public static Component infoLine(CropManagerInputRecipe recipe) {
        NutrientValue npk = recipe.npk();
        if (npk == null) {
            return Component.translatable("jei.roll_mod.crop_manager_inputs.tiers", recipe.input().tiers());
        }
        return Component.translatable("jei.roll_mod.crop_manager_inputs.npk", recipe.input().tiers(),
                npk.nitrogen(), npk.phosphorus(), npk.potassium());
    }
}
