package com.roll_54.roll_mod_server.minestar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class PVPCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("pvp")
                        .executes(PVPCommand::executeToggle)
        );
    }

    private static int executeToggle(CommandContext<CommandSourceStack> context) {
        Entity entity = context.getSource().getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Команда може бути виконана тільки гравцем"));
            return 0;
        }

        PvpManager.TogglePvpResult result = PvpManager.togglePvp(player);
        if (result.success()) {
            PvpManager.sendPvpStatusMessage(player);
        } else {
            context.getSource().sendSystemMessage(
                    Component.literal("Ви не можете змінити режим PVP ще " + result.cooldown() + " секунд(и).")
                            .withStyle(Style.EMPTY.withColor(0xFF5555))
            );
        }

        return 1;
    }
}