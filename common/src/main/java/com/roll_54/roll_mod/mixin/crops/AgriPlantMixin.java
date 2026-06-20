package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.plant.AgriPlant;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.compat.argicraft.WeedResistant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds an optional {@code weeds_resistance} boolean to the AgriCraft plant datapack format.
 *
 * <p>{@link AgriPlant} is built with a fixed-arity {@code RecordCodecBuilder}, so a field cannot be
 * appended to it directly. Instead we wrap the existing {@link AgriPlant#CODEC} at the tail of the
 * class initializer: the original codec still decodes every known field (it ignores the extra one),
 * while the wrapper additionally reads {@code weeds_resistance} (defaulting to {@code false}) and
 * stores it on the decoded plant through {@link WeedResistant}.
 */
@Mixin(AgriPlant.class)
public abstract class AgriPlantMixin implements WeedResistant {

    @Shadow
    @Final
    @Mutable
    public static Codec<AgriPlant> CODEC;

    @Unique
    private boolean roll_mod$weedsResistance = false;

    @Override
    public boolean roll_mod$isWeedsResistant() {
        return this.roll_mod$weedsResistance;
    }

    @Override
    public void roll_mod$setWeedsResistant(boolean weedsResistant) {
        this.roll_mod$weedsResistance = weedsResistant;
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void roll_mod$wrapCodecWithWeedsResistance(CallbackInfo ci) {
        Codec<AgriPlant> original = CODEC;
        MapCodec<AgriPlant> base = MapCodec.assumeMapUnsafe(original);
        MapCodec<Boolean> flag = Codec.BOOL.optionalFieldOf("weeds_resistance", false);
        CODEC = Codec.mapPair(base, flag).codec().xmap(
                pair -> {
                    AgriPlant plant = pair.getFirst();
                    ((WeedResistant) (Object) plant).roll_mod$setWeedsResistant(pair.getSecond());
                    return plant;
                },
                plant -> Pair.of(plant, ((WeedResistant) (Object) plant).roll_mod$isWeedsResistant())
        );
    }
}
