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
 * {@code /spawn} and {@code /rollmod spawn} — the world spawn, not a bed.
 *
 * <p>Registered in its own right rather than intercepted from FTB Essentials, for the reason
 * {@link BackCommand} gives.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class SpawnCommand {

    private SpawnCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rollmod").then(tree()));
        dispatcher.register(tree());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("spawn").executes(ctx -> run(ctx.getSource()));
    }

    private static int run(CommandSourceStack source) throws CommandSyntaxException {
        return TeleportService.spawn(source.getPlayerOrException()) ? 1 : 0;
    }
}
