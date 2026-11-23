package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod_client.RollModClient;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * {@code /roll_mod admin config} — opens roll_mod's config screen, what Fzzy's {@code /configure roll_mod} opens.
 *
 * <p>Client-side, because it opens a screen; for operators only. Fzzy still checks on the server that the player
 * may change a server config before it applies anything.
 */
@Deprecated
// No longer registered: a client root "roll_mod" showed only itself in place of the server's commands. The command is
// the server's {@code /rollmod admin config} now ({@code RollModRootCommands}), {@code /roll_mod} its alias.
public final class RollConfigCommand {

    /** Operators (level 2), as for the other admin commands. */
    private static final int PERMISSION_LEVEL = 2;

    private RollConfigCommand() {}

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(RollMod.MODID)
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                        .then(Commands.literal("config").executes(ctx -> open()))));
    }

    private static int open() {
        // After the chat screen has closed, or closing it would close the config screen too.
        Minecraft.getInstance().tell(() -> ConfigApi.INSTANCE.openScreen(RollMod.MODID));
        return 1;
    }
}
