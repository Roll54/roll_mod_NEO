package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerNegotiationEvent;

/**
 * The door: who is turned away, and at which point.
 *
 * <p>Two hooks, because they can reach different things and a ban deserves both.
 *
 * <ol>
 *   <li>{@link PlayerNegotiationEvent} rejects a banned player during the NeoForge handshake —
 *       after the login packet but before they are put in a world, so there is no join message, no
 *       chunk load and nothing to clean up. It runs off the server thread and has only a
 *       {@code GameProfile}, so it may ask {@link BanStore} (concurrent map, synchronized lazy
 *       load, touches nothing else) and nothing more. Two limits worth knowing: it cannot answer
 *       the whitelist question, which needs a {@code ServerPlayer} to test a permission against,
 *       and it only fires for a connection that performs the NeoForge handshake, which is every
 *       real client of this server but not, say, a bare protocol probe.
 *   <li>{@link PlayerEvent.PlayerLoggedInEvent} re-checks the ban and applies the whitelist. This
 *       is the one that catches everything, so it is not merely a nicety: a one-tick join before a
 *       kick is a fair price for being able to ask whether the player holds the bypass. The gate
 *       above is the earlier, tidier path for the case it can see.
 * </ol>
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ModerationEvents {

    private ModerationEvents() {}

    @SubscribeEvent
    public static void onNegotiation(PlayerNegotiationEvent event) {
        BanStore.Ban ban = BanStore.ban(event.getProfile().getId());
        if (ban == null) return;
        RollMod.LOGGER.info("[Moderation] refused {}: banned.", event.getProfile().getName());
        event.getConnection().disconnect(ModerationService.banScreen(ban));
    }

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BanStore.Ban ban = BanStore.ban(player.getUUID());
        if (ban != null) {
            player.connection.disconnect(ModerationService.banScreen(ban));
            return;
        }

        if (WhitelistStore.enabled() && !ModerationPermissions.bypassesWhitelist(player)) {
            RollMod.LOGGER.info("[Moderation] turned away {}: the server is closed.",
                    player.getGameProfile().getName());
            player.connection.disconnect(ModerationService.whitelistScreen());
            return;
        }

        ModerationStatus.sendTo(player);
    }
}
