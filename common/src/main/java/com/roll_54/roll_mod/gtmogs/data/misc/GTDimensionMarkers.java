package com.roll_54.roll_mod.gtmogs.data.misc;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.roll_54.roll_mod.gtmogs.api.worldgen.AdAstraDimensions;
import com.roll_54.roll_mod.gtmogs.api.worldgen.DimensionMarker;
import com.roll_54.roll_mod.gtmogs.api.worldgen.RollModDimensions;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class GTDimensionMarkers {

    // Plain blocks used only as dimension icons in the recipe viewer's ore vein pages. Not in any creative tab;
    // blockstates, models and names are static files under assets/roll_mod.
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(GTMOGS.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GTMOGS.MOD_ID);

    public static final DeferredBlock<Block> OVERWORLD_MARKER = createMarker("overworld");
    public static final DeferredBlock<Block> NETHER_MARKER = createMarker("the_nether");
    public static final DeferredBlock<Block> END_MARKER = createMarker("the_end");

    public static final DeferredBlock<Block> MOON_MARKER = createMarker("moon");
    public static final DeferredBlock<Block> MARS_STONE_MARKER = createMarker("mars");
    public static final DeferredBlock<Block> MERCURY_STONE_MARKER = createMarker("mercury");
    public static final DeferredBlock<Block> VENUS_STONE_MARKER = createMarker("venus");
    public static final DeferredBlock<Block> GLACIO_STONE_MARKER = createMarker("glacio");

    public static final DeferredBlock<Block> MINING_OVERWORLD_MARKER = createMarker("mining_overworld");

    public static final DimensionMarker OVERWORLD = createAndRegister(Level.OVERWORLD.location(), 0,
            () -> OVERWORLD_MARKER, null);
    public static final DimensionMarker NETHER = createAndRegister(Level.NETHER.location(), 1,
            () -> NETHER_MARKER, null);
    public static final DimensionMarker END = createAndRegister(Level.END.location(), 2,
            () -> END_MARKER, null);

    public static final DimensionMarker MOON = createAndRegister(AdAstraDimensions.DIMENSIONS.get("moon").location(),
            3,
            () -> MOON_MARKER, null);

    public static final DimensionMarker MARS_STONE = createAndRegister(
            AdAstraDimensions.DIMENSIONS.get("mars").location(), 4,
            () -> MARS_STONE_MARKER, null);

    public static final DimensionMarker MERCURY_STONE = createAndRegister(
            AdAstraDimensions.DIMENSIONS.get("mercury").location(), 5,
            () -> MERCURY_STONE_MARKER, null);

    public static final DimensionMarker VENUS_STONE = createAndRegister(
            AdAstraDimensions.DIMENSIONS.get("venus").location(), 6,
            () -> VENUS_STONE_MARKER, null);

    public static final DimensionMarker GLACIO_STONE = createAndRegister(
            AdAstraDimensions.DIMENSIONS.get("glacio").location(), 7,
            () -> GLACIO_STONE_MARKER, null);

    public static final DimensionMarker MINING_OVERWORLD = createAndRegister(
            RollModDimensions.DIMENSIONS.get("mining_overworld").location(), 8,
            () -> MINING_OVERWORLD_MARKER, null);

    public static DimensionMarker createAndRegister(ResourceLocation dim, int tier, ResourceLocation itemKey,
                                                    @Nullable String overrideName) {
        DimensionMarker marker = new DimensionMarker(tier, itemKey, overrideName);
        marker.register(dim);
        return marker;
    }

    public static DimensionMarker createAndRegister(ResourceLocation dim, int tier, Supplier<ItemLike> supplier,
                                                    @Nullable String overrideName) {
        DimensionMarker marker = new DimensionMarker(tier, supplier, overrideName);
        marker.register(dim);
        return marker;
    }

    private static DeferredBlock<Block> createMarker(String name) {
        DeferredBlock<Block> block = BLOCKS.registerSimpleBlock("%s_marker".formatted(name),
                BlockBehaviour.Properties.of());
        ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
