package com.roll_54.roll_mod.minestar.op;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /rollmod op …} — the command half of {@code minestar/operators.json}.
 *
 * <p>No short alias: vanilla {@code /op} is a different thing that this file overrides, and giving
 * the two the same name would hide which one somebody just used.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class OperatorCommand {

    private OperatorCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rollmod")
                .then(Commands.literal("op")
                        // The console counts as an administrator: it is already past every gate
                        // this file exists to hold, and it is the way back in if nobody is listed.
                        .requires(source -> source.getPlayer() == null
                                || OperatorStore.canManage(source.getPlayer()))
                        .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                        .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("op", BoolArgumentType.bool())
                                                .executes(ctx -> set(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        BoolArgumentType.getBool(ctx, "op"))))))));
    }

    private static int list(CommandSourceStack source) {
        var entries = OperatorStore.entries();
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("msg.roll_mod.op.none"), false);
            return 0;
        }
        for (OperatorEntry entry : entries) {
            source.sendSuccess(() -> Component.translatable(entry.op()
                            ? "msg.roll_mod.op.listOn" : "msg.roll_mod.op.listOff", entry.name()),
                    false);
        }
        return entries.size();
    }

    private static int set(CommandSourceStack source, ServerPlayer target, boolean op)
            throws CommandSyntaxException {
        String name = target.getGameProfile().getName();
        if (!OperatorStore.set(source.getServer(), target.getUUID(), name, op)) {
            source.sendFailure(Component.translatable("msg.roll_mod.op.anchored", name)
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(op
                ? "msg.roll_mod.op.granted" : "msg.roll_mod.op.revoked", name), true);
        OperatorViewers.resync(source.getServer());
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        OperatorStore.reload();
        OperatorStore.applyAll(source.getServer());
        OperatorViewers.resync(source.getServer());
        source.sendSuccess(() -> Component.translatable("msg.roll_mod.op.reloaded",
                OperatorStore.entries().size()), true);
        return 1;
    }
}
