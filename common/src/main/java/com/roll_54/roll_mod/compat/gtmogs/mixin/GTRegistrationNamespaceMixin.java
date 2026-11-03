package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.roll_54.roll_mod.RollMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * GTMOGS's Registrate registers under roll_mod's namespace, so its blocks and items are
 * {@code roll_mod:overworld_marker} and so on (see {@link GTMOGSNamespaceMixin}). Registrate then finds
 * roll_mod's mod event bus by that id; registry events reach every mod's bus, so that is all it needs.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.common.registry.GTRegistration")
public abstract class GTRegistrationNamespaceMixin {

    @ModifyArg(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lcom/quantumgarbage/gtmogs/api/registry/registrate/GTRegistrate;create(Ljava/lang/String;)Lcom/quantumgarbage/gtmogs/api/registry/registrate/GTRegistrate;"),
            index = 0)
    private static String roll_mod$registrateNamespace(String modId) {
        return RollMod.MODID;
    }
}
