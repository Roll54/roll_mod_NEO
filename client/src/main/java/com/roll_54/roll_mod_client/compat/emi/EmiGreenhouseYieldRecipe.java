package com.roll_54.roll_mod_client.compat.emi;

import com.agricraft.agricraft.api.codecs.AgriProduct;
import com.roll_54.roll_mod.compat.MBD2.greenhouse.GreenhouseGrowth;
import com.roll_54.roll_mod.compat.MBD2.greenhouse.GreenhouseSettings;
import com.roll_54.roll_mod_client.compat.jei.GreenhouseYieldRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * EMI page of the EV Greenhouse yield; same layout and numbers as
 * {@link com.roll_54.roll_mod_client.compat.jei.GreenhouseYieldCategory}.
 */
public class EmiGreenhouseYieldRecipe extends BasicEmiRecipe {
    private static final int WIDTH = 176;
    private static final int SLOT = 18;
    private static final int PRODUCTS_X = 40;
    private static final int MAX_PRODUCTS = (WIDTH - PRODUCTS_X) / SLOT;
    private static final int TABLE_Y = 24;
    private static final int LINE = 10;
    private static final int[][] GAIN_GROUPS = GreenhouseYieldRecipe.GAIN_GROUPS;
    private static final int HEIGHT = TABLE_Y + (GAIN_GROUPS.length + 1) * LINE + 2 * LINE + 4;

    private static final int COLOR_TEXT = 0xFF404040;
    private static final int COLOR_DIM = 0xFF707070;

    private final GreenhouseYieldRecipe recipe;
    private final List<AgriProduct> products;

    public EmiGreenhouseYieldRecipe(GreenhouseYieldRecipe recipe) {
        super(EMIRollModPlugin.EV_GREENHOUSE_YIELD, EMIRollModPlugin.syntheticId("ev_greenhouse_yield", recipe.plantId()), WIDTH, HEIGHT);
        this.recipe = recipe;
        this.products = recipe.products().subList(0, Math.min(recipe.products().size(), MAX_PRODUCTS));
        inputs.add(EmiStack.of(recipe.seed()));
        for (AgriProduct product : products) {
            // A tag product shows as a cycling list; EMI outputs are single stacks, so the first one
            // stands in for lookups.
            List<EmiStack> stacks = GreenhouseYieldRecipe.stacks(product).stream().map(EmiStack::of).toList();
            if (!stacks.isEmpty()) outputs.add(stacks.get(0));
        }
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 4, 3);
        for (int i = 0; i < products.size(); i++) {
            AgriProduct product = products.get(i);
            List<EmiStack> stacks = GreenhouseYieldRecipe.stacks(product).stream().map(EmiStack::of).toList();
            SlotWidget slot = widgets.addSlot(EmiIngredient.of(stacks), PRODUCTS_X + i * SLOT, 3).recipeContext(this);
            slot.appendTooltip(Component.translatable("jei.roll_mod.ev_greenhouse_yield.chance",
                    GreenhouseYieldRecipe.percent(product.chance())).withStyle(ChatFormatting.GRAY));
            slot.appendTooltip(Component.translatable("jei.roll_mod.ev_greenhouse_yield.amount",
                    product.min(), product.max()).withStyle(ChatFormatting.GRAY));
            for (int[] group : GAIN_GROUPS) {
                int rolls = GreenhouseGrowth.harvestRolls(group[0]);
                slot.appendTooltip(Component.translatable("jei.roll_mod.ev_greenhouse_yield.per_gain",
                        group[0], group[1], rolls, GreenhouseYieldRecipe.number(GreenhouseYieldRecipe.expected(product, rolls)))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        widgets.addText(Component.translatable("jei.roll_mod.ev_greenhouse_yield.header"), 4, TABLE_Y, COLOR_DIM, false);
        for (int row = 0; row < GAIN_GROUPS.length; row++) {
            int[] group = GAIN_GROUPS[row];
            int rolls = GreenhouseGrowth.harvestRolls(group[0]);
            int y = TABLE_Y + (row + 1) * LINE;
            widgets.addText(Component.literal(group[0] + "-" + group[1] + " ×" + rolls), 4, y, COLOR_TEXT, false);
            for (int i = 0; i < products.size(); i++) {
                String value = GreenhouseYieldRecipe.number(GreenhouseYieldRecipe.expected(products.get(i), rolls));
                widgets.addText(Component.literal(value), PRODUCTS_X + i * SLOT + SLOT / 2, y, COLOR_TEXT, false)
                        .horizontalAlign(TextWidget.Alignment.CENTER);
            }
        }

        int y = TABLE_Y + (GAIN_GROUPS.length + 1) * LINE + 2;
        widgets.addText(Component.translatable("jei.roll_mod.ev_greenhouse_yield.cycle",
                GreenhouseYieldRecipe.time(GreenhouseGrowth.cycleTicks(recipe.plant(), 1)),
                GreenhouseYieldRecipe.time(GreenhouseGrowth.cycleTicks(recipe.plant(), 10))), 4, y, COLOR_TEXT, false);
        widgets.addText(Component.translatable("jei.roll_mod.ev_greenhouse_yield.energy",
                String.format(Locale.ROOT, "%,d", GreenhouseSettings.RECIPE_EU)), 4, y + LINE, COLOR_DIM, false);
    }
}
