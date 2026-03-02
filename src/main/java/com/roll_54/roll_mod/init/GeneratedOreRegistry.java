package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.py.OreDefinition;
import com.roll_54.roll_mod.data.datagen.py.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.py.PyTextureTemplates.BlockSubLayer;
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

public class GeneratedOreRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RollMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RollMod.MODID);

    private static final BlockBehaviour.Properties ORE_PROPERTIES =
            BlockBehaviour.Properties.of().strength(3.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);

    public static void register(IEventBus eventBus) {
        for (OreDefinition def : OreDefinitions.ALL) {
            registerOreSet(def);
        }
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }

    private static void registerOreSet(OreDefinition def) {
        String oreName = def.oreName();

        // Register ore blocks for each base
        for (BlockSubLayer base : def.bases()) {
            String blockId = base.id() + "_" + oreName;
            registerBlockWithItem(blockId, () -> new DropExperienceBlock(UniformInt.of(def.minExperience(), def.maxExperience()), ORE_PROPERTIES));
        }

        // Register associated items
        registerSimpleItem("raw_" + oreName);
        registerSimpleItem(oreName + "_dust");
        registerSimpleItem("crushed_" + oreName + "_ore");
        registerSimpleItem("refined_" + oreName + "_ore");
        registerSimpleItem("purified_" + oreName + "_ore");
        registerSimpleItem("pure_" + oreName + "_dust");
        registerSimpleItem("impure_" + oreName + "_dust");
    }

    private static <T extends Block> DeferredHolder<Block, T> registerBlockWithItem(String name, Supplier<T> blockSupplier) {
        DeferredHolder<Block, T> blockHolder = BLOCKS.register(name, blockSupplier);
        ITEMS.register(name, () -> new BlockItem(blockHolder.get(), new Item.Properties()));
        return blockHolder;
    }

    private static DeferredHolder<Item, Item> registerSimpleItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }
}
