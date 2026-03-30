package com.roll_54.roll_mod.data;

import com.mojang.serialization.Codec;

public record UpgradeComponent(String id) {

    public static final Codec<UpgradeComponent> CODEC =
            Codec.STRING.xmap(UpgradeComponent::new, UpgradeComponent::id);

}