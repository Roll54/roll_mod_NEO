package com.roll_54.roll_mod.minestar.rtp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /rtp} and {@code /rollmod rtp} — a random spot in the world.
 *
 * <p>Both names are registered here, as real commands. The short one used to be FTB Essentials',
 * reached through a {@code CommandEvent} interceptor; that only ever worked because FTB had
 * registered the literal for Brigadier to parse. With FTB gone there is no node to intercept, so
 * the alias has to exist in its own right.
 *
 * <p>Registered from {@code common} rather than the server module's {@code CommandRegistry}, like
 * {@code TierCommand}, so it also exists in the dev client run where it is actually tested.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class RtpCommand {

    private RtpCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rollmod").then(tree()));
        dispatcher.register(tree());
    }

    /**
     * The command itself, built fresh for each registration.
     *
     * <p>Two builds rather than one built node used twice: Brigadier's {@code redirect} forwards
     * the rest of the input to the target, which reads badly under a {@code rollmod} parent, and
     * the tree is one node deep anyway.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("rtp").executes(ctx -> run(ctx.getSource()));
    }

    private static int run(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return RtpService.request(player) ? 1 : 0;
    }
}
