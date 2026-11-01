package com.roll_54.roll_mod_client.compat.emi;

import com.roll_54.roll_mod_client.compat.jei.HydroponicReagentRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * EMI page of one Hydroponic Garden Bed reagent; same data and text as
 * {@link com.roll_54.roll_mod_client.compat.jei.HydroponicReagentCategory}. Fluids get their bucket
 * as a second input, since the bed drains containers in place.
 */
public class EmiHydroponicReagentRecipe extends BasicEmiRecipe {
    private static final int TEXT_X = 46;

    private static final int COLOR_ACID = 0xFF8A6D00;
    private static final int COLOR_BASE = 0xFF6A1B9A;
    private static final int COLOR_WATER = 0xFF1565C0;
    private static final int COLOR_FERTILIZER = 0xFF2E7D32;
    private static final int COLOR_UNIT = 0xFF555555;

    private final HydroponicReagentRecipe recipe;

    public EmiHydroponicReagentRecipe(HydroponicReagentRecipe recipe) {
        super(EMIRollModPlugin.HYDROPONIC_REAGENTS, id(recipe), 150, 30);
        this.recipe = recipe;
        if (recipe.item() != null) {
            inputs.add(EmiStack.of(recipe.item()));
        } else {
            inputs.add(EmiStack.of(recipe.fluid(), 1000));
            ItemStack bucket = new ItemStack(recipe.fluid().getBucket());
            if (!bucket.isEmpty() && !bucket.is(Items.BUCKET)) {
                inputs.add(EmiStack.of(bucket));
            }
        }
    }

    private static ResourceLocation id(HydroponicReagentRecipe recipe) {
        ResourceLocation subject = recipe.item() != null
                ? BuiltInRegistries.ITEM.getKey(recipe.item())
                : BuiltInRegistries.FLUID.getKey(recipe.fluid());
        return EMIRollModPlugin.syntheticId("hydroponic_reagents/" + recipe.kind().name().toLowerCase(Locale.ROOT), subject);
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < inputs.size(); i++) {
            widgets.addSlot(inputs.get(i), 3 + i * 20, 6);
        }

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
        widgets.addText(line, TEXT_X, 6, color, false);
        widgets.addText(Component.translatable(recipe.isFluid()
                ? "jei.roll_mod.hydroponic_reagents.per_bucket"
                : "jei.roll_mod.hydroponic_reagents.per_item"), TEXT_X, 17, COLOR_UNIT, false);
    }
}
