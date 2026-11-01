package com.roll_54.roll_mod_client.compat.jei;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.codecs.AgriProduct;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.roll_54.roll_mod.compat.MBD2.greenhouse.GreenhouseMachines;
import com.roll_54.roll_mod.mixin.crops.AgriPlantProductsAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * One JEI page of the EV Greenhouse: an AgriCraft plant, its seed and its harvest products with their
 * chance and amount range, from which the category works out the yield per gain.
 */
public record GreenhouseYieldRecipe(ResourceLocation plantId, AgriPlant plant, ItemStack seed, List<AgriProduct> products) {

    /** Every plant that yields something, from the client's copy of the plant registry. */
    public static List<GreenhouseYieldRecipe> collect() {
        List<GreenhouseYieldRecipe> recipes = new ArrayList<>();
        AgriApi.getPlantRegistry().ifPresent(registry -> registry.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().location().toString()))
                .forEach(entry -> {
                    AgriPlant plant = entry.getValue();
                    List<AgriProduct> products = ((AgriPlantProductsAccessor) (Object) plant).roll_mod$getProducts();
                    if (products == null || products.isEmpty()) return;
                    recipes.add(new GreenhouseYieldRecipe(entry.getKey().location(), plant,
                            AgriSeedItem.toStack(plant), List.copyOf(products)));
                }));
        return recipes;
    }

    /** The items a product can be: every item of its tag, or its one item. */
    public static List<ItemStack> stacks(AgriProduct product) {
        ExtraCodecs.TagOrElementLocation location = product.item();
        List<ItemStack> stacks = new ArrayList<>();
        if (location.tag()) {
            BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, location.id()))
                    .ifPresent(tag -> tag.forEach(holder -> stacks.add(new ItemStack(holder))));
        } else {
            Item item = BuiltInRegistries.ITEM.get(location.id());
            stacks.add(new ItemStack(item));
        }
        int amount = Math.max(1, product.max());
        stacks.replaceAll(stack -> stack.copyWithCount(amount));
        return stacks;
    }

    /** Expected items of {@code product} from one harvest with {@code rolls} rolls. */
    public static double expected(AgriProduct product, int rolls) {
        return rolls * Math.min(1.0, Math.max(0.0, product.chance())) * (product.min() + product.max()) / 2.0;
    }

    // Shared by the JEI and EMI pages. Kept here rather than in GreenhouseYieldCategory, which
    // implements a JEI interface and so cannot be loaded when only EMI is installed.

    /** Gains grouped by equal rolls: 1-2, 3-5, 6-8, 9-10. */
    public static final int[][] GAIN_GROUPS = { {1, 2}, {3, 5}, {6, 8}, {9, 10} };

    /** The controller's item, for the icon and as the catalyst. */
    public static ItemStack controllerStack() {
        // MBD2 registers each machine's item under the machine's own id.
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(GreenhouseMachines.CONTROLLER_ID).orElse(Items.WHEAT_SEEDS));
    }

    public static String percent(double chance) {
        return String.format(Locale.ROOT, "%.0f%%", Math.min(1.0, chance) * 100);
    }

    /** Short average: whole numbers plain, fractions to two places. */
    public static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005) return String.valueOf((long) Math.rint(value));
        return String.format(Locale.ROOT, value >= 10 ? "%.1f" : "%.2f", value);
    }

    /** Game ticks as m:ss, or "-" for a plant that never grows. */
    public static String time(long ticks) {
        if (ticks < 0) return "-";
        long seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
