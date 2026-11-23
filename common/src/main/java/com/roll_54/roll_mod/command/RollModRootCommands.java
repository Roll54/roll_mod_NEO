package com.roll_54.roll_mod.command;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.network.packet.OpenConfigPacket;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@code /rollmod admin config} — opens roll_mod's config screen on the player's client (operators only; Fzzy still
 * checks on the server before applying a server setting). Every roll_mod command lives under {@code /rollmod}.
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
}
