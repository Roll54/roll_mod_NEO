package com.roll_54.roll_mod.minestar.perks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.createcybernetics.InstalledCyberwareChecks;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Undoes the airborne mining penalty for a player who is flying, or who has the Aerial Mining
 * Stabilizer implant ({@code roll_mod:bodyupgrade_aerial_mining}).
 *
 * <p>Vanilla divides mining speed by 5 whenever the player is not on the ground — meant to slow
 * down mining mid-jump, but it hits {@code /fly} and the gravi chestplate just as hard, and a
 * hovering player is no less deliberate about their mining than a standing one. The event fires
 * after vanilla has applied its penalty, so multiplying by the same 5 restores the true speed.
 *
 * <p>Without the implant only {@code abilities.flying} — creative-style flight — qualifies. A jump or
 * a fall keeps the vanilla penalty, and elytra gliding does too. With the implant installed and
 * enabled, being off the ground for any reason does not slow mining, whatever the tool. Either
 * reason restores the speed once; they never stack.
 *
 * <p>The event fires on both sides; the client needs the same answer to draw the breaking progress,
 * and Create Cybernetics syncs installed implants to it.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class FlightMiningSpeed {

    /** Vanilla's airborne divisor in {@code Player.getDestroySpeed}. */
    private static final float AIRBORNE_PENALTY = 5.0F;

    private FlightMiningSpeed() {}

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.onGround()) return;
        if (player.getAbilities().flying || hasStabilizer(player)) {
            event.setNewSpeed(event.getNewSpeed() * AIRBORNE_PENALTY);
        }
    }

    private static boolean hasStabilizer(Player player) {
        return ModList.get().isLoaded("createcybernetics")
                && InstalledCyberwareChecks.hasWorking(player, ItemRegistry.BODYUPGRADE_AERIAL_MINING.get());
    }
}
