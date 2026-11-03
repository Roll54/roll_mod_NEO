package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Since its Registrate registers under {@code roll_mod} ({@link GTRegistrationNamespaceMixin}), GTMOGS's
 * Registrate data provider would join roll_mod's own datagen ({@code :client:runData}): it writes marker
 * blockstates, loot tables and lang into roll_mod's generated resources, and it fails outright on the
 * marker textures, which are {@code gtmogs:} textures. roll_mod keeps the marker assets by hand
 * ({@code assets/roll_mod/blockstates|models/*_marker.json}), so GTMOGS's Registrate generates nothing.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.api.registry.registrate.GTRegistrate")
public abstract class GTRegistrateNoDatagenMixin {

    /** The {@code doDatagen} check before Registrate hooks {@code GatherDataEvent}. */
    @ModifyExpressionValue(method = "registerEventListeners",
            at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/nullness/NonNullSupplier;get()Ljava/lang/Object;"))
    private Object roll_mod$noDatagen(Object doDatagen) {
        return Boolean.FALSE;
    }
}
