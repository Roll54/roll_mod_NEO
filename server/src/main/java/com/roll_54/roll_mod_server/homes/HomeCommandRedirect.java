package com.roll_54.roll_mod_server.homes;

import com.mojang.brigadier.context.ParsedCommandNode;
import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.List;
import java.util.Set;

/**
 * Points {@code /home} and friends at this mod's homes rather than FTB Essentials'.
 *
 * <p>FTB Essentials registers those literals and its home module is enabled on this server. Sharing
 * a name with it is not an option: Brigadier merges same-named roots and the later registration wins
 * the executor, and mod load order is not something we control — so the mod's own commands live
 * under {@code /rollmod} where nothing can collide, and this rewrites the short names onto them.
 *
 * <p>A {@link CommandEvent} interceptor rather than a Mixin into FTB, so it needs no compile or
 * runtime dependency on FTB and survives FTB version bumps: it only keys off the top-level literal
 * at the stable NeoForge dispatch layer, mirroring {@code RtpCommandRedirect} and {@code
 * SpawnCommandRedirect}.
 *
 * <p>Console and command blocks are left alone — an operator script that means FTB's command should
 * still get it, and the hub commands all require a player anyway.
 *
 * <p>Note: homes players had already set through FTB Essentials are not migrated and become
 * unreachable through these names once this is active.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public final class HomeCommandRedirect {

    /** The literals FTB owns that we take over. */
    private static final Set<String> REDIRECTED =
            Set.of("home", "homes", "sethome", "delhome", "invitehome");

    private static final boolean FTBE_LOADED = ModList.get().isLoaded("ftbessentials");

    private HomeCommandRedirect() {}

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        List<? extends ParsedCommandNode<CommandSourceStack>> nodes =
                event.getParseResults().getContext().getNodes();
        if (nodes.isEmpty()) return;

        String root = nodes.get(0).getNode().getName();
        if (!REDIRECTED.contains(root)) return;

        CommandSourceStack source = event.getParseResults().getContext().getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return;

        // Bare /home is the one literal that does not mean "the hub". A single parsed node is the
        // whole of the test: with a name typed the argument node follows the literal.
        if (root.equals("home") && nodes.size() == 1) {
            event.setCanceled(true);
            toLastSeen(player);
            return;
        }

        // The reader holds the command as typed, without the leading slash. Prefixing is enough:
        // every redirected literal exists verbatim under /rollmod, arguments and all.
        String typed = event.getParseResults().getReader().getString();
        event.setCanceled(true);
        // No recursion: the rewritten command's root literal is "rollmod", which this ignores.
        player.server.getCommands()
                .performPrefixedCommand(player.createCommandSourceStack(), "rollmod " + typed);
    }

    /**
     * Sends the player to the position FTB Essentials recorded as their {@code last_seen}.
     *
     * <p>Bare {@code /home} teleports rather than opening the hub, which is what {@code /rollmod
     * home} is still for. The position is FTB's rather than one of ours because FTB is already
     * keeping it for every player on this server, so there is nothing to migrate and nothing to
     * record twice.
     *
     * <p>Worth knowing when reading the coordinates back: FTB writes {@code last_seen} on login and
     * on logout only, never as the player moves and never on a teleport. Within one session it
     * therefore stays pinned to wherever the player logged in.
     */
    private static void toLastSeen(ServerPlayer player) {
        if (!FTBE_LOADED) {
            refuse(player);
            return;
        }
        try {
            if (Ftbe.teleportToLastSeen(player)) return;
        } catch (Throwable ignored) {
            // FTB not ready, or its player data has not loaded yet — same answer as having none.
        }
        refuse(player);
    }

    private static void refuse(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("msg.roll_mod.homes.noLastSeen")
                .withStyle(ChatFormatting.RED));
    }

    /**
     * Isolated holder for the FTB Essentials API; only referenced when {@link #FTBE_LOADED}, the
     * same soft-dependency shape {@code LuckPermsCompat} and {@code RankResolver} use.
     */
    private static final class Ftbe {

        /** {@code false} when the player has no recorded position to go back to. */
        static boolean teleportToLastSeen(ServerPlayer player) {
            return dev.ftb.mods.ftbessentials.util.FTBEPlayerData.getOrCreate(player)
                    .map(data -> {
                        dev.ftb.mods.ftbessentials.util.TeleportPos pos = data.getLastSeenPos();
                        if (pos == null) return false;
                        // runCommand is FTB's own way of finishing a teleport: it reports the
                        // failure to the player itself when the result is not a success.
                        pos.teleport(player).runCommand(player);
                        return true;
                    })
                    .orElse(false);
        }
    }
}
