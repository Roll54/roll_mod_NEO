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

/**
 * Cauldron washing, in one row of plain slots the way Fruits Delight lays out its cauldron recipes:
 * [dirty stack] → [washed stack], plus the stone and clay dust each washed item may
 * drop (chanced outputs, split evenly like {@link RollCauldronInteractions} picks them). The whole stack is
 * washed at once for one water level.
 */
public class EmiCauldronWashingRecipe extends BasicEmiRecipe {

    private static final int SLOT = 18;
    private static final int OUT_X = 52;

    public EmiCauldronWashingRecipe(CauldronWashingRecipe recipe) {
        super(EMIRollModPlugin.CAULDRON_WASHING,
                EMIRollModPlugin.syntheticId("cauldron_washing", EMIRollModPlugin.itemId(recipe.getInput())), OUT_X + 3 * SLOT, SLOT);
        inputs.add(EmiStack.of(recipe.getInput()));
        outputs.add(EmiStack.of(recipe.getOutput()));

        Item[] dusts = RollCauldronInteractions.getDustItems();
        for (Item dust : dusts) {
            outputs.add(EmiStack.of(dust).setChance(recipe.getDustDropChance() / dusts.length));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        // In a water cauldron; how much water it takes goes in the tooltip.
        widgets.addSlot(inputs.get(0), 0, 0)
                .appendTooltip(Component.translatable("jei.roll_mod.cauldron_washing.water").withStyle(ChatFormatting.GRAY));
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 23, 1);
        widgets.addSlot(outputs.get(0), OUT_X, 0).recipeContext(this);
        // The dusts' chances show on their slots (EMI draws a chanced output's percent itself).
        for (int i = 1; i < outputs.size(); i++) {
            widgets.addSlot(outputs.get(i), OUT_X + i * SLOT, 0).recipeContext(this);
        }
    }
}
