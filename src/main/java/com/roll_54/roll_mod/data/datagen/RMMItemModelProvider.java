package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.init.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class RMMItemModelProvider extends ItemModelProvider {
    public RMMItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, RollMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ItemRegistry.PURPLE_BOULE.get());
        basicItem(ItemRegistry.PURPLE_WAFER.get());
        basicItem(ItemRegistry.PURPLE_WAFER_ACCUMULATION.get());
        basicItem(ItemRegistry.PURPLE_CHIP_ACCUMULATION.get());
        basicItem(ItemRegistry.PURPLE_WAFER_QUBIT.get());
        basicItem(ItemRegistry.PURPLE_CHIP_QUBIT.get());
        basicItem(ItemRegistry.PURPLE_WAFER_SOC.get());
        basicItem(ItemRegistry.PURPLE_CHIP_SOC.get());
        basicItem(ItemRegistry.PURPLE_WAFER_RAM.get()); // absent
        basicItem(ItemRegistry.PURPLE_WAFER_CONCURRENT.get()); // absent

        basicItem(ItemRegistry.BLUE_BOULE.get());
        basicItem(ItemRegistry.BLUE_WAFER.get());
        basicItem(ItemRegistry.BLUE_WAFER_RAM.get()); // absent
        basicItem(ItemRegistry.BLUE_CHIP_RAM.get());
        basicItem(ItemRegistry.BLUE_WAFER_CONCURRENT.get());
        basicItem(ItemRegistry.BLUE_CHIP_CONCURRENT.get());

        basicItem(ItemRegistry.BLUE_WAFER_NOT.get());
        basicItem(ItemRegistry.BLUE_WAFER_OR.get());
        basicItem(ItemRegistry.BLUE_WAFER_AND.get()); // absent
        basicItem(ItemRegistry.BLUE_WAFER_NAND.get()); // absent
        basicItem(ItemRegistry.BLUE_WAFER_PMIC.get()); // absent

        basicItem(ItemRegistry.STANDARD_WAFER_NOT.get());
        basicItem(ItemRegistry.STANDARD_CHIP_NOT.get());
        basicItem(ItemRegistry.STANDARD_WAFER_OR.get());
        basicItem(ItemRegistry.STANDARD_CHIP_OR.get());
        basicItem(ItemRegistry.STANDARD_WAFER_AND.get());
        basicItem(ItemRegistry.STANDARD_CHIP_AND.get());
        basicItem(ItemRegistry.STANDARD_WAFER_NAND.get());
        basicItem(ItemRegistry.STANDARD_CHIP_NAND.get());
        basicItem(ItemRegistry.STANDARD_WAFER_PMIC.get()); // absent
        basicItem(ItemRegistry.STANDARD_CHIP_PMIC.get()); // absent

        basicItem(ItemRegistry.RED_LENS.get());
        basicItem(ItemRegistry.GREEN_LENS.get());
        basicItem(ItemRegistry.YELLOW_LENS.get());
        basicItem(ItemRegistry.WHITE_LENS.get());
        basicItem(ItemRegistry.PURPLE_LENS.get());
        basicItem(ItemRegistry.LIGHT_BLUE_LENS.get());
        basicItem(ItemRegistry.PALE_LENS.get());

        basicItem(ItemRegistry.REDSTONE_TUBE.get());
        basicItem(ItemRegistry.ENERGIUM_LASER.get());

        basicItem(ItemRegistry.STONE_DUST.get());
        basicItem(ItemRegistry.DEEPSLATE_DUST.get());
        basicItem(ItemRegistry.NETHERRACK_DUST.get());
        basicItem(ItemRegistry.END_STONE_DUST.get());
        basicItem(ItemRegistry.MOON_STONE_DUST.get());
        basicItem(ItemRegistry.MARS_STONE_DUST.get());
        basicItem(ItemRegistry.VENUS_STONE_DUST.get());
     //   basicItem(ItemRegistry.MERCURY_STONE_DUST.get()); needs texture

    }
}
