package com.roll_54.roll_mod_server.commands;

import com.mojang.brigadier.CommandDispatcher;
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
 * <p>{@code /rollmod admin git_copy --force} does the same copy but reloads immediately,
 * skipping the grace period and the warning title.
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
                                        .executes(ctx -> execute(ctx, false))
                                        .then(Commands.literal("--force")
                                                .executes(ctx -> execute(ctx, true))
                                        )
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

    private static int execute(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, boolean force) {
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

        boolean started = GitScriptUpdater.run(server, player, force);
        if (!started) {
            source.sendFailure(Component.translatable("message.roll_mod.git.busy")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        return 1;
    }
}
