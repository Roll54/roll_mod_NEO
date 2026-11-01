package com.roll_54.roll_mod_client.compat.emi;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.MBD2.machine.RollMBD2Machines;
import com.roll_54.roll_mod.compat.MBD2.recipe.ExampleLavaRecipe;
import com.roll_54.roll_mod.gui.menu.ResearchWorkbenchMenu;
import com.roll_54.roll_mod.minestar.hub.gui.HubReturn;
import com.roll_54.roll_mod.recipe.ItemResearchRecipe;
import com.roll_54.roll_mod.registry.BlockRegistry;
import com.roll_54.roll_mod.registry.MenuTypes;
import com.roll_54.roll_mod.registry.RecipeRegistry;
import com.roll_54.roll_mod_client.compat.jei.CauldronWashingRecipe;
import com.roll_54.roll_mod_client.compat.jei.CropManagerInputRecipe;
import com.roll_54.roll_mod_client.compat.jei.GreenhouseYieldRecipe;
import com.roll_54.roll_mod_client.compat.jei.HydroponicReagentRecipe;
import com.roll_54.roll_mod_client.config.RecipeViewerConfig;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * Native EMI counterpart of {@link com.roll_54.roll_mod_client.compat.jei.JEIRollModPlugin}.
 *
 * <p>EMI would normally import JEI plugins on its own, but common's vending-block EMI plugin makes
 * EMI treat the whole {@code roll_mod} namespace as handled natively, so every roll_mod JEI category
 * is skipped. The categories here reuse the JEI ids, which also keeps EMI from listing a JEI copy
 * next to them when both mods are installed.
 */
@EmiEntrypoint
public class EMIRollModPlugin implements EmiPlugin {

    public static final EmiRecipeCategory ITEM_RESEARCH = category("item_research",
            EmiStack.of(BlockRegistry.RESEARCH_WORKBENCH.get()), "gui.roll_mod.jei.research_workbench.title");
    public static final EmiRecipeCategory CAULDRON_WASHING = category("cauldron_washing",
            EmiStack.of(Items.CAULDRON), "jei.roll_mod.cauldron_washing");
    public static final EmiRecipeCategory HYDROPONIC_REAGENTS = category("hydroponic_reagents",
            EmiStack.of(BlockRegistry.HYDROPONIC_GARDEN_BED.get()), "jei.roll_mod.hydroponic_reagents");
    public static final EmiRecipeCategory CROP_MANAGER_INPUTS = category("crop_manager_inputs",
            EmiStack.of(CropManagerInputRecipe.machine(RollMBD2Machines.CropManagerTier.LV)), "jei.roll_mod.crop_manager_inputs");
    public static final EmiRecipeCategory EV_GREENHOUSE_YIELD = category("ev_greenhouse_yield",
            EmiStack.of(GreenhouseYieldRecipe.controllerStack()), "jei.roll_mod.ev_greenhouse_yield");

    // Research workbench menu: 36 player slots first, then main input, catalyst, output.
    private static final int PLAYER_SLOTS = 36;
    private static final int MAIN_INPUT_SLOT = PLAYER_SLOTS;
    private static final int CATALYST_SLOT = PLAYER_SLOTS + 1;

    private static final ResourceLocation LDLIB_TEST_CATEGORY = ResourceLocation.fromNamespaceAndPath("ldlib2", "test_category");

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ITEM_RESEARCH);
        registry.addCategory(CAULDRON_WASHING);
        registry.addCategory(HYDROPONIC_REAGENTS);
        registry.addCategory(EV_GREENHOUSE_YIELD);
        registry.addCategory(CROP_MANAGER_INPUTS);

        registry.addWorkstation(ITEM_RESEARCH, EmiStack.of(BlockRegistry.RESEARCH_WORKBENCH.get()));
        registry.addWorkstation(CAULDRON_WASHING, EmiStack.of(Items.CAULDRON));
        registry.addWorkstation(HYDROPONIC_REAGENTS, EmiStack.of(BlockRegistry.HYDROPONIC_GARDEN_BED.get()));
        registry.addWorkstation(EV_GREENHOUSE_YIELD, EmiStack.of(GreenhouseYieldRecipe.controllerStack()));
        for (RollMBD2Machines.CropManagerTier tier : RollMBD2Machines.CropManagerTier.values()) {
            registry.addWorkstation(CROP_MANAGER_INPUTS, EmiStack.of(CropManagerInputRecipe.machine(tier)));
        }

        for (RecipeHolder<ItemResearchRecipe> holder
                : registry.getRecipeManager().getAllRecipesFor(RecipeRegistry.ITEM_RESEARCH_RECIPE_TYPE.get())) {
            registry.addRecipe(new EmiResearchRecipe(holder));
        }
        for (CauldronWashingRecipe recipe : CauldronWashingRecipe.collect()) {
            registry.addRecipe(new EmiCauldronWashingRecipe(recipe));
        }
        // Built from the synced data maps / plant registry, so EMI's reload picks up datapack changes.
        for (HydroponicReagentRecipe recipe : HydroponicReagentRecipe.collect()) {
            registry.addRecipe(new EmiHydroponicReagentRecipe(recipe));
        }
        for (CropManagerInputRecipe recipe : CropManagerInputRecipe.collect()) {
            registry.addRecipe(new EmiCropManagerInputRecipe(recipe));
        }
        for (GreenhouseYieldRecipe recipe : GreenhouseYieldRecipe.collect()) {
            registry.addRecipe(new EmiGreenhouseYieldRecipe(recipe));
        }

        // LDLib2 ships a developer test category in its JEI plugin, which EMI imports. EMI applies these
        // filters once every plugin has registered, and a category left with no recipes gets no tab.
        registry.removeRecipes(recipe -> LDLIB_TEST_CATEGORY.equals(recipe.getCategory().getId()));
        // MBD2 names its EMI categories after the recipe type; the example lava smeltery is opt-in.
        if (!RecipeViewerConfig.SHOW_LAVA_SMELTERY.get()) {
            registry.removeRecipes(recipe -> ExampleLavaRecipe.LAVA_SMELTERY.equals(recipe.getCategory().getId()));
        }

        registry.addRecipeHandler(MenuTypes.RESEARCH_WORKBENCH_MENU.get(), new StandardRecipeHandler<ResearchWorkbenchMenu>() {
            @Override
            public List<Slot> getInputSources(ResearchWorkbenchMenu menu) {
                return menu.slots.subList(0, PLAYER_SLOTS + 2);
            }

            @Override
            public List<Slot> getCraftingSlots(ResearchWorkbenchMenu menu) {
                return List.of(menu.slots.get(MAIN_INPUT_SLOT), menu.slots.get(CATALYST_SLOT));
            }

            @Override
            public boolean supportsRecipe(EmiRecipe recipe) {
                return recipe.getCategory() == ITEM_RESEARCH;
            }
        });

        // Same reason as JEIRollModPlugin#registerHubBounds: the hub's root covers the whole screen,
        // so EMI needs the floating window's rectangle or it has no room for its panels.
        registry.addScreenBoundsProvider(HubReturn.screenClass(), screen -> {
            float[] rect = HubReturn.windowRect(screen);
            if (rect == null) return null;
            return new Bounds(Math.round(rect[0]), Math.round(rect[1]), Math.round(rect[2]), Math.round(rect[3]));
        });
    }

    private static EmiRecipeCategory category(String path, EmiStack icon, String titleKey) {
        return new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(RollMod.MODID, path), icon) {
            @Override
            public Component getName() {
                return Component.translatable(titleKey);
            }
        };
    }

    /** Synthetic recipe id; EMI's convention is a path starting with "/" for recipes not in the recipe manager. */
    static ResourceLocation syntheticId(String category, ResourceLocation subject) {
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID,
                "/" + category + "/" + subject.getNamespace() + "/" + subject.getPath());
    }

    static ResourceLocation itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
