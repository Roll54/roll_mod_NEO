package com.roll_54.roll_mod_server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.roll_54.roll_mod_server.spawn.WorldSpawnState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Adds {@code /setworldspawn <pos> <angle> <dimension>} on top of the vanilla command.
 *
 * <p>Registering the {@code setworldspawn} literal again merges into the existing vanilla node
 * (Brigadier {@code CommandNode#addChild} merges recursively by name), so the vanilla forms
 * ({@code /setworldspawn}, {@code <pos>}, {@code <pos> <angle>}) keep their overworld-only
 * behaviour and we only add the new three-argument branch with the extra dimension.
 *
 * <p>The coordinates are written to the overworld's level data (the single global world-spawn pos
 * that every dimension proxies); the chosen dimension is persisted in {@link WorldSpawnState} and
 * used by {@code SpawnDimHandler} for first-join and bedless respawns.
 */
public class SetWorldSpawnDimCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("setworldspawn")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("angle", AngleArgument.angle())
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .executes(ctx -> setSpawn(
                                                        ctx.getSource(),
                                                        BlockPosArgument.getSpawnablePos(ctx, "pos"),
                                                        AngleArgument.getAngle(ctx, "angle"),
                                                        DimensionArgument.getDimension(ctx, "dimension")
                                                ))
                                        )
                                )
                        )
        );
    }

    private static int setSpawn(CommandSourceStack source, BlockPos pos, float angle, ServerLevel target) {
        MinecraftServer server = source.getServer();

        // World-spawn coordinates live in the overworld's PrimaryLevelData; derived dimensions
        // (incl. spawn_dim) proxy them, so target.getSharedSpawnPos() will return these coords too.
        server.overworld().setDefaultSpawnPos(pos, angle);

        WorldSpawnState state = WorldSpawnState.get(server);
        state.spawnDimension = target.dimension();
        state.setDirty();

        // Flush to disk now instead of waiting for the next autosave / clean /stop. The dimension
        // is marked dirty only once here (unlike StormState, which is re-dirtied every tick and so
        // gets caught by autosaves), so an ungraceful shutdown before the next autosave window would
        // otherwise drop it and silently revert spawn to the overworld on the next boot.
        server.overworld().getDataStorage().save();

        source.sendSuccess(
                () -> Component.translatable("commands.setworldspawn.success", pos.getX(), pos.getY(), pos.getZ(), angle)
                        .append(Component.literal(" in " + target.dimension().location())),
                true
        );
        return 1;
    }
}