package com.roll_54.roll_mod_client.compat.emi;

import com.roll_54.roll_mod.recipe.ItemResearchRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Locale;

/**
 * Research workbench: main input + one of the catalysts gives the output. Each catalyst has its own
 * success chance, so the catalyst slot's tooltip lists them all.
 */
public class EmiResearchRecipe extends BasicEmiRecipe {
    private final List<ItemResearchRecipe.CatalystEntry> catalystEntries;

    public EmiResearchRecipe(RecipeHolder<ItemResearchRecipe> holder) {
        super(EMIRollModPlugin.ITEM_RESEARCH, holder.id(), 118, 18);
        ItemResearchRecipe recipe = holder.value();
        this.catalystEntries = recipe.catalysts();
        inputs.add(EmiStack.of(recipe.mainInput().item()));
        inputs.add(EmiIngredient.of(catalystEntries.stream().map(c -> EmiStack.of(c.item())).toList()));
        outputs.add(EmiStack.of(recipe.output()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 0);
        SlotWidget catalyst = widgets.addSlot(inputs.get(1), 20, 0);
        for (ItemResearchRecipe.CatalystEntry entry : catalystEntries) {
            catalyst.appendTooltip(Component.empty()
                    .append(entry.item().getDescription())
                    .append(": " + String.format(Locale.ROOT, "%.2f%%", entry.successChance() * 100f))
                    .withStyle(ChatFormatting.GRAY));
        }
        widgets.addFillingArrow(54, 0, 10_000);
        widgets.addSlot(outputs.get(0), 100, 0).recipeContext(this);
    }
}
