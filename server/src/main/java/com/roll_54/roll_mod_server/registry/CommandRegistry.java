package com.roll_54.roll_mod_server.registry;

import com.roll_54.roll_mod_server.RollModServer;
import com.roll_54.roll_mod_server.commands.AutoGiveCommands;
import com.roll_54.roll_mod_server.commands.ChunkFreezeCommand;
import com.roll_54.roll_mod_server.commands.GitCopyCommand;
import com.roll_54.roll_mod_server.commands.NetherstormCommand;
import com.roll_54.roll_mod_server.commands.RegenBlockCommands;
import com.roll_54.roll_mod_server.minestar.PVPCommand;
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
        ChunkFreezeCommand.register(dispatcher);
        GitCopyCommand.register(dispatcher);
        PVPCommand.register(dispatcher);

        // minestar-backed command: only register on a dedicated server, where the minestar mod is present.
        // Optional: only in a build with the Minestar jar, and only when the mod is installed.
        if (FMLEnvironment.dist.isDedicatedServer() && com.roll_54.roll_mod_server.RollModServer.present("ua.com.minestar.Minestar")) {
            try {
                Class.forName("com.roll_54.roll_mod_server.minestar.ShopReceiveCommand")
                        .getMethod("register", com.mojang.brigadier.CommandDispatcher.class).invoke(null, dispatcher);
            } catch (ReflectiveOperationException | LinkageError e) {
                com.roll_54.roll_mod.RollMod.LOGGER.info("[roll_mod_server] /shop receive not available: {}", e.toString());
            }
        }
    }
}
