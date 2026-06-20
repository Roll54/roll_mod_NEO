package com.roll_54.roll_mod.compat.argicraft;

/**
 * Implemented (via mixin) by {@link com.agricraft.agricraft.api.plant.AgriPlant} to expose the
 * optional {@code weeds_resistance} flag parsed from the plant datapack JSON.
 *
 * <p>When {@code true}, the crop will never have weeds added to it (weed spawning and spreading are
 * blocked) and the herbicide-applying machines ({@code weed_manager}/{@code crop_manager}) skip it.
 */
public interface WeedResistant {

    boolean roll_mod$isWeedsResistant();

    void roll_mod$setWeedsResistant(boolean weedsResistant);
}
