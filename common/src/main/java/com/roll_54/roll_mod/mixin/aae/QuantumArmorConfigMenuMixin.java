package com.roll_54.roll_mod.mixin.aae;

import appeng.menu.AEBaseMenu;
import com.roll_54.roll_mod.compat.aae.AaeOxygenCard;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.pedroksl.advanced_ae.common.inventory.QuantumArmorMenuHost;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;
import net.pedroksl.advanced_ae.gui.QuantumArmorConfigMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the ✕ (uninstall) button work for the oxygen tank card. AE2 sends the uninstall request's
 * {@link UpgradeType} to the server as JSON, and Gson only knows enum values that are declared fields
 * -- the card's value is appended at runtime ({@code UpgradeTypeMixin}), so it arrived as null and
 * nothing happened. Our card gets its own argument-free action instead, which runs AAE's own
 * {@code requestUninstall} on the server (card back to the inventory, or dropped when it is full).
 */
@Mixin(value = QuantumArmorConfigMenu.class, remap = false)
public abstract class QuantumArmorConfigMenuMixin extends AEBaseMenu {

    @Unique
    private static final String roll_mod$UNINSTALL_OXYGEN = "roll_mod_uninstall_oxygen_tank";

    private QuantumArmorConfigMenuMixin(MenuType<?> type, int id, Inventory inventory, Object host) {
        super(type, id, inventory, host);
    }

    @Shadow
    public abstract void requestUninstall(UpgradeType type);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void roll_mod$registerOxygenUninstall(int id, Inventory inventory, QuantumArmorMenuHost<?> host,
                                                  CallbackInfo ci) {
        registerClientAction(roll_mod$UNINSTALL_OXYGEN, () -> requestUninstall(AaeOxygenCard.TYPE));
    }

    @Inject(method = "requestUninstall", at = @At("HEAD"), cancellable = true)
    private void roll_mod$sendOxygenUninstall(UpgradeType type, CallbackInfo ci) {
        if (type == AaeOxygenCard.TYPE && isClientSide()) {
            sendClientAction(roll_mod$UNINSTALL_OXYGEN);
            ci.cancel();
        }
    }
}
