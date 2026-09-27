package com.roll_54.roll_mod.minestar.data;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.kits.KitCooldowns;
import com.roll_54.roll_mod.minestar.kits.KitStore;
import com.roll_54.roll_mod.minestar.kits.KitViewers;
import com.roll_54.roll_mod.minestar.letters.LetterStore;
import com.roll_54.roll_mod.minestar.letters.LetterViewers;
import com.roll_54.roll_mod.minestar.moderation.BanStore;
import com.roll_54.roll_mod.minestar.moderation.WarnStore;
import com.roll_54.roll_mod.minestar.moderation.WhitelistStore;
import com.roll_54.roll_mod.minestar.op.OperatorStore;
import com.roll_54.roll_mod.minestar.op.OperatorViewers;
import com.roll_54.roll_mod.minestar.tpa.TpaSettings;
import com.roll_54.roll_mod.minestar.tpa.TpaViewers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /rollmod reload} — re-reads everything under {@code minestar/}.
 *
 * <p>Those files are meant to be edited by hand, and an operator who has just fixed a kit should
 * not have to restart the server to see it. Everything is read again from disk and pushed to
 * whoever has the hub open.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ReloadCommand {

    private ReloadCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rollmod")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> reload(ctx.getSource()))));
    }

    private static int reload(CommandSourceStack source) {
        KitStore.reload();
        KitCooldowns.reload();
        MuteStore.reload();
        PlayerPositions.reload();
        TpaSettings.reload();
        OperatorStore.reload();
        WarnStore.reload();
        BanStore.reload();
        WhitelistStore.reload();
        LetterStore.reload();
        LetterViewers.resync(source.getServer());

        // Operators are applied as well as re-read: the file is the authority, so reloading it has
        // to mean the server matches it again, not just that the next login will.
        OperatorStore.applyAll(source.getServer());

        KitViewers.resync(source.getServer());
        OperatorViewers.resync(source.getServer());
        // An edited setting has to reach whoever has the hub open, not just the next request.
        TpaViewers.resync(source.getServer());

        source.sendSuccess(() -> Component.translatable("msg.roll_mod.reloaded"), true);
        return 1;
    }
}
