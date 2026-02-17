package com.roll_54.roll_mod.init;


import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.*;
import com.roll_54.roll_mod.blocks.regenblock.RegenBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
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

    public static final DeferredBlock<Block> TREATED_LOG = BLOCKS.register(
            "treated_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD))
    );

    public static final DeferredBlock<Block> LAPOTRONIC_LASER_BLOCK = BLOCKS.register(
            "lapotronic_laser_block",
            () -> new Block(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.AMETHYST))
    );

    public static final DeferredBlock<Block> LATEX_DANDELION =
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

    public static final DeferredBlock<Block> SULFUR_BERRY_BLOCK =
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

    public static final DeferredBlock<Block> ROLL_PLUSH = BLOCKS.register(
            "roll_plush",
            () -> new RollPlushBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOL).noOcclusion())
    );
    public static final DeferredBlock<Block> YAN_PLUSH = BLOCKS.register(
            "yan_plush",
            () -> new FacingPlushBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOL).noOcclusion())
    );
    public static final DeferredBlock<Block> LEDOK_PLUSH = BLOCKS.register(
            "ledok_plush",
            () -> new FacingPlushBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOL).noOcclusion())
    );
    public static final DeferredBlock<Block> LORP_OOO_PLUSH = BLOCKS.register(
            "lorp_ooo_plush",
            () -> new FacingPlushBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOL).noOcclusion())
    );

    public static final DeferredBlock<Block> BUKVI_ORE_BLOCK = BLOCKS.register(
            "bukvi_ore_block",
            () -> new RegenBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.AMETHYST))
    );

    static {
        ITEMS.register("treated_planks", () -> new BlockItem(TREATED_PLANKS.get(), new Item.Properties()));
        ITEMS.register("treated_log", () -> new BlockItem(TREATED_LOG.get(), new Item.Properties()));
        ITEMS.register("lapotronic_laser_block", () -> new BlockItem(LAPOTRONIC_LASER_BLOCK.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}