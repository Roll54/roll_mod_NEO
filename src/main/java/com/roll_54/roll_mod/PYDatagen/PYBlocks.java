package com.roll_54.roll_mod.PYDatagen;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.init.ItemRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;



public class PYBlocks {
    public static final DeferredRegister.Blocks ORE_BLOCKS =
            DeferredRegister.createBlocks(RollMod.MODID);

    public static final BlockBehaviour.Properties ORE_PROPERTIES =
            BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.STONE);



    private static <T extends Block> DeferredHolder<Block, T> registerBlockAndItem(String name, Supplier<T> blockSupplier) {

        DeferredHolder<Block, T> blockHolder = ORE_BLOCKS.register(name, blockSupplier);


        ItemRegistry.ITEMS.register(name, () -> new BlockItem(blockHolder.get(), new Item.Properties()));

        return blockHolder;
    }

    private static DeferredHolder<Block, Block> registerSimpleBlock(String name, BlockBehaviour.Properties props) {
        return registerBlockAndItem(name, () -> new Block(props));
    }



    public static void register(IEventBus eventBus) {
        ORE_BLOCKS.register(eventBus);
    }

    public static final DeferredHolder<Block, Block> STONE_MICA = registerSimpleBlock("stone_mica", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> DEEPSLATE_MICA = registerSimpleBlock("deepslate_mica", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_MICA = registerSimpleBlock("netherrack_mica", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_KYANITE = registerSimpleBlock("stone_kyanite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> VENUS_KYANITE = registerSimpleBlock("venus_kyanite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_HEMATITE = registerSimpleBlock("stone_hematite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MARS_HEMATITE = registerSimpleBlock("mars_hematite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_YELLOW_LIMONITE = registerSimpleBlock("stone_yellow_limonite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_YELLOW_LIMONITE = registerSimpleBlock("netherrack_yellow_limonite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> DEEPSLATE_BIOTITE = registerSimpleBlock("deepslate_biotite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MOON_BIOTITE = registerSimpleBlock("moon_biotite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MARS_MAGNETITE = registerSimpleBlock("mars_magnetite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MOON_MAGNETITE = registerSimpleBlock("moon_magnetite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> STONE_MAGNETITE = registerSimpleBlock("stone_magnetite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_MAGNETITE = registerSimpleBlock("netherrack_magnetite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> DEEPSLATE_MAGNETITE = registerSimpleBlock("deepslate_magnetite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> VENUS_MAGNETITE = registerSimpleBlock("venus_magnetite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MERCURY_MAGNETITE = registerSimpleBlock("mercury_magnetite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> END_MAGNETITE = registerSimpleBlock("end_magnetite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_GARNIERITE = registerSimpleBlock("stone_garnierite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> VENUS_GARNIERITE = registerSimpleBlock("venus_garnierite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MARS_PENTLANDITE = registerSimpleBlock("mars_pentlandite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> STONE_PENTLANDITE = registerSimpleBlock("stone_pentlandite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_CHALCOPYRITE = registerSimpleBlock("stone_chalcopyrite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_CHALCOPYRITE = registerSimpleBlock("netherrack_chalcopyrite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MOON_CHALCOPYRITE = registerSimpleBlock("moon_chalcopyrite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> NETHERRACK_CERTUS_QUARTZ = registerSimpleBlock("netherrack_certus_quartz", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MOON_CERTUS_QUARTZ = registerSimpleBlock("moon_certus_quartz", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_PYROCHLORE = registerSimpleBlock("stone_pyrochlore", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> DEEPSLATE_PYROCHLORE = registerSimpleBlock("deepslate_pyrochlore", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_PYROPE = registerSimpleBlock("stone_pyrope", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> VENUS_PYROPE = registerSimpleBlock("venus_pyrope", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> VENUS_APATITE = registerSimpleBlock("venus_apatite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MOON_APATITE = registerSimpleBlock("moon_apatite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_GALENA = registerSimpleBlock("stone_galena", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MARS_GALENA = registerSimpleBlock("mars_galena", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> DEEPSLATE_PYROLUSITE = registerSimpleBlock("deepslate_pyrolusite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> STONE_PYROLUSITE = registerSimpleBlock("stone_pyrolusite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_CHROMITE = registerSimpleBlock("stone_chromite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_CHROMITE = registerSimpleBlock("netherrack_chromite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_MALACHITE = registerSimpleBlock("stone_malachite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> VENUS_MALACHITE = registerSimpleBlock("venus_malachite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_FLUORITE = registerSimpleBlock("stone_fluorite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MOON_FLUORITE = registerSimpleBlock("moon_fluorite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_FLUORITE = registerSimpleBlock("netherrack_fluorite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_PYRITE = registerSimpleBlock("stone_pyrite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MARS_PYRITE = registerSimpleBlock("mars_pyrite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_CINNABAR = registerSimpleBlock("stone_cinnabar", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_CINNABAR = registerSimpleBlock("netherrack_cinnabar", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_PERIDOT = registerSimpleBlock("stone_peridot", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> DEEPSLATE_PERIDOT = registerSimpleBlock("deepslate_peridot", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_SODALITE = registerSimpleBlock("stone_sodalite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MOON_SODALITE = registerSimpleBlock("moon_sodalite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> NETHERRACK_TETRAHEDRITE = registerSimpleBlock("netherrack_tetrahedrite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MARS_TETRAHEDRITE = registerSimpleBlock("mars_tetrahedrite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> NETHERRACK_STIBNITE = registerSimpleBlock("netherrack_stibnite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> VENUS_STIBNITE = registerSimpleBlock("venus_stibnite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> VENUS_ILMENITE = registerSimpleBlock("venus_ilmenite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> STONE_ILMENITE = registerSimpleBlock("stone_ilmenite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> VENUS_BLUE_TOPAZ = registerSimpleBlock("venus_blue_topaz", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_BLUE_TOPAZ = registerSimpleBlock("netherrack_blue_topaz", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MARS_TOPAZ = registerSimpleBlock("mars_topaz", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_TOPAZ = registerSimpleBlock("netherrack_topaz", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> DEEPSLATE_CHALCOCITE = registerSimpleBlock("deepslate_chalcocite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_CHALCOCITE = registerSimpleBlock("netherrack_chalcocite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MOON_BORNITE = registerSimpleBlock("moon_bornite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_BORNITE = registerSimpleBlock("netherrack_bornite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> NETHERRACK_SULFUR = registerSimpleBlock("netherrack_sulfur", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_SPHALERITE = registerSimpleBlock("stone_sphalerite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_SPHALERITE = registerSimpleBlock("netherrack_sphalerite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MERCURY_MONAZITE = registerSimpleBlock("mercury_monazite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_MONAZITE = registerSimpleBlock("netherrack_monazite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> END_RUBY = registerSimpleBlock("end_ruby", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_RUBY = registerSimpleBlock("netherrack_ruby", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> STONE_RUBY = registerSimpleBlock("stone_ruby", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MARS_REDSTONE = registerSimpleBlock("mars_redstone", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_REDSTONE = registerSimpleBlock("netherrack_redstone", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> STONE_REDSTONE = registerSimpleBlock("stone_redstone", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_SALTPETER = registerSimpleBlock("stone_saltpeter", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_SALTPETER = registerSimpleBlock("netherrack_saltpeter", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> VENUS_EMERALD = registerSimpleBlock("venus_emerald", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_EMERALD = registerSimpleBlock("netherrack_emerald", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> DEEPSLATE_BERYLLIUM = registerSimpleBlock("deepslate_beryllium", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_BERYLLIUM = registerSimpleBlock("netherrack_beryllium", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_TANTALITE = registerSimpleBlock("stone_tantalite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_TANTALITE = registerSimpleBlock("netherrack_tantalite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MOON_MOLYBDENITE = registerSimpleBlock("moon_molybdenite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_MOLYBDENITE = registerSimpleBlock("netherrack_molybdenite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> NETHERRACK_WULFENITE = registerSimpleBlock("netherrack_wulfenite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MERCURY_POWELLITE = registerSimpleBlock("mercury_powellite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_POWELLITE = registerSimpleBlock("netherrack_powellite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MARS_MOLYBDENUM = registerSimpleBlock("mars_molybdenum", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_MOLYBDENUM = registerSimpleBlock("netherrack_molybdenum", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_GOLD = registerSimpleBlock("stone_gold", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_GOLD = registerSimpleBlock("netherrack_gold", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> DEEPSLATE_GOLD = registerSimpleBlock("deepslate_gold", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> DEEPSLATE_GOETHITE = registerSimpleBlock("deepslate_goethite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_GOETHITE = registerSimpleBlock("netherrack_goethite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> STONE_GOETHITE = registerSimpleBlock("stone_goethite", ORE_PROPERTIES);

}
