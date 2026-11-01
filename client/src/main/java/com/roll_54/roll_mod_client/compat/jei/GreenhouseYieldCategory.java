package com.roll_54.roll_mod_client.compat.jei;

import com.agricraft.agricraft.api.codecs.AgriProduct;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.MBD2.greenhouse.GreenhouseGrowth;
import com.roll_54.roll_mod.compat.MBD2.greenhouse.GreenhouseMachines;
import com.roll_54.roll_mod.compat.MBD2.greenhouse.GreenhouseSettings;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * What one crop grown in the EV Greenhouse yields, per gain value.
 *
 * <p>AgriCraft harvests {@code (gain + 3) / 3} times per mature plant, and each time every product
 * drops on its own chance with an amount in its range. The grid shows the <em>average</em> items of
 * each product per harvest for each group of gains with the same number of rolls; hovering a product
 * gives its chance and range. Under it, the base time of one regrowth at growth 1 and 10 (upgrades
 * shorten it). All numbers come from {@link GreenhouseGrowth}, which the machine itself uses.
 */
public class GreenhouseYieldCategory implements IRecipeCategory<GreenhouseYieldRecipe> {
    public static final RecipeType<GreenhouseYieldRecipe> TYPE =
            RecipeType.create(RollMod.MODID, "ev_greenhouse_yield", GreenhouseYieldRecipe.class);

    private static final int WIDTH = 176;
    private static final int SLOT = 18;
    private static final int PRODUCTS_X = 40;
    /** As many product columns as fit beside the seed. */
    private static final int MAX_PRODUCTS = (WIDTH - PRODUCTS_X) / SLOT;
    private static final int TABLE_Y = 24;
    private static final int LINE = 10;
    /** Gains grouped by equal rolls: 1-2, 3-5, 6-8, 9-10. */
    private static final int[][] GAIN_GROUPS = { {1, 2}, {3, 5}, {6, 8}, {9, 10} };
    private static final int HEIGHT = TABLE_Y + (GAIN_GROUPS.length + 1) * LINE + 2 * LINE + 4;

    private static final int COLOR_TEXT = 0xFF404040;
    private static final int COLOR_DIM = 0xFF707070;

    private final IDrawable icon;

    public GreenhouseYieldCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableIngredient(VanillaTypes.ITEM_STACK, controllerStack());
    }

    /** The controller's item, for the icon and as the catalyst. */
    public static ItemStack controllerStack() {
        // MBD2 registers each machine's item under the machine's own id.
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(GreenhouseMachines.CONTROLLER_ID).orElse(Items.WHEAT_SEEDS));
    }

    @Override
    public @NotNull RecipeType<GreenhouseYieldRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.roll_mod.ev_greenhouse_yield");
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
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull GreenhouseYieldRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 4, 3)
                .setStandardSlotBackground()
                .addItemStack(recipe.seed());
        List<AgriProduct> products = recipe.products();
        for (int i = 0; i < Math.min(products.size(), MAX_PRODUCTS); i++) {
            AgriProduct product = products.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, PRODUCTS_X + i * SLOT, 3)
                    .setStandardSlotBackground()
                    .addItemStacks(GreenhouseYieldRecipe.stacks(product))
                    .addRichTooltipCallback((view, tooltip) -> {
                        tooltip.add(Component.translatable("jei.roll_mod.ev_greenhouse_yield.chance",
                                percent(product.chance())).withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.translatable("jei.roll_mod.ev_greenhouse_yield.amount",
                                product.min(), product.max()).withStyle(ChatFormatting.GRAY));
                        for (int[] group : GAIN_GROUPS) {
                            int rolls = GreenhouseGrowth.harvestRolls(group[0]);
                            tooltip.add(Component.translatable("jei.roll_mod.ev_greenhouse_yield.per_gain",
                                    group[0], group[1], rolls, number(GreenhouseYieldRecipe.expected(product, rolls)))
                                    .withStyle(ChatFormatting.DARK_GRAY));
                        }
                    });
        }
    }

    @Override
    public void draw(@NotNull GreenhouseYieldRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView,
                     @NotNull GuiGraphics gui, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        gui.drawString(font, Component.translatable("jei.roll_mod.ev_greenhouse_yield.header"), 4, TABLE_Y, COLOR_DIM, false);

        List<AgriProduct> products = recipe.products();
        int columns = Math.min(products.size(), MAX_PRODUCTS);
        for (int row = 0; row < GAIN_GROUPS.length; row++) {
            int[] group = GAIN_GROUPS[row];
            int rolls = GreenhouseGrowth.harvestRolls(group[0]);
            int y = TABLE_Y + (row + 1) * LINE;
            gui.drawString(font, group[0] + "-" + group[1] + " ×" + rolls, 4, y, COLOR_TEXT, false);
            for (int i = 0; i < columns; i++) {
                String value = number(GreenhouseYieldRecipe.expected(products.get(i), rolls));
                int x = PRODUCTS_X + i * SLOT + (SLOT - font.width(value)) / 2;
                gui.drawString(font, value, x, y, COLOR_TEXT, false);
            }
        }

        int y = TABLE_Y + (GAIN_GROUPS.length + 1) * LINE + 2;
        gui.drawString(font, Component.translatable("jei.roll_mod.ev_greenhouse_yield.cycle",
                time(GreenhouseGrowth.cycleTicks(recipe.plant(), 1)),
                time(GreenhouseGrowth.cycleTicks(recipe.plant(), 10))), 4, y, COLOR_TEXT, false);
        gui.drawString(font, Component.translatable("jei.roll_mod.ev_greenhouse_yield.energy",
                String.format(Locale.ROOT, "%,d", GreenhouseSettings.RECIPE_EU)), 4, y + LINE, COLOR_DIM, false);
    }

    private static String percent(double chance) {
        return String.format(Locale.ROOT, "%.0f%%", Math.min(1.0, chance) * 100);
    }

    /** Short average: whole numbers plain, fractions to two places. */
    private static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005) return String.valueOf((long) Math.rint(value));
        return String.format(Locale.ROOT, value >= 10 ? "%.1f" : "%.2f", value);
    }

    /** Game ticks as m:ss, or "-" for a plant that never grows. */
    private static String time(long ticks) {
        if (ticks < 0) return "-";
        long seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
