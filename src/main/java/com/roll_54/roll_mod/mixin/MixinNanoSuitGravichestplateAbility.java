package com.roll_54.roll_mod.mixin;

import net.swedz.extended_industrialization.item.nanosuit.ability.NanoSuitGravichestplateAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NanoSuitGravichestplateAbility.class)
public class MixinNanoSuitGravichestplateAbility {

    /**
     * @author roll_54
     * @reason Змінює дефолтну ємність з 16,777,216 на 54,000,000. Це Дуже красіве число!!!
     */
    @Overwrite
    public long overrideEnergyCapacity() {
        return 54_000_000L;
    }
}