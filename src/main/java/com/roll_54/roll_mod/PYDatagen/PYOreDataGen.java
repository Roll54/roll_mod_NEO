/*
package com.roll_54.roll_mod.PYDatagen;


import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.init.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class PYOreDataGen {

    public static final DeferredRegister.Blocks ORE_BLOCKS =
            DeferredRegister.createBlocks(RollMod.MODID);

    public static final DeferredRegister.Items ORE_ITEMS =
            DeferredRegister.createItems(RollMod.MODID);

    public static final BlockBehaviour.Properties ORE_PROPERTIES =
            BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.STONE);

    // =============================== //
    // ======== базові методи ======== //
    // =============================== //

    private static <T extends Block> DeferredHolder<Block, T> registerBlockAndItem(
            String name,
            Supplier<T> blockSupplier
    ) {
        DeferredHolder<Block, T> blockHolder = ORE_BLOCKS.register(name, blockSupplier);
        ItemRegistry.ITEMS.register(
                name,
                () -> new BlockItem(blockHolder.get(), new Item.Properties())
        );
        return blockHolder;
    }

    private static DeferredHolder<Block, Block> registerSimpleBlock(
            String name,
            BlockBehaviour.Properties props
    ) {
        return registerBlockAndItem(name, () -> new Block(props));
    }

    private static DeferredHolder<Item, Item> registerSimpleItem(String name) {
        return ORE_ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredHolder<Block, Block> registerXpOreBlock(
            String name,
            BlockBehaviour.Properties props,
            int minXp,
            int maxXp
    ) {
        return registerBlockAndItem(
                name,
                () -> new DropExperienceBlock(
                        UniformInt.of(minXp, maxXp),
                        props
                )
        );
    }



    private static void registerOreVariants(
            String oreName, BlockBehaviour.Properties props, int minXp, int maxXp, String block_base, String item_base, String... dimensions) {
        for (String prefix : dimensions) {
            String id = prefix + "_" + oreName;

            // XP-руда
            registerXpOreBlock(id, props, minXp, maxXp);
        }

        registerSimpleItem("raw_" + oreName);
        registerSimpleItem("crushed_" + oreName + "_ore");
        registerSimpleItem("impure_" + oreName + "_dust");

        registerSimpleItem("refined_" + oreName + "_ore");
        registerSimpleItem("pure_" + oreName + "_dust");

        registerSimpleItem("purified_" + oreName + "_ore");
        registerSimpleItem(oreName + "_dust");
    }

    public static void register(IEventBus eventBus) {
        ORE_BLOCKS.register(eventBus);
        ORE_ITEMS.register(eventBus);
    }

}

 */
