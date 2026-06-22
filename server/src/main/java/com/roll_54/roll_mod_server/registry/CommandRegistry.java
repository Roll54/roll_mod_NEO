package com.roll_54.roll_mod_server.registry;

import com.roll_54.roll_mod_server.RollModServer;
import com.roll_54.roll_mod_server.commands.AutoGiveCommands;
import com.roll_54.roll_mod_server.commands.GitCopyCommand;
import com.roll_54.roll_mod_server.commands.NetherstormCommand;
import com.roll_54.roll_mod_server.commands.RegenBlockCommands;
import com.roll_54.roll_mod_server.minestar.PVPCommand;
import com.roll_54.roll_mod_server.minestar.ShopReceiveCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = RollModServer.MODID)
public class CommandRegistry {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        AutoGiveCommands.register(dispatcher);
        RegenBlockCommands.register(dispatcher);
        NetherstormCommand.register(dispatcher);
        GitCopyCommand.register(dispatcher);
        PVPCommand.register(dispatcher);

        // minestar-backed command: only register on a dedicated server, where the minestar mod is present.
        if (FMLEnvironment.dist.isDedicatedServer()) {
            ShopReceiveCommand.register(dispatcher);
        }
    }
}
