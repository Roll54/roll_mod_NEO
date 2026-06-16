package com.roll_54.roll_mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.util.GitScriptUpdater;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /rollmod admin git_copy} — pulls the {@code server_scripts} folder from the
 * configured GitHub repo into {@code kubejs/server_scripts}, then schedules a reload.
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
                                        .executes(GitCopyCommand::execute)
                                )
                        )
        );
    }

    /** True only for an online {@link ServerPlayer} whose name is in the allow-list. */
    private static boolean isAllowedPlayer(CommandSourceStack src) {
        return src.getEntity() instanceof ServerPlayer sp && isAllowedName(sp.getGameProfile().getName());
    }

    private static boolean isAllowedName(String name) {
        for (String allowed : MyConfig.INSTANCE.gitUpdater.allowedPlayers) {
            if (allowed.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static int execute(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        // Defensive re-check (the .requires gate already blocks console/command blocks).
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.translatable("message.roll_mod.git.no_permission")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!isAllowedName(player.getGameProfile().getName())) {
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

        boolean started = GitScriptUpdater.run(server, player);
        if (!started) {
            source.sendFailure(Component.translatable("message.roll_mod.git.busy")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        return 1;
    }
}
