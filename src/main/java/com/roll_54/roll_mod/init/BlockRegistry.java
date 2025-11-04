package com.roll_54.roll_mod.init;


import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.block.FacingPlushBlock;
import com.roll_54.roll_mod.block.LatexDandelion;
import com.roll_54.roll_mod.block.SulfurBerryBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(RollMod.MODID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(RollMod.MODID);

    public static final DeferredBlock<Block> TREATED_PLANKS = BLOCKS.registerBlock(
            "treated_planks",
            Block::new,
            BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.BAMBOO_WOOD)
    );

    public static final DeferredBlock<Block> TREATED_LOG = BLOCKS.registerBlock(
            "treated_log",
            RotatedPillarBlock::new, // колона
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD)
    );

    public static final DeferredHolder<Block, LatexDandelion> LATEX_DANDELION =
            BLOCKS.register("latex_dandelion",
                    () -> new LatexDandelion(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .noCollission()
                                    .randomTicks()
                                    .instabreak()
                                    .sound(SoundType.CROP)
                                    .noOcclusion()
                    )
            );

    public static final DeferredHolder<Block, SulfurBerryBlock> SULFUR_BERRY_BLOCK =
            BLOCKS.register("sulfur_berry_block",
                    () -> new SulfurBerryBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.PLANT)
                                    .noCollission()
                                    .randomTicks()
                                    .instabreak()
                                    .sound(SoundType.CROP)
                                    .noOcclusion()
                    )
            );

    public static final DeferredBlock<Block> ROLL_PLUSH = BLOCKS.registerBlock(
            "roll_plush",
            FacingPlushBlock::new, // колона
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOL).noOcclusion()
    );
    public static final DeferredBlock<Block> YAN_PLUSH = BLOCKS.registerBlock(
            "yan_plush",
            FacingPlushBlock::new, // колона
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOL).noOcclusion()
    );
    public static final DeferredBlock<Block> LEDOK_PLUSH = BLOCKS.registerBlock(
            "ledok_plush",
            FacingPlushBlock::new, // колона
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOL).noOcclusion()
    );
    public static final DeferredBlock<Block> LORP_OOO_PLUSH = BLOCKS.registerBlock(
            "lorp_ooo_plush",
            FacingPlushBlock::new, // колона
            BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOL).noOcclusion()
    );

    public static final DeferredBlock<Block> MARS_LAPIS_TUNGSTEN_ORE = BLOCKS.registerBlock(
            "mars_lapis_tungsten",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
    );

    public static final DeferredBlock<Block> MOON_LAPIS_TUNGSTEN_ORE = BLOCKS.registerBlock(
            "moon_lapis_tungsten",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
    );

    public static final DeferredBlock<Block> STONE_LAPIS_TUNGSTEN_ORE = BLOCKS.registerBlock(
            "stone_lapis_tungsten",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
    );

    public static final DeferredBlock<Block> NETHERRACK_LAPIS_TUNGSTEN_ORE = BLOCKS.registerBlock(
            "netherrack_lapis_tungsten",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NETHER)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.NETHERRACK)
    );

    // Nickel ores
    public static final DeferredBlock<Block> MARS_REDSTONE_NICKEL_ORE = BLOCKS.registerBlock(
            "mars_redstone_nickel",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
    );

    public static final DeferredBlock<Block> STONE_REDSTONE_NICKEL_ORE = BLOCKS.registerBlock(
            "stone_redstone_nickel",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.STONE)
    );

    // Iridium ores (all bases)
    public static final DeferredBlock<Block> MARS_DIAMOND_IRIDIUM_ORE = BLOCKS.registerBlock(
            "mars_diamond_iridium",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 4.0F)
                    .sound(SoundType.STONE)
    );

    public static final DeferredBlock<Block> MOON_DIAMOND_IRIDIUM_ORE = BLOCKS.registerBlock(
            "moon_diamond_iridium",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 4.0F)
                    .sound(SoundType.STONE)
    );

    public static final DeferredBlock<Block> STONE_DIAMOND_IRIDIUM_ORE = BLOCKS.registerBlock(
            "stone_diamond_iridium",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 4.0F)
                    .sound(SoundType.STONE)
    );

    public static final DeferredBlock<Block> NETHERRACK_DIAMOND_IRIDIUM_ORE = BLOCKS.registerBlock(
            "netherrack_diamond_iridium",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 4.0F)
                    .sound(SoundType.STONE)
    );

    // Uranium ores
    public static final DeferredBlock<Block> MOON_QUARTZ_URANIUM_ORE = BLOCKS.registerBlock(
            "moon_quartz_uranium",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(4.0F, 4.0F)
                    .sound(SoundType.STONE)
    );

    public static final DeferredBlock<Block> STONE_QUARTZ_URANIUM_ORE = BLOCKS.registerBlock(
            "stone_quartz_uranium",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(4.0F, 4.0F)
                    .sound(SoundType.STONE)
    );

    static {
        ITEMS.registerSimpleBlockItem(TREATED_PLANKS);
        ITEMS.registerSimpleBlockItem(TREATED_LOG);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}