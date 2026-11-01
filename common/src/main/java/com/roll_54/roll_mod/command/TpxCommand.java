package com.roll_54.roll_mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /tpx <dimension>}: teleports whoever runs it into that dimension — overworld, nether, end or
 * any modded one (the argument suggests every loaded level). Operators only (level 2).
 *
 * <p>The player lands at the same spot scaled by the two dimensions' coordinate scale (so x/8 going to
 * the nether, like a portal), on the highest safe floor under the dimension's ceiling: solid ground,
 * two free blocks, no fluid. Where there is none, the dimension's spawn is used (the End's obsidian
 * platform for the End), and as a last resort a glass block is put under the player's feet.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class TpxCommand {

    private TpxCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpx")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(ctx -> teleport(ctx.getSource(), DimensionArgument.getDimension(ctx, "dimension")))));
    }

    private static int teleport(CommandSourceStack source, ServerLevel target) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel from = player.serverLevel();

        double scale = from.dimensionType().coordinateScale() / target.dimensionType().coordinateScale();
        double x = target.getWorldBorder().clampToBounds(player.getX() * scale, player.getY(), player.getZ() * scale).getX();
        double z = target.getWorldBorder().clampToBounds(player.getX() * scale, player.getY(), player.getZ() * scale).getZ();

        Vec3 spot = landing(target, BlockPos.containing(x, 0, z));
        player.teleportTo(target, spot.x, spot.y, spot.z, player.getYRot(), player.getXRot());
        player.fallDistance = 0;

        String name = target.dimension().location().toString();
        source.sendSuccess(() -> Component.translatable("commands.roll_mod.tpx.done", name,
                (int) Math.floor(spot.x), (int) Math.floor(spot.y), (int) Math.floor(spot.z)), false);
        return 1;
    }

    /** Where to put the player in {@code level} around {@code column}: feet position, block-centred. */
    private static Vec3 landing(ServerLevel level, BlockPos column) {
        BlockPos safe = safeFloor(level, column);
        if (safe != null) return Vec3.atBottomCenterOf(safe);

        if (level.dimension() == Level.END) {
            BlockPos platform = ServerLevel.END_SPAWN_POINT;
            EndPlatformFeature.createEndPlatform(level, platform.below(), true);
            return Vec3.atBottomCenterOf(platform);
        }

        BlockPos spawn = level.getSharedSpawnPos();
        BlockPos spawnSafe = safeFloor(level, spawn);
        if (spawnSafe != null) return Vec3.atBottomCenterOf(spawnSafe);

        // Nothing to stand on anywhere near (a void or space dimension): a single block to land on.
        BlockPos floor = new BlockPos(column.getX(), Math.max(level.getSeaLevel(), level.getMinBuildHeight() + 1), column.getZ());
        level.setBlockAndUpdate(floor, Blocks.GLASS.defaultBlockState());
        level.setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(floor.above(2), Blocks.AIR.defaultBlockState());
        return Vec3.atBottomCenterOf(floor.above());
    }

    /**
     * The highest spot in this column, below the dimension's ceiling (the nether roof), with solid
     * ground under it and room for a player: two blocks without collision or fluid.
     */
    private static BlockPos safeFloor(ServerLevel level, BlockPos column) {
        level.getChunk(column.getX() >> 4, column.getZ() >> 4); // generate / load it first
        int top = Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + level.dimensionType().logicalHeight()) - 2;
        top = Math.min(top, level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ()) + 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(column.getX(), top, column.getZ());
        for (; pos.getY() > level.getMinBuildHeight(); pos.move(Direction.DOWN)) {
            if (!free(level, pos) || !free(level, pos.above())) continue;
            BlockPos below = pos.below();
            BlockState ground = level.getBlockState(below);
            if (ground.isFaceSturdy(level, below, Direction.UP) && ground.getFluidState().isEmpty()
                    && !ground.is(Blocks.MAGMA_BLOCK) && !ground.is(Blocks.BEDROCK)) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty()
                && !state.is(Blocks.FIRE) && !state.is(Blocks.SOUL_FIRE);
    }
}
