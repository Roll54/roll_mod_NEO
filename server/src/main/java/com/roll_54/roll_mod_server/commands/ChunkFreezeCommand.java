package com.roll_54.roll_mod_server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.roll_54.roll_mod_server.crash.ChunkFreezer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * {@code /rollmod admin chunkfreeze list | pause [<player>] | unfreeze [<dimension> <chunkX> <chunkZ>] | unfreezeall}
 * — inspect, pause and thaw chunks frozen by {@link ChunkFreezer}. Overload-frozen chunks refreeze on the next scan
 * while they are still over the limit.
 */
public class ChunkFreezeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Gate the unique child, never the shared "rollmod"/"admin" literals (see NetherstormCommand).
        dispatcher.register(Commands.literal("rollmod")
                .then(Commands.literal("admin")
                        .then(Commands.literal("chunkfreeze")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                                .then(Commands.literal("pause")
                                        .executes(ctx -> pause(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> pause(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                                .then(Commands.literal("unfreeze")
                                        .executes(ctx -> {
                                            CommandSourceStack src = ctx.getSource();
                                            return unfreeze(src, src.getLevel(), new ChunkPos(BlockPos.containing(src.getPosition())));
                                        })
                                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                                .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                                                        .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                                                                .executes(ctx -> unfreeze(ctx.getSource(),
                                                                        DimensionArgument.getDimension(ctx, "dimension"),
                                                                        new ChunkPos(IntegerArgumentType.getInteger(ctx, "chunkX"),
                                                                                IntegerArgumentType.getInteger(ctx, "chunkZ"))))))))
                                .then(Commands.literal("unfreezeall")
                                        .executes(ctx -> {
                                            int count = ChunkFreezer.unfreezeAll();
                                            ctx.getSource().sendSuccess(() -> Component.literal("Unfroze " + count + " chunk(s)"), true);
                                            return count;
                                        })))));
    }

    private static int list(CommandSourceStack src) {
        var snapshot = ChunkFreezer.snapshot();
        if (snapshot.isEmpty()) {
            src.sendSuccess(() -> Component.literal("No frozen chunks"), false);
            return 0;
        }
        int[] total = {0};
        long now = System.currentTimeMillis();
        snapshot.forEach((dim, map) -> map.long2ObjectEntrySet().forEach(e -> {
            total[0]++;
            ChunkFreezer.Frozen f = e.getValue();
            long minutes = (now - f.sinceMillis()) / 60_000L;
            src.sendSuccess(() -> ChunkFreezer.chunkLink(dim, new ChunkPos(e.getLongKey()))
                    .append(Component.literal(" " + f.reason().name().toLowerCase() + ", " + minutes + " min: " + f.detail())
                            .withStyle(ChatFormatting.GRAY)), false);
        }));
        return total[0];
    }

    /** Pause the chunk {@code target} is standing in. The player keeps ticking; everything else there stops. */
    private static int pause(CommandSourceStack src, ServerPlayer target) {
        ServerLevel level = target.serverLevel();
        ChunkPos pos = target.chunkPosition();
        if (!ChunkFreezer.freezeManually(level, pos.toLong(), src.getTextName())) {
            src.sendFailure(Component.literal("Chunk " + pos + " in " + level.dimension().location() + " is already frozen"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Paused chunk ").append(ChunkFreezer.chunkLink(level.dimension(), pos))
                .append(Component.literal(" (" + target.getGameProfile().getName() + "'s current chunk)")), true);
        return 1;
    }

    private static int unfreeze(CommandSourceStack src, ServerLevel level, ChunkPos pos) {
        if (!ChunkFreezer.unfreeze(level, pos.toLong())) {
            src.sendFailure(Component.literal("Chunk " + pos + " in " + level.dimension().location() + " is not frozen"));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Unfroze chunk " + pos + " in " + level.dimension().location()), true);
        return 1;
    }
}
