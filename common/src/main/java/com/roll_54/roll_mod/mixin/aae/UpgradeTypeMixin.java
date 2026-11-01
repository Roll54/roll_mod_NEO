package com.roll_54.roll_mod.mixin.aae;

import com.roll_54.roll_mod.compat.aae.AaeOxygenCard;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeSettings;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

/**
 * Appends the oxygen tank card ({@link AaeOxygenCard}) to AAE's {@link UpgradeType} enum, so every
 * piece of AAE that loops over the card types -- data components, the card slot, the install
 * progress, the upgrade list with its on/off switch -- handles it like one of its own.
 *
 * <p>The two methods that {@code switch} over the enum are answered for the new value before the
 * switch, which would otherwise hit its default branch and throw.
 */
@Mixin(value = UpgradeType.class, remap = false)
public abstract class UpgradeTypeMixin {

    @Shadow @Final @Mutable
    private static UpgradeType[] $VALUES;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void roll_mod$addOxygenTankCard(CallbackInfo ci) {
        UpgradeType type = UpgradeTypeInvoker.roll_mod$create(AaeOxygenCard.TYPE_NAME, $VALUES.length,
                AaeOxygenCard.DISPLAY_NAME, null, UpgradeType.SettingType.NONE, AaeOxygenCard.COST,
                UpgradeType.ApplicationType.PASSIVE_USE, AaeOxygenCard.DEFINITION);
        UpgradeType[] values = Arrays.copyOf($VALUES, $VALUES.length + 1);
        values[values.length - 1] = type;
        $VALUES = values;
        AaeOxygenCard.TYPE = type;
    }

    @Inject(method = "getSettings", at = @At("HEAD"), cancellable = true)
    private void roll_mod$oxygenSettings(CallbackInfoReturnable<UpgradeSettings> cir) {
        if ((Object) this == AaeOxygenCard.TYPE) cir.setReturnValue(new UpgradeSettings(1));
    }

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true)
    private void roll_mod$oxygenTooltip(CallbackInfoReturnable<MutableComponent> cir) {
        if ((Object) this == AaeOxygenCard.TYPE) {
            cir.setReturnValue(Component.translatable("tooltip.roll_mod.oxygen_tank_card",
                    com.roll_54.roll_mod.util.NanoSuitUtils.OXYGEN_CARD_MULTIPLIER));
        }
    }
}
