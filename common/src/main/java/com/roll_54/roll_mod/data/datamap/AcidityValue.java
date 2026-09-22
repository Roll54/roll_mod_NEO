package com.roll_54.roll_mod.data.datamap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A value of the {@code roll_mod:acidity} data map.
 *
 * <p>{@code value} is signed and expressed in acidity units, the same unit the Hydroponic Garden
 * Bed's acidity buffer is measured in: <b>positive means acid</b> (sulfur, sulfuric acid),
 * <b>negative means base</b> (sodium hydroxide, lime). For items it is the amount contributed by a
 * single item; for fluids it is the amount contributed by a full bucket (1000 mB), scaled linearly.
 *
 * <p>Both the long and the short JSON form are accepted:
 * <pre>
 * "roll_mod:sulfur": 250
 * "roll_mod:sulfur": { "value": 250 }
 * </pre>
 */
public record AcidityValue(int value) {

    public static final Codec<AcidityValue> CODEC = Codec.withAlternative(
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("value").forGetter(AcidityValue::value)
            ).apply(instance, AcidityValue::new)),
            Codec.INT.xmap(AcidityValue::new, AcidityValue::value)
    );
}
