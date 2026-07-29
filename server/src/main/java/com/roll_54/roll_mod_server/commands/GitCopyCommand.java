package com.roll_54.roll_mod_server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.mixin.CommandSourceStackAccessor;
import com.roll_54.roll_mod_server.util.GitScriptUpdater;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /rollmod admin git_copy} — pulls the {@code server_scripts} folder from the
 * configured GitHub repo into {@code kubejs/server_scripts}, then schedules a reload after
 * the configured delay (warning players first).
 *
 * <p>Sub-commands:
 * <ul>
 *   <li>{@code --force} — same copy, but reload immediately with no grace period or warning.</li>
 *   <li>{@code time <duration>} — schedule the reload after a custom delay (e.g. {@code 30s},
 *       {@code 5m}, {@code 1h}, {@code 1h30m}) instead of the configured default.</li>
 *   <li>{@code no_reload} — copy the scripts but never reload; reload manually afterwards.</li>
 *   <li>{@code message} — don't copy anything; just broadcast the red reload-warning title.</li>
 * </ul>
 *
 * Restricted to the nicknames in {@link MyConfig.GitUpdater#allowedPlayers} (default
 * {@code roll_54}). The server console, command blocks and command-block minecarts cannot
 * run it because they are not {@link ServerPlayer}s.
 */
public class GitCopyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rollmod")
                        .then(Commands.literal("admin")
                                .then(Commands.literal("git_copy")
                                        .requires(GitCopyCommand::isAllowedPlayer)
                                        // Bare command: reload after the configured delay, warning players.
                                        .executes(ctx -> execute(ctx,
                                                GitScriptUpdater.ReloadMode.scheduled(MyConfig.INSTANCE.gitUpdater.reloadDelaySeconds)))
                                        .then(Commands.literal("--force")
                                                .executes(ctx -> execute(ctx, GitScriptUpdater.ReloadMode.immediate())))
                                        .then(Commands.literal("no_reload")
                                                .executes(ctx -> execute(ctx, GitScriptUpdater.ReloadMode.none())))
                                        .then(Commands.literal("time")
                                                .then(Commands.argument("duration", StringArgumentType.word())
                                                        .executes(GitCopyCommand::executeTimed)))
                                        // "message" broadcasts the warning; "massage" is a friendly alias.
                                        .then(Commands.literal("message")
                                                .executes(GitCopyCommand::broadcastWarningOnly))
                                        .then(Commands.literal("massage")
                                                .executes(GitCopyCommand::broadcastWarningOnly))
                                )
                        )
        );
    }

    /**
     * True only when an allow-listed {@link ServerPlayer} issued the command <em>directly</em>.
     *
     * <p>We require the stack's entity to be a player in the allow-list AND the stack's
     * underlying {@link net.minecraft.commands.CommandSource} to be that same player. When a
     * player types a command, the source IS the player; {@code /execute as roll_54 run ...}
     * (from console, an op, or a command block) swaps the entity to roll_54 but keeps the
     * original issuer as the source — so the reference check below rejects impersonation.
     */
    private static boolean isAllowedPlayer(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer sp)) {
            return false;
        }
        Object underlying = ((CommandSourceStackAccessor) (Object) src).roll_mod$getSource();
        if (underlying != sp) {
            return false; // run via /execute as — not roll_54 himself
        }
        return isAllowedName(sp.getGameProfile().getName());
    }

    private static boolean isAllowedName(String name) {
        for (String allowed : MyConfig.INSTANCE.gitUpdater.allowedPlayers) {
            if (allowed.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** {@code time <duration>} — parse the delay, then copy + schedule a reload after it. */
    private static int executeTimed(CommandContext<CommandSourceStack> ctx) {
        String raw = StringArgumentType.getString(ctx, "duration");
        int seconds;
        try {
            seconds = parseDurationSeconds(raw);
        } catch (IllegalArgumentException e) {
            ctx.getSource().sendFailure(Component.translatable("message.roll_mod.git.bad_duration", raw)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        return execute(ctx, GitScriptUpdater.ReloadMode.scheduled(seconds));
    }

    /** {@code message} / {@code massage} — broadcast the red reload warning only. */
    private static int broadcastWarningOnly(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!isAllowedPlayer(source)) {
            source.sendFailure(Component.translatable("message.roll_mod.git.no_permission")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        GitScriptUpdater.broadcastReloadWarning(source.getServer());
        source.sendSuccess(() -> Component.translatable("message.roll_mod.git.message_sent")
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, GitScriptUpdater.ReloadMode reload) {
        CommandSourceStack source = ctx.getSource();

        // Defensive re-check: the .requires gate already blocks console, command blocks and
        // /execute-as impersonation, but re-verify here so the rule holds even if the node is
        // ever reached another way.
        if (!isAllowedPlayer(source)) {
            source.sendFailure(Component.translatable("message.roll_mod.git.no_permission")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.translatable("message.roll_mod.git.no_permission")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (GitScriptUpdater.isBusy()) {
            source.sendFailure(Component.translatable("message.roll_mod.git.busy")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> Component.translatable("message.roll_mod.git.start")
                .withStyle(ChatFormatting.YELLOW), false);

        boolean started = GitScriptUpdater.run(server, player, reload);
        if (!started) {
            source.sendFailure(Component.translatable("message.roll_mod.git.busy")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        return 1;
    }

    /**
     * Parses a duration like {@code "30s"}, {@code "5m"}, {@code "1h"} or a combination such
     * as {@code "1h30m"} into a number of seconds. A bare number is treated as seconds.
     *
     * @throws IllegalArgumentException on empty/invalid input, a non-positive result, or a
     *                                  value that would overflow an {@code int}.
     */
    private static int parseDurationSeconds(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("empty duration");
        }
        String s = input.trim().toLowerCase(java.util.Locale.ROOT);

        // Bare number -> seconds.
        if (s.matches("\\d+")) {
            return checkedInt(Long.parseLong(s));
        }

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)([smh])").matcher(s);
        long totalSeconds = 0;
        int matchedChars = 0;
        while (m.find()) {
            matchedChars += m.group().length();
            long value = Long.parseLong(m.group(1));
            totalSeconds += switch (m.group(2)) {
                case "s" -> value;
                case "m" -> value * 60L;
                case "h" -> value * 3600L;
                default -> 0L;
            };
            if (totalSeconds > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("duration too large: " + input);
            }
        }

        // Reject anything with leftover garbage ("5x", "5m3") or no time at all.
        if (matchedChars != s.length() || totalSeconds == 0) {
            throw new IllegalArgumentException("bad duration format: " + input);
        }
        return checkedInt(totalSeconds);
    }

    private static int checkedInt(long seconds) {
        if (seconds <= 0 || seconds > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("duration out of range");
        }
        return (int) seconds;
    }
}
