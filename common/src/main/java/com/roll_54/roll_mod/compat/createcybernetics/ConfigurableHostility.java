package com.roll_54.roll_mod.compat.createcybernetics;

import net.minecraft.world.entity.Entity;

/**
 * An entity whose "hostile by Minecraft's definition" status — being a {@code Monster} / {@code
 * Enemy} — is decided by config instead of by its class hierarchy.
 *
 * <p>Mixed onto Create Cybernetics' {@code RipperEntity}, which extends {@code AbstractIllager} and
 * so inherits every vanilla consequence of being a monster even though it is a shopkeeper NPC.
 * The vanilla call sites that test {@code instanceof Enemy} ask {@link #isPassive} instead of naming
 * the Ripper, so they never class-load Create Cybernetics and stay safe when the mod is absent.
 */
public interface ConfigurableHostility {

    /** Current answer from config. Read on every call, so a config change applies live. */
    boolean roll_mod$isHostile();

    /** True for an entity that is a {@code Monster} by class but configured not to act like one. */
    static boolean isPassive(Entity entity) {
        return entity instanceof ConfigurableHostility h && !h.roll_mod$isHostile();
    }
}
