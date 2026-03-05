package com.roll_54.roll_mod.modItems.spaceModule;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CartridgeData(String dimensionRaw, String name, int fuelAmount, int requiredTier) {

    public static final Codec<CartridgeData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("dimension_raw").forGetter(CartridgeData::dimensionRaw),
            Codec.STRING.fieldOf("name").forGetter(CartridgeData::name),
            Codec.INT.fieldOf("fuel_amount").forGetter(CartridgeData::fuelAmount),
            Codec.INT.fieldOf("required_tier").forGetter(CartridgeData::requiredTier)
    ).apply(inst, CartridgeData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CartridgeData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), CartridgeData::dimensionRaw,
            ByteBufCodecs.stringUtf8(256), CartridgeData::name,
            ByteBufCodecs.INT, CartridgeData::fuelAmount,
            ByteBufCodecs.INT, CartridgeData::requiredTier,
            CartridgeData::new
    );
}
