package com.roll_54.roll_mod.command;

import com.mojang.brigadier.tree.CommandNode;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.network.packet.OpenConfigPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@code /rollmod admin config} — opens roll_mod's config screen on the player's client (operators only; Fzzy still
 * checks on the server before applying a server setting); and {@code /roll_mod}, the same tree as {@code /rollmod}
 * under the mod's id, so every roll_mod command shows up whichever way it is typed. Registered last
 * ({@code LOWEST}), when every {@code /rollmod} command is already there.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class RollModRootCommands {

    private RollModRootCommands() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRegisterConfig(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rollmod")
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("config").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PacketDistributor.sendToPlayer(player, OpenConfigPacket.INSTANCE);
                            return 1;
                        }))));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterAlias(RegisterCommandsEvent event) {
        CommandNode<CommandSourceStack> root = event.getDispatcher().getRoot().getChild("rollmod");
        if (root == null) return;
        event.getDispatcher().register(Commands.literal(RollMod.MODID).redirect(root));
    }
}
