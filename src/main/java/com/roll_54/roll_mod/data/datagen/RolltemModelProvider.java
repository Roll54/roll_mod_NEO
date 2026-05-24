package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Objects;

public class RolltemModelProvider extends ItemModelProvider {
    public RolltemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
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
        basicItem(ItemRegistry.MERCURY_STONE_DUST.get());

        basicItem(ItemRegistry.OVERWORLD_CARTRIDGE.get());
        basicItem(ItemRegistry.NETHER_CARTRIDGE.get());
        basicItem(ItemRegistry.END_CARTRIDGE.get());
        basicItem(ItemRegistry.MOON_CARTRIDGE.get());
        basicItem(ItemRegistry.MARS_CARTRIDGE.get());
        basicItem(ItemRegistry.MERCURY_CARTRIDGE.get());
        basicItem(ItemRegistry.VENUS_CARTRIDGE.get());
        basicItem(ItemRegistry.EMPTY_CARTRIDGE.get());

        getBuilder(ItemRegistry.RESEARCH_ROCKET_CONTROLLER.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research_paper_special"));

        getBuilder(ItemRegistry.RESEARCH_ROCKET_T2.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research_paper_special1"));

        getBuilder(ItemRegistry.RESEARCH_ROCKET_T3.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research"));

        getBuilder(ItemRegistry.RESEARCH_LITHOGRAPHY.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research_paper_special"));

        getBuilder(ItemRegistry.RESEARCH_HIGH_EFFICIENCY_STEAM_TURBINE.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research_paper_special1"));

        getBuilder(ItemRegistry.RESEARCH_BETTER_CIRCUITS_LV.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research"));

        getBuilder(ItemRegistry.RESEARCH_BETTER_CIRCUITS_MV.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research_paper_special"));

        getBuilder(ItemRegistry.RESEARCH_BETTER_CIRCUITS_HV.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research_paper_special1"));

        getBuilder(ItemRegistry.RESEARCH_BETTER_CIRCUITS_EV.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research"));

        getBuilder(ItemRegistry.RESEARCH_BETTER_CIRCUITS_IV.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research_paper_special"));

        getBuilder(ItemRegistry.RESEARCH_ENERGY_CRYSTALS.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/handheld"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/research_paper_special1"));

        basicItem(ItemRegistry.LITHIUM_SOAP.get());
        basicItem(ItemRegistry.POTASSIUM_SOAP.get());
        basicItem(ItemRegistry.SODIUM_SOAP.get());
        basicItem(ItemRegistry.EMPTY_PRIMITIVE_BATTERY.get());
        basicItem(ItemRegistry.PRIMITIVE_BATTERY.get());
        basicItem(ItemRegistry.RAW_QUANTUM_BOOTS.get());
        basicItem(ItemRegistry.RAW_QUANTUM_CHESTPLATE.get());
        basicItem(ItemRegistry.RAW_QUANTUM_LEGGINGS.get());
        basicItem(ItemRegistry.RAW_QUANTUM_HELMET.get());
        basicItem(ItemRegistry.RAW_PLATINUM_DUST.get());
        basicItem(ItemRegistry.RAW_PALLADIUM_DUST.get());
        basicItem(ItemRegistry.RAW_CRYSTAL_CHIP.get());
        basicItem(ItemRegistry.RAW_CRYSTAL_CHIP_PARTS.get());
        basicItem(ItemRegistry.RAW_LATEX.get());
        basicItem(ItemRegistry.RAW_RUBBER.get());

        cropModelItem(ItemRegistry.ICEBERG_MINT_LEAF.get());
        cropModelItem(ItemRegistry.ICEBERG_MINT_SEEDS.get());


    }


    //мій апі для створення кропсів, я чуть чуть їбав всі текстури тикати в папку ітемів тому воно тут.
    ItemModelBuilder cropModelItem(Item item) {
        return this.cropModelItem((ResourceLocation) Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)));
    }


    ItemModelBuilder cropModelItem(ResourceLocation item){
        return (ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)this.getBuilder(item.toString())).parent(new ModelFile.UncheckedModelFile("item/generated"))).texture("layer0", ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "item/crop_textures/" + item.getPath()));
    }
}







