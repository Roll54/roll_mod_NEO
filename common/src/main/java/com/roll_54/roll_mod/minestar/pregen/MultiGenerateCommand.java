package com.roll_54.roll_mod.minestar.pregen;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.command.CommandUtils;
import net.neoforged.neoforge.server.command.generation.GenerationTask;

/**
 * Replacement for NeoForge's {@code /neoforge generate} subcommand (swapped in by
 * {@code GenerateCommandMixin}) that allows one pregeneration task per dimension at the same time,
 * instead of a single task for the whole server.
 *
 * <p>Uses NeoForge's own {@link GenerationTask}; only the bookkeeping around it changes. Two tasks in
 * the same dimension are still refused: they would share the {@code neoforge_generate_forced} tickets
 * of that level and release each other's chunks.
 *
 * <ul>
 *   <li>{@code start [in <dimension>] <pos> <chunkRadius> [progressBar]} – dimension defaults to the
 *       source's (so {@code /execute in ...} keeps working)</li>
 *   <li>{@code stop [dimension]} – without a dimension stops every running task</li>
 *   <li>{@code status [dimension]} – without a dimension lists every running task</li>
 * </ul>
 */
public final class MultiGenerateCommand {
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.00");
    private static final Map<ResourceKey<Level>, Running> TASKS = new ConcurrentHashMap<>();

    static {
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, e -> stopAll());
    }

    private MultiGenerateCommand() {}

    private record Running(ResourceKey<Level> dimension, GenerationTask task, ServerBossEvent bar) {
        int done() {
            return task.getOkCount() + task.getErrorCount() + task.getSkippedCount();
        }

        double percent() {
            return (double) done() / task.getTotalCount() * 100.0;
        }

        void closeBar() {
            if (bar != null) {
                bar.setVisible(false);
                bar.removeAllPlayers();
            }
        }
    }

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("generate").requires(cs -> cs.hasPermission(4));

        // start <pos> <chunkRadius> [progressBar]                    -> the source's dimension (/execute in works)
        // start in <dimension> <pos> <chunkRadius> [progressBar]     -> explicit dimension
        // A literal "in" is needed: a bare dimension id before <pos> would be ambiguous ("100" is a valid id).
        builder.then(Commands.literal("start")
                .then(Commands.literal("in")
                        .then(Commands.argument("dimension", DimensionArgument.dimension())
                                .then(startArgs(ctx -> DimensionArgument.getDimension(ctx, "dimension")))))
                .then(startArgs(ctx -> ctx.getSource().getLevel())));

        builder.then(Commands.literal("stop")
                .executes(ctx -> stop(ctx.getSource(), null))
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(ctx -> stop(ctx.getSource(), DimensionArgument.getDimension(ctx, "dimension").dimension()))));

        builder.then(Commands.literal("status")
                .executes(ctx -> status(ctx.getSource(), null))
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                        .executes(ctx -> status(ctx.getSource(), DimensionArgument.getDimension(ctx, "dimension").dimension()))));

        builder.then(Commands.literal("help")
                .executes(ctx -> help(ctx.getSource())));

        return builder;
    }

    @FunctionalInterface
    private interface LevelGetter {
        ServerLevel get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> startArgs(LevelGetter level) {
        return Commands.argument("pos", BlockPosArgument.blockPos())
                .then(Commands.argument("chunkRadius", IntegerArgumentType.integer(1, 2500)) // 40000 block radius limit
                        .executes(ctx -> start(ctx, true, level.get(ctx)))
                        .then(Commands.argument("progressBar", BoolArgumentType.bool())
                                .executes(ctx -> start(ctx, BoolArgumentType.getBool(ctx, "progressBar"), level.get(ctx)))));
    }

    private static int start(CommandContext<CommandSourceStack> ctx, boolean progressBar, ServerLevel level) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        BlockPos pos = BlockPosArgument.getSpawnablePos(ctx, "pos");
        int chunkRadius = IntegerArgumentType.getInteger(ctx, "chunkRadius");
        ResourceKey<Level> dim = level.dimension();

        if (TASKS.containsKey(dim)) {
            source.sendFailure(prefix(dim).append(Component.translatableWithFallback("commands.roll_mod.chunkgen.already_running",
                    "Generation is already running in this dimension. Run '/neoforge generate stop %s' first.", dim.location().toString())));
            return 0;
        }

        ChunkPos origin = new ChunkPos(pos);
        GenerationTask task = new GenerationTask(level, origin.x, origin.z, chunkRadius);
        ServerBossEvent bar = null;
        if (progressBar) {
            bar = new ServerBossEvent(prefix(dim).append(CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.progress_bar_title")),
                    BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
            bar.setPlayBossMusic(false);
            bar.setCreateWorldFog(false);
            bar.setDarkenScreen(false);
            if (source.getEntity() instanceof ServerPlayer player) bar.addPlayer(player);
        }

        Running running = new Running(dim, task, bar);
        if (TASKS.putIfAbsent(dim, running) != null) return 0;

        int diameter = chunkRadius * 2 + 1;
        source.sendSuccess(() -> prefix(dim).append(CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.started",
                task.getTotalCount(), diameter, diameter, diameter * 16, diameter * 16)), true);

        task.run(listener(source, running));
        return Command.SINGLE_SUCCESS;
    }

    private static int stop(CommandSourceStack source, ResourceKey<Level> dim) {
        List<Running> targets = select(dim);
        if (targets.isEmpty()) {
            source.sendSuccess(() -> notRunning(dim), false);
            return 0;
        }
        for (Running r : targets) {
            if (!TASKS.remove(r.dimension(), r)) continue;
            r.task().stop();
            r.closeBar();
            int count = r.done();
            int total = r.task().getTotalCount();
            double percent = r.percent();
            source.sendSuccess(() -> prefix(r.dimension()).append(
                    CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.stopped", count, total, percent)), true);
        }
        return targets.size();
    }

    private static int status(CommandSourceStack source, ResourceKey<Level> dim) {
        List<Running> targets = select(dim);
        if (targets.isEmpty()) {
            source.sendSuccess(() -> notRunning(dim), false);
            return 0;
        }
        for (Running r : targets) {
            int count = r.done();
            int total = r.task().getTotalCount();
            double percent = r.percent();
            source.sendSuccess(() -> prefix(r.dimension()).append(
                    CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.status", count, total, percent)), false);
        }
        return targets.size();
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatableWithFallback("commands.roll_mod.chunkgen.help_line",
                "§2/neoforge generate start [in <dimension>] <x> <y> <z> <chunkRadius> [progressBar] §r§f- Generates a square centered on the given position that is chunkRadius * 2 on each side. One generation can run per dimension at the same time.\n"
                        + "§2/neoforge generate stop [dimension] §r§f- Stops the generation in a dimension, or every running generation.\n"
                        + "§2/neoforge generate status [dimension] §r- Displays the progress of the generation in a dimension, or of every running generation.\n"
                        + "§2/neoforge generate help §r- Displays this message."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void stopAll() {
        for (Running r : new ArrayList<>(TASKS.values())) {
            r.task().stop();
            r.closeBar();
        }
        TASKS.clear();
    }

    private static List<Running> select(ResourceKey<Level> dim) {
        if (dim != null) {
            Running r = TASKS.get(dim);
            return r == null ? List.of() : List.of(r);
        }
        List<Running> all = new ArrayList<>(TASKS.values());
        all.sort(Comparator.comparing(r -> r.dimension().location().toString()));
        return all;
    }

    private static MutableComponent notRunning(ResourceKey<Level> dim) {
        if (dim == null) return CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.not_running");
        return prefix(dim).append(CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.not_running"));
    }

    private static MutableComponent prefix(ResourceKey<Level> dim) {
        return Component.literal("[" + dim.location() + "] ").withStyle(ChatFormatting.AQUA);
    }

    private static GenerationTask.Listener listener(CommandSourceStack source, Running running) {
        ResourceKey<Level> dim = running.dimension();
        return new GenerationTask.Listener() {
            @Override
            public void update(int ok, int error, int skipped, int total) {
                ServerBossEvent bar = running.bar();
                if (bar == null) return;
                float fraction = (float) (ok + error + skipped) / total;
                MutableComponent title = prefix(dim)
                        .append(CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.progress_bar_progress", total))
                        .append(Component.literal(PERCENT_FORMAT.format(fraction * 100.0F) + "%").withStyle(ChatFormatting.GOLD));
                if (error > 0) {
                    title.append(CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.progress_bar_errors", error)
                            .withStyle(ChatFormatting.RED));
                }
                bar.setName(title);
                bar.setProgress(fraction);
            }

            @Override
            public void complete(int error) {
                // Called from a background thread; finish on the server thread.
                source.getServer().execute(() -> {
                    if (!TASKS.remove(dim, running)) return; // stopped meanwhile
                    running.closeBar();
                    source.sendSuccess(() -> prefix(dim).append(CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.success")), true);
                    if (error > 0) {
                        source.sendFailure(prefix(dim).append(CommandUtils.makeTranslatableWithFallback("commands.neoforge.chunkgen.error", error)));
                    }
                });
            }
        };
    }
}
