package com.roll_54.roll_mod_client.compat.jei;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.codecs.AgriProduct;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.common.item.AgriSeedItem;
import com.roll_54.roll_mod.mixin.crops.AgriPlantProductsAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
}
