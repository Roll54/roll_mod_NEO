package com.roll_54.roll_mod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.blocks.regenblock.AbstractRegenBlock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RegenBlockCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("regen")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("get_timer")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(RegenBlockCommands::getTimer)
                        )
                )
        );
    }

    private static int getTimer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        Level world = context.getSource().getLevel();
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        if (block instanceof AbstractRegenBlock regenBlock) {
            int timer = regenBlock.getTimerSeconds();
            context.getSource().sendSuccess(() -> Component.literal("Regen block at " + pos.toShortString() + " has a timer of " + timer + " seconds."), false);
            return timer;
        } else {
            context.getSource().sendFailure(Component.literal("Block at " + pos.toShortString() + " is not a regen block."));
            return 0;
        }
    }
}
