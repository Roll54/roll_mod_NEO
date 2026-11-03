package com.roll_54.roll_mod.gtmogs.data.misc;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.roll_54.roll_mod.gtmogs.api.worldgen.AdAstraDimensions;
import com.roll_54.roll_mod.gtmogs.api.worldgen.DimensionMarker;
import com.roll_54.roll_mod.gtmogs.api.worldgen.RollModDimensions;
import com.roll_54.roll_mod.gtmogs.utils.FormattingUtil;
import com.tterrag.registrate.util.entry.BlockEntry;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static com.roll_54.roll_mod.gtmogs.common.registry.GTRegistration.REGISTRATE;

@SuppressWarnings("unused")
public class GTDimensionMarkers {

    static {
        REGISTRATE.creativeModeTab(null);
    }

    public static final BlockEntry<Block> OVERWORLD_MARKER = createMarker("overworld");
    public static final BlockEntry<Block> NETHER_MARKER = createMarker("the_nether");
    public static final BlockEntry<Block> END_MARKER = createMarker("the_end");

    public static final BlockEntry<Block> MOON_MARKER = createMarker("moon");
    public static final BlockEntry<Block> MARS_STONE_MARKER = createMarker("mars");
    public static final BlockEntry<Block> MERCURY_STONE_MARKER = createMarker("mercury");
    public static final BlockEntry<Block> VENUS_STONE_MARKER = createMarker("venus");
    public static final BlockEntry<Block> GLACIO_STONE_MARKER = createMarker("glacio");

    public static final BlockEntry<Block> MINING_OVERWORLD_MARKER = createMarker("mining_overworld");

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

    private static BlockEntry<Block> createMarker(String name) {
        return REGISTRATE.block("%s_marker".formatted(name), Block::new)
                .lang(FormattingUtil.toEnglishName(name))
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(), prov.models().cube(ctx.getName(),
                        prov.modLoc("block/dim_markers/%s/down".formatted(name)),
                        prov.modLoc("block/dim_markers/%s/up".formatted(name)),
                        prov.modLoc("block/dim_markers/%s/north".formatted(name)),
                        prov.modLoc("block/dim_markers/%s/south".formatted(name)),
                        prov.modLoc("block/dim_markers/%s/east".formatted(name)),
                        prov.modLoc("block/dim_markers/%s/west".formatted(name)))
                        .texture("particle", "#north")
                        .guiLight(BlockModel.GuiLight.FRONT)))
                .simpleItem()
                .register();
    }

    public static void init() {}
}
