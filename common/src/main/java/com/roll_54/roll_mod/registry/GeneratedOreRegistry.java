package com.roll_54.roll_mod.registry;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.OreSampleBlock;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.StoneVariant;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GeneratedOreRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RollMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RollMod.MODID);

    private static final BlockBehaviour.Properties ORE_PROPERTIES =
            BlockBehaviour.Properties.of().strength(3.0F, 3.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);

    private static final BlockBehaviour.Properties RAW_BLOCK_PROPERTIES =
            BlockBehaviour.Properties.of().strength(5.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL);

    /** Ore samples are thin decorative outcrops: walkable (no collision) and quick to mine. */
    private static final BlockBehaviour.Properties ORE_SAMPLE_PROPERTIES =
            BlockBehaviour.Properties.of().strength(1.0F, 1.0F).requiresCorrectToolForDrops().sound(SoundType.STONE)
                    .noCollission().noOcclusion().pushReaction(PushReaction.DESTROY);

    public static void register(IEventBus eventBus) {
        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
            registerOreSet(def);
        }
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }

    private static void registerOreSet(OreDefinition def) {
        String oreName = def.id();

        // Register ore blocks for each base, plus the matching surface sample
        for (BlockSubLayer base : def.bases()) {
            String blockId = base.id() + "_" + oreName;
            registerBlockWithItem(blockId, () -> new DropExperienceBlock(UniformInt.of(def.minExperience(), def.maxExperience()), ORE_PROPERTIES));
            registerBlockWithItem(oreSampleId(base, oreName), () -> new OreSampleBlock(ORE_SAMPLE_PROPERTIES));
        }

        // The same ore in the rocks that come with those layers (granite, tuff, basalt...). No samples of
        // their own: a vein's samples are its base layer's.
        for (StoneVariant variant : def.variants()) {
            registerBlockWithItem(def.variantBlockId(variant), () -> new DropExperienceBlock(UniformInt.of(def.minExperience(), def.maxExperience()), ORE_PROPERTIES));
        }

        // Register raw storage block (texture chosen from the raw pool, tinted per-ore)
        registerBlockWithItem("raw_" + oreName + "_block", () -> new Block(RAW_BLOCK_PROPERTIES));

        // Register associated items
        registerSimpleItem("raw_" + oreName);
        registerSimpleItem(oreName + "_dust");
        registerSimpleItem("crushed_" + oreName + "_ore");
        registerSimpleItem("refined_" + oreName + "_ore");
        registerSimpleItem("purified_" + oreName + "_ore");
        registerSimpleItem("pure_" + oreName + "_dust");
        registerSimpleItem("impure_" + oreName + "_dust");
    }

    /** Registry path of the ore sample belonging to the {@code <base>_<ore>} ore block. */
    public static String oreSampleId(BlockSubLayer base, String oreName) {
        return base.id() + "_" + oreName + "_ore_sample";
    }

    /** {@code <ore>_ore_in_<layer>}, e.g. {@code hematite_ore_in_stone}; the same path for the block and item tag. */
    public static String oreInLayerTagPath(OreDefinition def, BlockSubLayer base) {
        return def.id() + "_ore_in_" + base.id();
    }

    public static TagKey<Block> oreInLayerBlockTag(OreDefinition def, BlockSubLayer base) {
        return TagKey.create(Registries.BLOCK, RollMod.id(oreInLayerTagPath(def, base)));
    }

    public static TagKey<Item> oreInLayerItemTag(OreDefinition def, BlockSubLayer base) {
        return TagKey.create(Registries.ITEM, RollMod.id(oreInLayerTagPath(def, base)));
    }

    /** Block ids in {@link #oreInLayerBlockTag}: the ore in the layer's own rock, then in each rock of that layer. */
    public static List<String> oreInLayerIds(OreDefinition def, BlockSubLayer base) {
        List<String> ids = new ArrayList<>();
        ids.add(base.id() + "_" + def.id());
        for (StoneVariant variant : def.variants()) {
            if (variant.parent() == base) ids.add(def.variantBlockId(variant));
        }
        return ids;
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
