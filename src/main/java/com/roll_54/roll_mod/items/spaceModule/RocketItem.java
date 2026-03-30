package com.roll_54.roll_mod.items.spaceModule;

import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.world.item.Item;

public class RocketItem extends Item {

    private final int tier;

    public RocketItem(Properties properties, int tier) {
        super(properties.component(ComponentsRegistry.ROCKET_TIER.get(), tier));
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }
}

