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

    private static <T extends Block> DeferredHolder<Block, T> registerBlockAndItem(String name, Supplier<T> blockSupplier) {
        DeferredHolder<Block, T> blockHolder = ORE_BLOCKS.register(name, blockSupplier);
        // Одночасна реєстрація BlockItem
        ItemRegistry.ITEMS.register(name, () -> new BlockItem(blockHolder.get(), new Item.Properties()));
        return blockHolder;
    }

    private static DeferredHolder<Block, Block> registerSimpleBlock(String name, BlockBehaviour.Properties props) {
        return registerBlockAndItem(name, () -> new Block(props));
    }

    private static DeferredHolder<Item, Item> registerSimpleItem(String name) {
        return ORE_ITEMS.register(name, () -> new Item(new Item.Properties()));
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
    private static void registerOreVariants(String oreName, BlockBehaviour.Properties props, String... dimensions) {
        for (String prefix : dimensions) {
            String id = prefix + "_" + oreName;
            registerSimpleBlock(id, props);
        }
        // Базові предмети
        registerSimpleItem("raw_" + oreName);
        registerSimpleItem("crushed_" + oreName + "_ore");
        registerSimpleItem("impure_" + oreName + "_dust");

        registerSimpleItem("refined_" + oreName + "_ore");
        registerSimpleItem("pure_" + oreName + "_dust");
        
        registerSimpleItem("purified_" + oreName + "_ore");
        registerSimpleItem(oreName + "_dust");
    }

    // Викликати цей метод у FMLCommonSetup або під час реєстрації
    public static void register(IEventBus eventBus) {
        ORE_BLOCKS.register(eventBus);
        ORE_ITEMS.register(eventBus);
    }
    // === Приклади викликів ===

    static {
        registerOreVariants("mica", ORE_PROPERTIES, "stone", "deepslate", "netherrack");
        registerOreVariants("kyanite", ORE_PROPERTIES, "stone", "venus");
        registerOreVariants("hematite", ORE_PROPERTIES, "mars", "moon", "stone", "netherrack", "deepslate", "venus", "mercury", "end");
        registerOreVariants("yellow_limonite", ORE_PROPERTIES, "stone", "netherrack");
        registerOreVariants("biotite", ORE_PROPERTIES, "deepslate", "moon", "netherrack");
        registerOreVariants("magnetite", ORE_PROPERTIES, "mars", "moon", "stone", "netherrack", "deepslate", "venus", "mercury", "end");
        registerOreVariants("garnierite", ORE_PROPERTIES, "stone", "venus");
        registerOreVariants("pentlandite", ORE_PROPERTIES, "mars", "stone");
        registerOreVariants("chalcopyrite", ORE_PROPERTIES, "stone", "netherrack", "moon");
        registerOreVariants("certus_quartz", ORE_PROPERTIES, "netherrack", "moon");
        registerOreVariants("pyrochlore", ORE_PROPERTIES, "stone", "deepslate", "moon");
        registerOreVariants("pyrope", ORE_PROPERTIES, "stone", "end");
        registerOreVariants("apatite", ORE_PROPERTIES, "venus", "moon");
        registerOreVariants("galena", ORE_PROPERTIES, "stone", "mars");
        registerOreVariants("pyrolusite", ORE_PROPERTIES, "end", "mars");
        registerOreVariants("rhodochrosite", ORE_PROPERTIES, "end");
        registerOreVariants("chromite", ORE_PROPERTIES, "end", "moon", "mars");
        registerOreVariants("malachite", ORE_PROPERTIES, "stone", "venus");
        registerOreVariants("fluorite", ORE_PROPERTIES, "stone", "moon", "netherrack");
        registerOreVariants("pyrite", ORE_PROPERTIES, "stone", "mars", "netherrack", "end");
        registerOreVariants("cinnabar", ORE_PROPERTIES, "stone", "netherrack");
        registerOreVariants("peridot", ORE_PROPERTIES, "stone", "deepslate");
        registerOreVariants("sodalite", ORE_PROPERTIES, "stone", "moon");
        registerOreVariants("tetrahedrite", ORE_PROPERTIES, "netherrack", "mars");
        registerOreVariants("stibnite", ORE_PROPERTIES, "netherrack", "venus");
        registerOreVariants("ilmenite", ORE_PROPERTIES, "venus", "stone", "moon");
        registerOreVariants("blue_topaz", ORE_PROPERTIES, "venus", "netherrack");
        registerOreVariants("topaz", ORE_PROPERTIES, "mars", "netherrack");
        registerOreVariants("chalcocite", ORE_PROPERTIES, "deepslate", "netherrack");
        registerOreVariants("bornite", ORE_PROPERTIES, "moon", "netherrack", "end");
        registerOreVariants("sulfur", ORE_PROPERTIES, "netherrack");
        registerOreVariants("sphalerite", ORE_PROPERTIES, "stone", "netherrack");
        registerOreVariants("monazite", ORE_PROPERTIES, "mercury", "netherrack");
        registerOreVariants("ruby", ORE_PROPERTIES, "end", "netherrack", "stone", "deepslate");
        registerOreVariants("redstone", ORE_PROPERTIES, "mars", "netherrack", "stone", "deepslate");
        registerOreVariants("saltpeter", ORE_PROPERTIES, "stone", "netherrack");
        registerOreVariants("emerald", ORE_PROPERTIES, "venus", "netherrack");
        registerOreVariants("beryllium", ORE_PROPERTIES,  "netherrack");
        registerOreVariants("tantalite", ORE_PROPERTIES, "mars");
        registerOreVariants("molybdenite", ORE_PROPERTIES, "moon", "netherrack");
        registerOreVariants("wulfenite", ORE_PROPERTIES, "netherrack");
        registerOreVariants("powellite", ORE_PROPERTIES, "mercury", "netherrack");
        registerOreVariants("molybdenum", ORE_PROPERTIES, "mars", "netherrack");
        registerOreVariants("gold", ORE_PROPERTIES, "stone", "netherrack", "deepslate", "end", "venus");
        registerOreVariants("goethite", ORE_PROPERTIES, "deepslate", "netherrack", "stone");
        registerOreVariants("vanadium_magnetite", ORE_PROPERTIES, "moon", "mercury");
        registerOreVariants("rutile", ORE_PROPERTIES, "moon", "venus");
        registerOreVariants("silver", ORE_PROPERTIES, "stone", "deepslate");
        registerOreVariants("bauxite", ORE_PROPERTIES, "mars", "moon", "stone", "netherrack", "deepslate", "venus", "mercury", "end");
        registerOreVariants("salt", ORE_PROPERTIES, "stone", "deepslate");
        registerOreVariants("rock_salt", ORE_PROPERTIES, "stone", "deepslate");
        registerOreVariants("lepidolite", ORE_PROPERTIES, "stone", "deepslate");
        registerOreVariants("tungstate", ORE_PROPERTIES,  "end");
        registerOreVariants("scheelite", ORE_PROPERTIES, "stone", "deepslate", "end");
        registerOreVariants("lithium", ORE_PROPERTIES,  "end");
        registerOreVariants("sheldonite", ORE_PROPERTIES,  "end", "mars");
        registerOreVariants("pitchblende", ORE_PROPERTIES, "end");
        registerOreVariants("thorium", ORE_PROPERTIES, "end");
        registerOreVariants("uraninite", ORE_PROPERTIES, "end");
        registerOreVariants("lead", ORE_PROPERTIES, "stone", "deepslate", "end");
        registerOreVariants("cassiterite", ORE_PROPERTIES, "stone", "deepslate");
    }
}
