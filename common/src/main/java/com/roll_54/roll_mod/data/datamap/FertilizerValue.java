package com.roll_54.roll_mod.data.datamap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A value of the {@code roll_mod:fertilizer} data map.
 *
 * <p>{@code amount} is the fertilizer contributed, in millibuckets of the Hydroponic Garden Bed's
 * fertilizer buffer. For items it is the amount per single item; for fluids it is the amount per
 * full bucket (1000 mB) of that fluid, scaled linearly — so a dedicated liquid fertilizer would be
 * {@code 1000} and a diluted one less.
 *
 * <p>Both the long and the short JSON form are accepted:
 * <pre>
 * "minecraft:bone_meal": 100
 * "minecraft:bone_meal": { "amount": 100 }
 * </pre>
 */
public record FertilizerValue(int amount) {

    public static final Codec<FertilizerValue> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("amount").forGetter(FertilizerValue::amount)
            ).apply(instance, FertilizerValue::new)),
            Codec.INT.xmap(FertilizerValue::new, FertilizerValue::amount)
    );
}
