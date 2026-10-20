package com.roll_54.roll_mod.minestar.welcome;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.RMMAttachment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The starting screen's server side: who has it open, and what their answer does.
 *
 * <p>The screen opens by itself only while {@link RMMAttachment#NEW_PLAYER} is {@code false} — see
 * {@link WelcomeEvents} — and otherwise only through {@code /rollmod welcome open}. Accepting sets
 * the attachment to {@code true}; declining, or closing the screen any other way, kicks the player
 * and leaves it {@code false}, so the screen is back on their next login.
 *
 * <p>The kick on close is decided here, from the server's own view of the menu, not from anything
 * the client claims: a client that just closes the screen still sends the container close.
 */
public final class WelcomeService {

    /** Players the screen is open for and who have not answered yet. */
    private static final Set<UUID> OPEN = ConcurrentHashMap.newKeySet();

    private WelcomeService() {}

    public static boolean accepted(ServerPlayer player) {
        return player.getData(RMMAttachment.NEW_PLAYER);
    }

    public static void open(ServerPlayer player) {
        // Re-opening over itself would close the current copy first, and that close reads as a
        // refusal.
        if (OPEN.contains(player.getUUID()) && isWelcome(player.containerMenu)) return;
        OPEN.add(player.getUUID());
        PlayerUIMenuType.openUI(player, WelcomeUI.UI_ID);
    }

    public static void accept(ServerPlayer player) {
        if (!OPEN.remove(player.getUUID())) return;
        player.setData(RMMAttachment.NEW_PLAYER, true);
        RollMod.LOGGER.info("[{}] {} accepted the starting screen", RollMod.MODID, player.getGameProfile().getName());
        // Removed from OPEN first, so this close is not taken for a refusal.
        if (isWelcome(player.containerMenu)) {
            player.closeContainer();
        }
    }

    public static void decline(ServerPlayer player) {
        if (!OPEN.contains(player.getUUID())) return;
        kick(player);
    }

    /** Called when the welcome menu closes on the server; a close without an answer is a refusal. */
    static void onClosed(ServerPlayer player, AbstractContainerMenu menu) {
        if (!isWelcome(menu) || !OPEN.contains(player.getUUID())) return;
        // Deferred: this runs inside the container-close handling, and disconnecting from there
        // would tear the connection down under it.
        player.server.execute(() -> {
            if (!player.hasDisconnected() && OPEN.contains(player.getUUID())) {
                kick(player);
            }
        });
    }

    static void forget(UUID player) {
        OPEN.remove(player);
    }

    private static void kick(ServerPlayer player) {
        OPEN.remove(player.getUUID());
        RollMod.LOGGER.info("[{}] {} declined the starting screen, kicking", RollMod.MODID, player.getGameProfile().getName());
        player.connection.disconnect(Component.translatable("gui.roll_mod.welcome.kicked"));
    }

    private static boolean isWelcome(@Nullable AbstractContainerMenu menu) {
        return menu instanceof ModularUIContainerMenu modular && WelcomeUI.isWelcome(modular.getModularUI());
    }
}
