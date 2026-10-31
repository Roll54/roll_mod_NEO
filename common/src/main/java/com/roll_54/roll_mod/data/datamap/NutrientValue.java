package com.roll_54.roll_mod.data.datamap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A value of the {@code roll_mod:npk} data map: the nitrogen, phosphorus and potassium a Crop Manager
 * (MV/HV) gets from one item, or from one bucket (1000 mB) of a fluid, scaled linearly. Every field is
 * optional and defaults to 0:
 * <pre>
 * "minecraft:bone_meal": { "p": 10 }
 * </pre>
 */
public record NutrientValue(int nitrogen, int phosphorus, int potassium) {

    public static final Codec<NutrientValue> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("n", 0).forGetter(NutrientValue::nitrogen),
            Codec.INT.optionalFieldOf("p", 0).forGetter(NutrientValue::phosphorus),
            Codec.INT.optionalFieldOf("k", 0).forGetter(NutrientValue::potassium)
    ).apply(instance, NutrientValue::new));

    public boolean isEmpty() {
        return nitrogen <= 0 && phosphorus <= 0 && potassium <= 0;
    }
}
