package com.roll_54.roll_mod_client.compat.emi;

import com.roll_54.roll_mod_client.compat.jei.CropManagerInputCategory;
import com.roll_54.roll_mod_client.compat.jei.CropManagerInputRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * EMI page of one Crop Manager input; same data and text as {@link CropManagerInputCategory}.
 * The machines that take it are catalysts, so a Crop Manager's uses also list these pages.
 */
public class EmiCropManagerInputRecipe extends BasicEmiRecipe {
    private final CropManagerInputRecipe recipe;

    public EmiCropManagerInputRecipe(CropManagerInputRecipe recipe) {
        super(EMIRollModPlugin.CROP_MANAGER_INPUTS, id(recipe), CropManagerInputCategory.WIDTH, CropManagerInputCategory.HEIGHT);
        this.recipe = recipe;
        if (recipe.item() != null) {
            inputs.add(EmiStack.of(recipe.item()));
        } else {
            inputs.add(EmiStack.of(recipe.fluid(), 1000));
            ItemStack bucket = recipe.bucket();
            if (!bucket.isEmpty()) {
                inputs.add(EmiStack.of(bucket));
            }
        }
        catalysts.add(EmiIngredient.of(CropManagerInputRecipe.machines(recipe.input()).stream()
                .map(EmiStack::of).toList()));
    }

    private static ResourceLocation id(CropManagerInputRecipe recipe) {
        ResourceLocation subject = recipe.item() != null
                ? BuiltInRegistries.ITEM.getKey(recipe.item())
                : BuiltInRegistries.FLUID.getKey(recipe.fluid());
        return EMIRollModPlugin.syntheticId("crop_manager_inputs/" + recipe.input().name().toLowerCase(Locale.ROOT), subject);
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
        widgets.addSlot(catalysts.get(0), CropManagerInputCategory.MACHINE_X - 1, 6).catalyst(true);
        widgets.addText(CropManagerInputCategory.slotLine(recipe), CropManagerInputCategory.TEXT_X, 6,
                CropManagerInputCategory.COLOR_SLOT, false);
        widgets.addText(CropManagerInputCategory.infoLine(recipe), CropManagerInputCategory.TEXT_X, 17,
                CropManagerInputCategory.COLOR_INFO, false);
    }
}
