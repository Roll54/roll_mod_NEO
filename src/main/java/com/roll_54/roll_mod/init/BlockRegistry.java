package com.roll_54.roll_mod.init;


import com.roll_54.roll_mod.RollMod;
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


    static {
        ITEMS.registerSimpleBlockItem(TREATED_PLANKS);
        ITEMS.registerSimpleBlockItem(TREATED_LOG);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}