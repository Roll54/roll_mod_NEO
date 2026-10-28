package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.codecs.AgriProduct;
import com.agricraft.agricraft.api.plant.AgriPlant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Reads a plant's harvest products with their chance and amount range. AgriCraft only hands them out
 * as rolled or flattened stacks; the EV Greenhouse needs the definitions to size its output and to
 * show yields per gain in JEI.
 */
@Mixin(AgriPlant.class)
public interface AgriPlantProductsAccessor {

    @Accessor("products")
    List<AgriProduct> roll_mod$getProducts();
}
