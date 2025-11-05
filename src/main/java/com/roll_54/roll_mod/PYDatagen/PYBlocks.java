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

    public static final DeferredHolder<Block, Block> STONE_KYANITE = registerSimpleBlock("stone_kyanite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> VENUS_KYANITE = registerSimpleBlock("venus_kyanite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_HEMATITE = registerSimpleBlock("stone_hematite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MARS_HEMATITE = registerSimpleBlock("mars_hematite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_YELLOW_LIMONITE = registerSimpleBlock("stone_yellow_limonite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_YELLOW_LIMONITE = registerSimpleBlock("netherrack_yellow_limonite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> DEEPSLATE_BIOTITE = registerSimpleBlock("deepslate_biotite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> MOON_BIOTITE = registerSimpleBlock("moon_biotite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_MAGNETITE = registerSimpleBlock("stone_magnetite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> DEEPSLATE_MAGNETITE = registerSimpleBlock("deepslate_magnetite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_GARNIERITE = registerSimpleBlock("stone_garnierite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> VENUS_GARNIERITE = registerSimpleBlock("venus_garnierite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> MARS_PENTLANDITE = registerSimpleBlock("mars_pentlandite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> STONE_PENTLANDITE = registerSimpleBlock("stone_pentlandite", ORE_PROPERTIES);

    public static final DeferredHolder<Block, Block> STONE_CHALCOPYRITE = registerSimpleBlock("stone_chalcopyrite", ORE_PROPERTIES);
    public static final DeferredHolder<Block, Block> NETHERRACK_CHALCOPYRITE = registerSimpleBlock("netherrack_chalcopyrite", ORE_PROPERTIES);

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

}
