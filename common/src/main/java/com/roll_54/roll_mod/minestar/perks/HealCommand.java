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
 * {@code /heal} and {@code /rollmod heal} — patch yourself up.
 *
 * <p>Gated and cooled down inside {@link HealService}, for the reason spelled out in
 * {@link FlyCommand}: a {@code requires} predicate would hide the command instead of refusing it.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class HealCommand {

    private HealCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rollmod").then(tree()));
        dispatcher.register(tree());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree() {
        return Commands.literal("heal").executes(ctx -> run(ctx.getSource()));
    }

    private static int run(CommandSourceStack source) throws CommandSyntaxException {
        return HealService.heal(source.getPlayerOrException()) ? 1 : 0;
    }
}
