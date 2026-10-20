package com.roll_54.roll_mod_client.compat.jei;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.BlockRegistry;
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
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

/**
 * Lists every reagent the Hydroponic Garden Bed accepts, straight from the {@code roll_mod:acidity}
 * and {@code roll_mod:fertilizer} data maps (plus {@code c:water}), so datapack additions show up
 * without any code change.
 *
 * <p>Fluids get a second slot with their bucket, since the bed's input slots drain containers in
 * place — that way looking up the uses of a filled bucket also lands here.
 */
public class HydroponicReagentCategory implements IRecipeCategory<HydroponicReagentRecipe> {
    public static final RecipeType<HydroponicReagentRecipe> TYPE =
            RecipeType.create(RollMod.MODID, "hydroponic_reagents", HydroponicReagentRecipe.class);

    private static final int WIDTH = 150;
    private static final int HEIGHT = 30;
    private static final int TEXT_X = 46;

    private static final int COLOR_ACID = 0xFF8A6D00;
    private static final int COLOR_BASE = 0xFF6A1B9A;
    private static final int COLOR_WATER = 0xFF1565C0;
    private static final int COLOR_FERTILIZER = 0xFF2E7D32;
    private static final int COLOR_UNIT = 0xFF555555;

    private final IDrawable icon;

    public HydroponicReagentCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(BlockRegistry.HYDROPONIC_GARDEN_BED));
    }

    @Override
    public @NotNull RecipeType<HydroponicReagentRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.roll_mod.hydroponic_reagents");
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
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull HydroponicReagentRecipe recipe, @NotNull IFocusGroup focuses) {
        if (recipe.item() != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 4, 7)
                    .setStandardSlotBackground()
                    .addItemStack(new ItemStack(recipe.item()));
            return;
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 7)
                .setStandardSlotBackground()
                .setFluidRenderer(1000, false, 16, 16)
                .addFluidStack(recipe.fluid(), 1000);
        ItemStack bucket = new ItemStack(recipe.fluid().getBucket());
        if (!bucket.isEmpty() && !bucket.is(Items.BUCKET)) {
            builder.addSlot(RecipeIngredientRole.INPUT, 24, 7)
                    .setStandardSlotBackground()
                    .addItemStack(bucket);
        }
    }

    @Override
    public void draw(@NotNull HydroponicReagentRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView,
                     @NotNull GuiGraphics gui, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int value = recipe.value();
        String amount = String.format("%,d", Math.abs(value));
        Component line = switch (recipe.kind()) {
            case ACIDITY -> value > 0
                    ? Component.translatable("jei.roll_mod.hydroponic_reagents.acid", amount)
                    : Component.translatable("jei.roll_mod.hydroponic_reagents.base", amount);
            case WATER -> Component.translatable("jei.roll_mod.hydroponic_reagents.water", amount);
            case FERTILIZER -> Component.translatable("jei.roll_mod.hydroponic_reagents.fertilizer", amount);
        };
        int color = switch (recipe.kind()) {
            case ACIDITY -> value > 0 ? COLOR_ACID : COLOR_BASE;
            case WATER -> COLOR_WATER;
            case FERTILIZER -> COLOR_FERTILIZER;
        };
        gui.drawString(font, line, TEXT_X, 6, color, false);
        gui.drawString(font, Component.translatable(recipe.isFluid()
                ? "jei.roll_mod.hydroponic_reagents.per_bucket"
                : "jei.roll_mod.hydroponic_reagents.per_item"), TEXT_X, 17, COLOR_UNIT, false);
    }
}
