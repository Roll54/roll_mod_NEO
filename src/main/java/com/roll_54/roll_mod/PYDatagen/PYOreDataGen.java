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

    /**
     * Реєструє всі варіанти руди + предмети raw/dust
     *
     * Наприклад:
     * registerOreVariants("topaz", ORE_PROPERTIES, "mars", "netherrack");
     *
     * Зареєструє:
     *  - mars_topaz, netherrack_topaz як блоки
     *  - raw_topaz, topaz_dust як предмети
     */

    private static void registerOreVariants(
            String oreName, BlockBehaviour.Properties props, int minXp, int maxXp, String... dimensions) {
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
    // === Приклади викликів ===

    static {
        registerOreVariants("mica", ORE_PROPERTIES, 2, 5, "stone", "deepslate", "netherrack");
        registerOreVariants("kyanite", ORE_PROPERTIES, 2, 5, "stone", "venus");
        registerOreVariants("hematite", ORE_PROPERTIES, 2, 5, "mars", "moon", "stone", "netherrack", "deepslate", "venus", "mercury", "end");
        registerOreVariants("yellow_limonite", ORE_PROPERTIES, 2, 5, "stone", "netherrack", "deepslate");
        registerOreVariants("biotite", ORE_PROPERTIES, 2, 5, "deepslate", "moon", "netherrack");
        registerOreVariants("magnetite", ORE_PROPERTIES, 2, 5, "mars", "moon", "stone", "netherrack", "deepslate", "venus", "mercury", "end");
        registerOreVariants("garnierite", ORE_PROPERTIES, 2, 5, "stone", "venus");
        registerOreVariants("pentlandite", ORE_PROPERTIES, 2, 5, "mars", "stone");
        registerOreVariants("chalcopyrite", ORE_PROPERTIES, 2, 5, "stone", "netherrack", "moon");
        registerOreVariants("certus_quartz", ORE_PROPERTIES, 2, 5, "netherrack", "moon");
        registerOreVariants("pyrochlore", ORE_PROPERTIES, 2, 5, "stone", "deepslate", "moon");
        registerOreVariants("pyrope", ORE_PROPERTIES, 2, 5, "stone", "end");
        registerOreVariants("apatite", ORE_PROPERTIES, 2, 5, "venus", "moon");
        registerOreVariants("galena", ORE_PROPERTIES, 2, 5, "stone", "mars");
        registerOreVariants("pyrolusite", ORE_PROPERTIES, 2, 5, "end", "mars");
        registerOreVariants("rhodochrosite", ORE_PROPERTIES, 2, 5, "end");
        registerOreVariants("chromite", ORE_PROPERTIES, 2, 5, "end", "moon", "mars");
        registerOreVariants("malachite", ORE_PROPERTIES, 2, 5, "stone", "venus", "deepslate", "end");
        registerOreVariants("fluorite", ORE_PROPERTIES, 2, 5, "stone", "moon", "netherrack");
        registerOreVariants("pyrite", ORE_PROPERTIES, 2, 5, "stone", "mars", "netherrack", "end");
        registerOreVariants("cinnabar", ORE_PROPERTIES, 2, 5, "stone", "netherrack");
        registerOreVariants("peridot", ORE_PROPERTIES, 2, 5, "end");
        registerOreVariants("sodalite", ORE_PROPERTIES, 2, 5, "stone", "end");
        registerOreVariants("lazurite", ORE_PROPERTIES, 2, 5, "stone", "end");
        registerOreVariants("lapis_lazuli", ORE_PROPERTIES, 2, 5, "stone", "end");
        registerOreVariants("tetrahedrite", ORE_PROPERTIES, 2, 5, "netherrack", "mars");
        registerOreVariants("stibnite", ORE_PROPERTIES, 2, 5, "netherrack", "venus");
        registerOreVariants("ilmenite", ORE_PROPERTIES, 2, 5, "venus", "stone", "moon");
        registerOreVariants("blue_topaz", ORE_PROPERTIES, 2, 5, "venus", "netherrack");
        registerOreVariants("topaz", ORE_PROPERTIES, 2, 5, "mars", "netherrack");
        registerOreVariants("chalcocite", ORE_PROPERTIES, 2, 5, "deepslate", "netherrack");
        registerOreVariants("bornite", ORE_PROPERTIES, 2, 5, "moon", "netherrack", "end");
        registerOreVariants("sulfur", ORE_PROPERTIES, 2, 5, "netherrack");
        registerOreVariants("sphalerite", ORE_PROPERTIES, 2, 5, "stone", "netherrack");
        registerOreVariants("monazite", ORE_PROPERTIES, 2, 5, "mercury", "netherrack");
        registerOreVariants("ruby", ORE_PROPERTIES, 2, 5, "end", "netherrack", "stone", "deepslate");
        registerOreVariants("redstone", ORE_PROPERTIES, 2, 5, "mars", "netherrack", "stone", "deepslate");
        registerOreVariants("saltpeter", ORE_PROPERTIES, 2, 5, "stone", "netherrack");
        registerOreVariants("emerald", ORE_PROPERTIES, 5, 15, "venus", "netherrack");
        registerOreVariants("beryllium", ORE_PROPERTIES, 2, 5,  "netherrack");
        registerOreVariants("tantalite", ORE_PROPERTIES, 2, 5, "mars");
        registerOreVariants("molybdenite", ORE_PROPERTIES, 2, 5, "moon", "netherrack");
        registerOreVariants("wulfenite", ORE_PROPERTIES, 2, 5, "netherrack");
        registerOreVariants("powellite", ORE_PROPERTIES, 2, 5, "mercury", "netherrack");
        registerOreVariants("molybdenum", ORE_PROPERTIES, 2, 5, "mars", "netherrack");
        registerOreVariants("gold", ORE_PROPERTIES, 2, 5, "stone", "netherrack", "deepslate", "end", "venus");
        registerOreVariants("goethite", ORE_PROPERTIES, 2, 5, "deepslate", "netherrack", "stone");
        registerOreVariants("vanadium_magnetite", ORE_PROPERTIES, 2, 5, "moon", "mercury");
        registerOreVariants("rutile", ORE_PROPERTIES, 2, 5, "moon", "venus");
        registerOreVariants("silver", ORE_PROPERTIES, 2, 5, "stone", "deepslate");
        registerOreVariants("bauxite", ORE_PROPERTIES, 2, 5, "mars", "moon", "stone", "netherrack", "deepslate", "venus", "mercury", "end");
        registerOreVariants("salt", ORE_PROPERTIES, 2, 5, "stone", "deepslate");
        registerOreVariants("rock_salt", ORE_PROPERTIES, 2, 5, "stone", "deepslate");
        registerOreVariants("lepidolite", ORE_PROPERTIES, 2, 5, "stone", "deepslate");
        registerOreVariants("tungstate", ORE_PROPERTIES, 2, 5,  "end");
        registerOreVariants("scheelite", ORE_PROPERTIES, 2, 5, "stone", "deepslate", "end");
        registerOreVariants("lithium", ORE_PROPERTIES, 2, 5,  "end");
        registerOreVariants("sheldonite", ORE_PROPERTIES, 2, 5,  "end", "mars");
        registerOreVariants("pitchblende", ORE_PROPERTIES, 2, 5, "end");
        registerOreVariants("thorium", ORE_PROPERTIES, 2, 5, "end");
        registerOreVariants("uraninite", ORE_PROPERTIES, 2, 5, "end");
        registerOreVariants("lead", ORE_PROPERTIES, 2, 5, "stone", "deepslate", "end");
        registerOreVariants("cassiterite", ORE_PROPERTIES, 2, 5, "stone", "deepslate");
        registerOreVariants("olivine", ORE_PROPERTIES, 2, 5, "end","stone");
        registerOreVariants("trona", ORE_PROPERTIES, 2, 5, "netherrack");
        registerOreVariants("bismuth", ORE_PROPERTIES, 2, 5, "netherrack");
        registerOreVariants("iridium", ORE_PROPERTIES, 2, 5, "mars");
        registerOreVariants("gold_amalgam", ORE_PROPERTIES, 2, 5, "mars", "end");
        registerOreVariants("silver_amalgam", ORE_PROPERTIES, 2, 5, "mars", "end");
        registerOreVariants("coal", ORE_PROPERTIES, 2, 5, "stone", "deepslate");
        registerOreVariants("lignite_coal", ORE_PROPERTIES, 2, 5, "stone", "deepslate");
        registerOreVariants("quartz", ORE_PROPERTIES, 2, 5, "mars", "moon", "stone", "netherrack", "deepslate", "venus", "mercury", "end");
        registerOreVariants("diamond", ORE_PROPERTIES, 10, 50, "deepslate");
        registerOreVariants("bort", ORE_PROPERTIES, 2, 5, "deepslate");
        registerOreVariants("cassiterite_sand", ORE_PROPERTIES, 2, 5, "stone", "deepslate");
        registerOreVariants("azure_silver", ORE_PROPERTIES, 2, 5, "end");


    }
}
