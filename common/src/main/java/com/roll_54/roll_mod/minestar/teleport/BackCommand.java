package com.roll_54.roll_mod.minestar.teleport;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /back} and {@code /rollmod back} — undo the last thing that moved you.
 *
 * <p>One position, not a history. The trip back is itself a teleport, so it records where you were
 * standing: a second {@code /back} returns you to where the first one started, which is what makes
 * it usable for stepping between two places.
 *
 * <p>Both names are registered here as real commands. The short one used to be FTB Essentials',
 * reached through a {@code CommandEvent} interceptor, and that only ever worked because FTB had
 * registered the literal for Brigadier to parse.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class BackCommand {

    private BackCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rollmod").then(tree()));
        dispatcher.register(tree());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("back").executes(ctx -> run(ctx.getSource()));
    }

    private static int run(CommandSourceStack source) throws CommandSyntaxException {
        return TeleportService.back(source.getPlayerOrException()) ? 1 : 0;
    }
}
