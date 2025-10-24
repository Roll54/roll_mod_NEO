package com.roll_54.roll_mod.mixin;

import net.swedz.extended_industrialization.item.nanosuit.ability.NanoSuitGravichestplateAbility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NanoSuitGravichestplateAbility.class)
public class NanoSuitGravichestplateAbilityMixin {

    /**
     * @author roll_54
     * @reason Changes the default capacity from 16,777,216 to 54,000,000. It's a very fancy number!!!
     */
    @Overwrite
    public long overrideEnergyCapacity() {
        return 54_000_000L;
    }
}