package com.roll_54.roll_mod.registry;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.commands.AutoGiveCommands;
import com.roll_54.roll_mod.commands.NetherstormCommand;
import com.roll_54.roll_mod.commands.RegenBlockCommands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = RollMod.MODID)
public class CommandRegistry {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        AutoGiveCommands.register(dispatcher);
        RegenBlockCommands.register(dispatcher);
        NetherstormCommand.register(dispatcher);
    }
}
