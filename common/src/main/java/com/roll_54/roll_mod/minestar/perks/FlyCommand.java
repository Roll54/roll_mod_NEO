package com.roll_54.roll_mod.minestar.perks;

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
 * {@code /fly} and {@code /rollmod fly} — flight on and off.
 *
 * <p>No {@code requires} predicate: Brigadier evaluates those when the command tree is sent to the
 * client, so a rank granted mid-session would leave the tree stale, and a player without the rank
 * would be told the command does not exist rather than that it is not theirs. The node is checked in
 * {@link FlyService} instead, where the refusal can be a sentence.
 *
 * <p>Registered from {@code common} rather than the server module, like {@code RtpCommand}, so it
 * also exists in the dev client run where it is actually tested.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class FlyCommand {

    private FlyCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rollmod").then(tree()));
        dispatcher.register(tree());
    }

    /** Built fresh for each registration rather than redirected — the tree is one node deep. */
    private static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("fly").executes(ctx -> run(ctx.getSource()));
    }

    private static int run(CommandSourceStack source) throws CommandSyntaxException {
        return FlyService.toggle(source.getPlayerOrException()) ? 1 : 0;
    }
}
