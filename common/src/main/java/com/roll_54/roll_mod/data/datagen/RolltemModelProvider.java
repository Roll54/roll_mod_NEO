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

    public enum AgricraftCropModel {
        CROP_HASH("crop_hash"),
        CROP_PLUS("crop_plus"),
        CROP_GOURD("crop_gourd"),
        CROP_CROSS("crop_cross");

        private final String modelId;

        AgricraftCropModel(String modelId) {
            this.modelId = modelId;
        }

        public String modelId() {
            return modelId;
        }
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
        basicItem(ItemRegistry.BLUE_CHIP_CPU.get());

        basicItem(ItemRegistry.BLUE_WAFER_NOT.get());
        basicItem(ItemRegistry.BLUE_WAFER_AND.get());
        basicItem(ItemRegistry.BLUE_WAFER_NAND.get());
        basicItem(ItemRegistry.BLUE_WAFER_PMIC.get());
        basicItem(ItemRegistry.BLUE_WAFER_CPU.get());


        basicItem(ItemRegistry.STANDARD_WAFER_NOT.get());
        basicItem(ItemRegistry.STANDARD_CHIP_NOT.get());
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
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
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
        basicItem(ItemRegistry.BIOMASS.get());
        basicItem(ItemRegistry.POTASSIUM_NITRITE_DUST.get());
        basicItem(ItemRegistry.SODIUM_NITRITE_DUST.get());
        basicItem(ItemRegistry.FLINT_DUST.get());
        basicItem(ItemRegistry.DIORITE_DUST.get());
        basicItem(ItemRegistry.ANDESITE_DUST.get());
        basicItem(ItemRegistry.GRANITE_DUST.get());
        basicItem(ItemRegistry.FELDSPARS_DUST.get());
        //  basicItem(ItemRegistry.METAL_MIXTURE_DUST.get());
        basicItem(ItemRegistry.MARBLE_DUST.get());
        basicItem(ItemRegistry.CALCITE_DUST.get());
        basicItem(ItemRegistry.BASALT_DUST.get());
        basicItem(ItemRegistry.TUFF_DUST.get());
        basicItem(ItemRegistry.QUBIT_SENSOR.get());
        basicItem(ItemRegistry.ALUMINA_CERAMIC_PLATE.get());
        basicItem(ItemRegistry.ALUMINA_SMALL_CERAMIC_PLATE.get());
        basicItem(ItemRegistry.SULFUR_SALTPETER_MIXTURE.get());
        basicItem(ItemRegistry.METAL_MIXTURE_DUST.get());

        handheldItem(ItemRegistry.HERBICIDE_TIER_1.get());
        handheldItem(ItemRegistry.HERBICIDE_TIER_2.get());
        handheldItem(ItemRegistry.HERBICIDE_TIER_3.get());

        basicItem(ItemRegistry.IRIDIUM_BASED_BOARD_ASSEMBLY.get());
        basicItem(ItemRegistry.IRIDIUM_BASED_BOARD.get());
        basicItem(ItemRegistry.NYLON_FABRIC.get());
        basicItem(ItemRegistry.NYLON_STRING.get());

        basicItem(ItemRegistry.LUNGSUPGRADE_SULFUR_RESISTANCE.get());


        // TEXTURES ARE NOT NEDEED FOR THESE ITEMS, THEY ARE DEV ONES.
        getBuilder(ItemRegistry.EXAMPLE_ARMOR_HELMET.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/error_item"));
        getBuilder(ItemRegistry.COPPER_GEAR.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/error_item"));
        getBuilder(ItemRegistry.CHEMICAL_CORE.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/error_item"));
        getBuilder(ItemRegistry.SUPERSTEEL_GEAR.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/error_item"));
        getBuilder(ItemRegistry.SUPER_CIRCUIT.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/error_item"));
        getBuilder(ItemRegistry.HERBICIDE.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/error_item"));
        getBuilder(ItemRegistry.ENERGY_SWORD.get().toString())
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/error_item"));
        //END OF DEV ITEMS

        seedItemModel(ItemRegistry.ICEBERG_MINT_LEAF.get());
        seedItemModel(ItemRegistry.ICEBERG_MINT_SEEDS.get());
        seedItemModel(ItemRegistry.NIKELIA_FLOWERS.get());
        seedItemModel(ItemRegistry.NIKELIA_SEEDS.get());
        seedItemModel(ItemRegistry.STONELIA_FLOWERS.get());
        seedItemModel(ItemRegistry.STONELIA_SEEDS.get());
        seedItemModel(ItemRegistry.RED_BELL_PEPPER_SEEDS.get());
        seedItemModel(ItemRegistry.YELLOW_BELL_PEPPER_SEEDS.get());
        seedItemModel(ItemRegistry.GREEN_BELL_PEPPER_SEEDS.get());
        seedItemModel(ItemRegistry.SULFUR_BERRY_COFFEE_BEANS.get());
        seedItemModel(ItemRegistry.SULFUR_BERRY_COFFEE_SEEDS.get());
        seedItemModel(ItemRegistry.FLUORITE_PINEAPPLE.get());
        seedItemModel(ItemRegistry.FLUORITE_PINEAPPLE_SEEDS.get());
        seedItemModel(ItemRegistry.HOPS_LEAF.get());
        seedItemModel(ItemRegistry.HOPS_SEEDS.get());
        seedItemModel(ItemRegistry.RUTILE_BELL_PEPPER.get());
        seedItemModel(ItemRegistry.RUTILE_BELL_PEPPER_SEEDS.get());
        seedItemModel(ItemRegistry.COFFEE_BEANS.get());
        seedItemModel(ItemRegistry.COFFEE_SEEDS.get());
        seedItemModel(ItemRegistry.MAGNETITE_SPINACH.get());
        seedItemModel(ItemRegistry.MAGNETITE_SPINACH_SEEDS.get());

        // generates seed JSON: assets/roll_mod/models/seed/iceberg_mint.json
        // and 8 crop stage JSONs: assets/roll_mod/models/crop/iceberg_mint_stage0.json to _stage7.json
        generateAgricraftPlantModels("iceberg_mint", 4, AgricraftCropModel.CROP_CROSS);

        generateAgricraftPlantModels("sulfur_berry", 4, AgricraftCropModel.CROP_CROSS);
        generateAgricraftPlantModels("latex_dandelion", 6, AgricraftCropModel.CROP_HASH);
        generateAgricraftPlantModels("nikelia", 4, AgricraftCropModel.CROP_HASH);
        generateAgricraftPlantModels("stonelia", 4, AgricraftCropModel.CROP_CROSS);

        generateAgricraftPlantModels("wild_bell_pepper", 8, AgricraftCropModel.CROP_CROSS);
        generateAgricraftPlantModels("red_bell_pepper", 8, AgricraftCropModel.CROP_CROSS);
        generateAgricraftPlantModels("yellow_bell_pepper", 8, AgricraftCropModel.CROP_CROSS);
        generateAgricraftPlantModels("green_bell_pepper", 8, AgricraftCropModel.CROP_CROSS);

        generateAgricraftPlantModels("sulfur_berry_coffee", 6, AgricraftCropModel.CROP_CROSS);

        generateAgricraftPlantModels("rice", 4, AgricraftCropModel.CROP_CROSS);
        generateAgricraftPlantModels("cabbage", 8, AgricraftCropModel.CROP_CROSS);
        generateAgricraftPlantModels("tomato", 8, AgricraftCropModel.CROP_HASH);

        generateAgricraftPlantModels("rutile_bell_pepper", 8, AgricraftCropModel.CROP_CROSS);

        generateAgricraftPlantModels("coffee", 6, AgricraftCropModel.CROP_CROSS);
        generateAgricraftPlantModels("magnetite_spinach", 4, AgricraftCropModel.CROP_CROSS);

        generateAgricraftPlantModels("fluorite_pineapple", 5, AgricraftCropModel.CROP_CROSS);




    }


    //мій апі для створення кропсів, я чуть чуть їбав всі текстури тикати в папку ітемів тому воно тут.
    ItemModelBuilder seedItemModel(Item item) {
        return this.seedItemModel((ResourceLocation) Objects.requireNonNull(BuiltInRegistries.ITEM.getKey(item)));
    }
    ItemModelBuilder seedItemModel(ResourceLocation item){
        return (ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)this.getBuilder(item.toString())).parent(new ModelFile.UncheckedModelFile("item/generated"))).texture("layer0", ResourceLocation.fromNamespaceAndPath(item.getNamespace(), "item/crop_textures/" + item.getPath()));
    }

    public void generateAgricraftPlantModels(String plantName, int numTextures, AgricraftCropModel cropModel) {
        if (numTextures < 1 || numTextures > 8) {
            throw new IllegalArgumentException("numTextures must be between 1 and 8 inclusive");
        }

        // 1. Generate the seed model (e.g. models/seed/iceberg_mint.json)
        this.getBuilder("seed/" + plantName)
                .parent(new ModelFile.UncheckedModelFile("item/generated"))
                .texture("layer0", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "item/crop_textures/" + plantName + "_seeds"));

        // 2. Generate 8 crop stages (0 through 7)
        for (int stage = 0; stage < 8; stage++) {
            // Equally divide the 8 stages among the available textures:
            // e.g. if numTextures = 4, stage 0,1 -> texture 0; stage 2,3 -> texture 1, etc.
            int textureIndex = (stage * numTextures) / 8;

            this.getBuilder("crop/" + plantName + "_stage" + stage)
                    .parent(new ModelFile.UncheckedModelFile("agricraft:crop/" + cropModel.modelId()))
                    .texture("crop", ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "block/crops/" + plantName + "_stage" + textureIndex));
        }
    }


}
