package com.roll_54.roll_mod.gtmogs.api.worldgen.generator.indicators;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import com.roll_54.roll_mod.compat.gtmogs.OreSampleVeins;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.WorldGeneratorUtils;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreBlockPlacer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Scatters decorative indicator blocks on the surface above an ore vein, using vanilla's random-patch
 * algorithm (the one behind {@code minecraft:patch_pumpkin}).
 *
 * <p>
 * Like {@link net.minecraft.world.level.levelgen.feature.RandomPatchFeature}, this makes {@code tries}
 * attempts, each offset from the vein center by {@code nextInt(spread + 1) - nextInt(spread + 1)} on
 * every axis. That triangular distribution clumps positions towards the center instead of spreading
 * them evenly, which is what gives vanilla patches their look. Attempts that do not land on a valid
 * spot are simply discarded, exactly as vanilla discards the ones failing its block predicate.
 *
 * <p>
 * There is no spawn probability: a vein that declares indicators always runs the full patch. Density is
 * controlled purely with {@code tries}, so a definition asking for indicators reliably gets them.
 *
 * <p>
 * Vanilla's origin already sits on the surface (a heightmap placement modifier puts it there), whereas
 * a vein center is underground, so a {@link Attachment#FLOOR} patch in a dimension without a ceiling
 * snaps each attempt's column to the surface heightmap first and applies the vertical offset on top of
 * that. Every other combination has no world surface to aim at — a ceiling dimension (e.g. the Nether)
 * has none at all, and {@link Attachment#CEILING} deliberately wants the roof of whatever cave sits
 * near the vein — so those scan a column near the vein instead, in the attachment's preferred direction
 * first and then the opposite one, for the first spot matching the attachment.
 *
 * <p>
 * {@link Attachment#CEILING} is what marks a vein that generated <em>above</em> the space a player walks
 * through: the sample hangs under the cave roof below the vein, where a floor-attached sample would
 * either land on some unrelated ledge above the vein or find nowhere to sit at all.
 */
@NoArgsConstructor
@Accessors(chain = true, fluent = true)
public class SurfaceIndicatorGenerator extends IndicatorGenerator {

    private static final int MAX_COLUMN_SCAN = 64;

    /** Which face an indicator rests against. */
    public enum Attachment implements StringRepresentable {

        /** Stands on top of a block with a sturdy up-face, as a surface sample does. */
        FLOOR("floor", Direction.UP),
        /** Hangs under a block with a sturdy down-face — a cave roof beneath a vein. */
        CEILING("ceiling", Direction.DOWN),
        /** Either of the above, whichever the column scan reaches first. */
        EITHER("either", Direction.UP);

        public static final Codec<Attachment> CODEC = StringRepresentable.fromEnum(Attachment::values);

        private final String name;
        /** Direction the column scan tries first; the opposite one is the fallback. */
        @Getter
        private final Direction scanDirection;

        Attachment(String name, Direction scanDirection) {
            this.name = name;
            this.scanDirection = scanDirection;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    // spotless:off
    public static final MapCodec<SurfaceIndicatorGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().listOf().fieldOf("blocks").forGetter(gen -> gen.blocks),
            Codec.intRange(1, 4096).optionalFieldOf("tries", 64).forGetter(gen -> gen.tries),
            Codec.intRange(0, 256).optionalFieldOf("xz_spread", 7).forGetter(gen -> gen.xzSpread),
            Codec.intRange(0, 256).optionalFieldOf("y_spread", 3).forGetter(gen -> gen.ySpread),
            Attachment.CODEC.optionalFieldOf("attachment", Attachment.FLOOR).forGetter(gen -> gen.attachment)
    ).apply(instance, SurfaceIndicatorGenerator::new));
    // spotless:on

    @Getter
    private List<Block> blocks = new ArrayList<>();
    /** Attempts per vein, as in vanilla's {@code RandomPatchConfiguration#tries}. Pumpkin patches use 64. */
    @Getter
    @Setter
    private int tries = 64;
    /** Horizontal spread, as in {@code RandomPatchConfiguration#xzSpread}. Pumpkin patches use 7. */
    @Getter
    @Setter
    private int xzSpread = 7;
    /** Vertical spread, as in {@code RandomPatchConfiguration#ySpread}. Pumpkin patches use 3. */
    @Getter
    @Setter
    private int ySpread = 3;
    /** Which face the indicators rest against. Surface samples use {@link Attachment#FLOOR}. */
    @Getter
    @Setter
    private Attachment attachment = Attachment.FLOOR;

    public SurfaceIndicatorGenerator(List<Block> blocks, int tries, int xzSpread, int ySpread) {
        this(blocks, tries, xzSpread, ySpread, Attachment.FLOOR);
    }

    public SurfaceIndicatorGenerator(List<Block> blocks, int tries, int xzSpread, int ySpread, Attachment attachment) {
        this.blocks = new ArrayList<>(blocks);
        this.tries = tries;
        this.xzSpread = xzSpread;
        this.ySpread = ySpread;
        this.attachment = attachment;
    }

    public SurfaceIndicatorGenerator block(Block block) {
        this.blocks.add(block);
        return this;
    }

    public SurfaceIndicatorGenerator blocks(List<Block> blocks) {
        this.blocks = new ArrayList<>(blocks);
        return this;
    }

    @Override
    public MapCodec<? extends IndicatorGenerator> codec() {
        return CODEC;
    }

    @Override
    public Map<BlockPos, OreBlockPlacer> generate(WorldGenLevel level, RandomSource random, OreVeinDefinition entry,
                                                  BlockPos origin) {
        Map<BlockPos, OreBlockPlacer> generated = new Object2ObjectOpenHashMap<>();
        if (blocks.isEmpty())
            return generated;

        // Only a floor patch in a dimension that actually has a surface can aim at the heightmap.
        boolean useSurface = attachment == Attachment.FLOOR && !level.dimensionType().hasCeiling();
        Predicate<BlockPos> isSpot = spotPredicate(level);
        // Vanilla widens both spreads by one before sampling, so a spread of 0 still yields offset 0.
        int xzRange = xzSpread + 1;
        int yRange = ySpread + 1;

        for (int i = 0; i < tries; i++) {
            // Triangular offsets, identical to RandomPatchFeature: two independent draws, subtracted.
            int dx = random.nextInt(xzRange) - random.nextInt(xzRange);
            int dy = random.nextInt(yRange) - random.nextInt(yRange);
            int dz = random.nextInt(xzRange) - random.nextInt(xzRange);

            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;

            BlockPos pos;
            if (useSurface) {
                // Snap to the surface, then apply the vertical offset. Attempts that end up buried or
                // floating fail isSpot below, which is how vanilla thins its patches out.
                pos = new BlockPos(x, groundHeight(level, x, z) + dy, z);
                if (!isSpot.test(pos))
                    continue;
            } else {
                Optional<BlockPos> found = scanColumn(new BlockPos(x, origin.getY() + dy, z), isSpot);
                if (found.isEmpty())
                    continue;
                pos = found.get();
            }

            // Equal weight: pick uniformly at random among the configured blocks.
            BlockState state = blocks.get(random.nextInt(blocks.size())).defaultBlockState();
            generated.put(pos, (access, section) -> placeIndicator(access, pos, state));
        }

        return generated;
    }

    /**
     * roll_mod: the top of the ground, not of everything. {@code WORLD_SURFACE_WG} is the first air above
     * anything at all, so samples ended up floating on lakes and the sea, on tree tops and on tall grass.
     * From there this goes down to the first block with a solid top that is not part of a tree; the
     * sample sits on it, in water if there is water (then placed waterlogged, see {@link #placeIndicator}).
     */
    private static int groundHeight(WorldGenLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        int bottom = level.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top - 1, z);
        while (pos.getY() > bottom) {
            if (isGround(level, pos, level.getBlockState(pos))) return pos.getY() + 1;
            pos.move(Direction.DOWN);
        }
        return top;
    }

    /** A block a sample can stand on: a solid top face, and not a tree, a plant or a fluid. */
    private static boolean isGround(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced()) return false;
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) return false;
        return state.isFaceSturdy(level, pos, Direction.UP);
    }

    private Predicate<BlockPos> spotPredicate(WorldGenLevel level) {
        return switch (attachment) {
            case FLOOR -> pos -> isFloorSpot(level, pos);
            case CEILING -> pos -> isCeilingSpot(level, pos);
            case EITHER -> pos -> isFloorSpot(level, pos) || isCeilingSpot(level, pos);
        };
    }

    /** Scans the attachment's preferred direction first, then the opposite one, for the first valid spot. */
    private Optional<BlockPos> scanColumn(BlockPos start, Predicate<BlockPos> isSpot) {
        Direction primary = attachment.scanDirection();
        Direction fallback = primary.getOpposite();
        return WorldGeneratorUtils.findBlockPos(start, isSpot, pos -> pos.move(primary), MAX_COLUMN_SCAN)
                .or(() -> WorldGeneratorUtils.findBlockPos(start, isSpot, pos -> pos.move(fallback), MAX_COLUMN_SCAN));
    }

    private static boolean isFloorSpot(WorldGenLevel level, BlockPos pos) {
        return isFree(level, pos) && isAnchor(level, pos.below(), Direction.UP);
    }

    private static boolean isCeilingSpot(WorldGenLevel level, BlockPos pos) {
        return isFree(level, pos) && isAnchor(level, pos.above(), Direction.DOWN);
    }

    private static boolean isFree(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    /** True when {@code pos} holds a block an indicator can rest against on its {@code face} side. */
    private static boolean isAnchor(WorldGenLevel level, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.isFaceSturdy(level, pos, face);
    }

    private static void placeIndicator(BulkSectionAccess access, BlockPos pos, BlockState state) {
        // roll_mod: a sample only goes where its ore is straight beneath it, so it always marks real ore.
        // Samples go in after the chunk's ores (OrePlacer.placeOres, then placeIndicators), so the column
        // below is already final.
        if (!OreSampleVeins.hasOreBelow(access, pos, state))
            return;
        LevelChunkSection section = access.getSection(pos);
        if (section == null)
            return;

        int sectionX = SectionPos.sectionRelative(pos.getX());
        int sectionY = SectionPos.sectionRelative(pos.getY());
        int sectionZ = SectionPos.sectionRelative(pos.getZ());

        BlockState current = section.getBlockState(sectionX, sectionY, sectionZ);
        if (!current.isAir() && !current.canBeReplaced())
            return; // never overwrite real blocks

        // roll_mod: water counts as replaceable, so an indicator placed in water deleted the water source;
        // one that can be waterlogged (roll_mod's ore samples can) goes in waterlogged instead.
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && current.getFluidState().getType() == Fluids.WATER) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        section.setBlockState(sectionX, sectionY, sectionZ, state, false);
    }
}
