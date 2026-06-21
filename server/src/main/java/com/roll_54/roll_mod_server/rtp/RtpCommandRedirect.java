package com.roll_54.roll_mod_server.rtp;

import com.mojang.brigadier.context.ParsedCommandNode;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Restricts {@code /rtp} (FTB Essentials' random teleport) to the dimensions listed in
 * {@link MyConfig.RtpSettings#allowedDimensions}. When a player runs {@code /rtp} from a dimension that
 * is not allowed, they are first teleported to the overworld and the random teleport is then re-run there,
 * so the player still ends up at a random overworld location rather than randomly spread through a
 * dimension where rtp is undesirable (the Nether, a custom dungeon dimension, etc.).
 *
 * <p>Implemented as a {@link CommandEvent} interceptor rather than a Mixin into FTB Essentials so it needs
 * no compile/runtime dependency on FTB and survives FTB version bumps — it only keys off the top-level
 * command literal ({@code "rtp"}) at the stable NeoForge command-dispatch layer, mirroring
 * {@code SpawnCommandRedirect}.
 *
 * <p>Only the bare self-targeted {@code /rtp} is handled; any variant with extra arguments is left to the
 * original command.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public final class RtpCommandRedirect {

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        List<? extends ParsedCommandNode<CommandSourceStack>> nodes =
                event.getParseResults().getContext().getNodes();
        // Match exactly `/rtp` with no further arguments; let `/rtp <player>` etc. fall through.
        if (nodes.size() != 1 || !nodes.get(0).getNode().getName().equals("rtp")) return;

        CommandSourceStack source = event.getParseResults().getContext().getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return; // console / command block — leave the original command alone

        Set<ResourceLocation> allowed = allowedDimensions();
        ResourceLocation current = player.level().dimension().location();
        if (allowed.contains(current)) return; // already in an allowed dimension — let FTB handle it

        // Not allowed here: send the player to the overworld and run the random teleport there instead.
        MinecraftServer server = source.getServer();
        ServerLevel overworld = server.overworld();

        event.setCanceled(true);

        BlockPos spawn = overworld.getSharedSpawnPos();
        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                overworld.getSharedSpawnAngle(), 0.0F);

        // Re-dispatch /rtp now that the player is in the overworld so FTB performs the spread there.
        // Guarded so a misconfigured list (overworld itself not allowed) can't cause infinite recursion.
        if (allowed.contains(Level.OVERWORLD.location())) {
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "rtp");
        } else {
            source.sendSuccess(() -> Component.literal(
                    "/rtp is not allowed in this dimension — moved you to the overworld."), false);
        }
    }

    private static Set<ResourceLocation> allowedDimensions() {
        Set<ResourceLocation> set = new HashSet<>();
        for (String id : MyConfig.INSTANCE.rtp.allowedDimensions) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) set.add(rl);
        }
        return set;
    }
}