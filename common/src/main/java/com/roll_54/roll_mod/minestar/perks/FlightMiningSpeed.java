package com.roll_54.roll_mod.minestar.perks;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Undoes the airborne mining penalty for a player who is flying.
 *
 * <p>Vanilla divides mining speed by 5 whenever the player is not on the ground — meant to slow
 * down mining mid-jump, but it hits {@code /fly} and the gravi chestplate just as hard, and a
 * hovering player is no less deliberate about their mining than a standing one. The event fires
 * after vanilla has applied its penalty, so multiplying by the same 5 restores the true speed.
 *
 * <p>Only {@code abilities.flying} — creative-style flight — qualifies. A jump or a fall keeps the
 * vanilla penalty, and elytra gliding does too: nobody aims a drill mid-glide.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class FlightMiningSpeed {

    /** Vanilla's airborne divisor in {@code Player.getDestroySpeed}. */
    private static final float AIRBORNE_PENALTY = 5.0F;

    private FlightMiningSpeed() {}

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player.getAbilities().flying && !player.onGround()) {
            event.setNewSpeed(event.getNewSpeed() * AIRBORNE_PENALTY);
        }
    }
}
