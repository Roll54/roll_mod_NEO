package com.roll_54.roll_mod_client.mixin.cyberware;

import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The crop analyzer lens counts as a HUD lens: Create Cybernetics draws its cyberware HUD only for
 * players whose eyes hold a working {@code eyeupgrades_hudlens} (or monovision / hudjack + cybereyes).
 * Here a working crop lens in the eye slot passes that same check.
 */
@Mixin(targets = "com.perigrine3.createcybernetics.screen.custom.hud.CyberwareHudLayer$CyberwareInstallQueries")
public abstract class CropLensHudAccessMixin {

    @Shadow
    private static boolean hasFunctionalSpecificItem(LocalPlayer player, PlayerCyberwareData data, Item item) {
        throw new AssertionError();
    }

    @Inject(method = "hasFunctionalHudLens", at = @At("RETURN"), cancellable = true)
    private static void roll_mod$cropLensIsHudLens(LocalPlayer player, PlayerCyberwareData data,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()
                && hasFunctionalSpecificItem(player, data, ItemRegistry.CROP_ANALYZER_LENSE.get())) {
            cir.setReturnValue(true);
        }
    }
}
