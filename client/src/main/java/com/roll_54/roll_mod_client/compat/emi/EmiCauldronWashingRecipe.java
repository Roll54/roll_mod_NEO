package com.roll_54.roll_mod_client.compat.emi;

import com.roll_54.roll_mod.util.RollCauldronInteractions;
import com.roll_54.roll_mod_client.compat.jei.CauldronWashingRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluids;

/**
 * Cauldron washing: right-click a water cauldron with the dirty stack. The whole stack is washed at
 * once for one water level, and each item has a chance to drop one of the dusts beside the cauldron
 * (shown as chanced outputs, split evenly like {@link RollCauldronInteractions} picks them).
 */
public class EmiCauldronWashingRecipe extends BasicEmiRecipe {

    public EmiCauldronWashingRecipe(CauldronWashingRecipe recipe) {
        super(EMIRollModPlugin.CAULDRON_WASHING,
                EMIRollModPlugin.syntheticId("cauldron_washing", EMIRollModPlugin.itemId(recipe.getInput())), 134, 18);
        inputs.add(EmiStack.of(recipe.getInput()));
        catalysts.add(EmiStack.of(Fluids.WATER));
        outputs.add(EmiStack.of(recipe.getOutput()));

        Item[] dusts = RollCauldronInteractions.getDustItems();
        for (Item dust : dusts) {
            outputs.add(EmiStack.of(dust).setChance(recipe.getDustDropChance() / dusts.length));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 0);
        widgets.addTexture(EmiTexture.PLUS, 19, 3);
        // Water in the cauldron, not a bucket; how much it costs goes in the tooltip, not a text line.
        widgets.addSlot(catalysts.get(0), 33, 0).catalyst(true)
                .appendTooltip(Component.translatable("jei.roll_mod.cauldron_washing.water").withStyle(ChatFormatting.GRAY));
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 53, 1);
        widgets.addSlot(outputs.get(0), 80, 0).recipeContext(this);
        for (int i = 1; i < outputs.size(); i++) {
            widgets.addSlot(outputs.get(i), 98 + (i - 1) * 18, 0).recipeContext(this);
        }
    }
}
