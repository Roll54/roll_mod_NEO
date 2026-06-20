package com.roll_54.roll_mod.data.attribute;

import net.minecraft.world.entity.ai.attributes.Attribute;

public class SulfurArmorAttribute extends Attribute {
    public double defaultValue;

    public SulfurArmorAttribute(String descriptionId, double defaultValue) {
        super(descriptionId, defaultValue);
        this.defaultValue = defaultValue;
    }

}
