package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;

/**
 * roll_mod's own recipe types (growth chamber, item research, cauldron washing…) have no place in the vanilla recipe
 * book, which logged "Unknown recipe category" for each of their recipes on every join. They are filed under
 * {@link RecipeBookCategories#UNKNOWN}: never shown in the book, and no warning.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class RecipeBookCategoriesSetup {

    private RecipeBookCategoriesSetup() {}

    @SubscribeEvent
    public static void onRegister(RegisterRecipeBookCategoriesEvent event) {
        BuiltInRegistries.RECIPE_TYPE.entrySet().forEach(entry -> {
            if (RollMod.MODID.equals(entry.getKey().location().getNamespace())) {
                event.registerRecipeCategoryFinder(entry.getValue(), recipe -> RecipeBookCategories.UNKNOWN);
            }
        });
    }
}
