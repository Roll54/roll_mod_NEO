package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod_server.RollModServer;
import com.roll_54.roll_mod.RollMod;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.types.InheritanceNode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

import static ua.com.minestar.Minestar.minestar;

@EventBusSubscriber(modid = RollModServer.MODID, value = Dist.DEDICATED_SERVER)
public class PrimeManager {

    private static final String PRIME_GROUP = "prime";

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        Component nicknamePlayer = player.getName();
        UUID playerUuid = player.getUUID();

        minestar.getUserByProfileUuid(playerUuid)
                .onSuccess(minestarUser -> minestarUser.getPrimeStatus()
                        .ifPresentOrElse(primeStatus -> {
                            RollMod.LOGGER.info("Player {} have prime", nicknamePlayer.getString());
                            setPrimeGroup(playerUuid, true);
                        }, () -> {
                            RollMod.LOGGER.info("Player {} don't have prime", nicknamePlayer.getString());
                            setPrimeGroup(playerUuid, false);
                        }))
                .onFailure(cause -> RollMod.LOGGER.error("Failed to get prime status", cause));
    }

    /**
     * Checks whether the given online player currently inherits the {@code prime} group in LuckPerms.
     * Replaces the former FTBRanks {@code Rank#isActive} check.
     */
    public static boolean hasPrime(ServerPlayer player) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUUID());
        if (user == null) return false;
        return user.getInheritedGroups(user.getQueryOptions()).stream()
                .anyMatch(group -> group.getName().equalsIgnoreCase(PRIME_GROUP));
    }

    private static void setPrimeGroup(UUID playerUuid, boolean hasPrime) {
        LuckPerms luckPerms = LuckPermsProvider.get();
        UserManager userManager = luckPerms.getUserManager();
        InheritanceNode primeNode = InheritanceNode.builder(PRIME_GROUP).build();

        userManager.modifyUser(playerUuid, user -> {
            if (hasPrime) {
                user.data().add(primeNode);
            } else {
                user.data().remove(primeNode);
            }
        });
    }
}