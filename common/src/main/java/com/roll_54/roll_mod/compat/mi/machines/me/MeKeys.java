package com.roll_54.roll_mod.compat.mi.machines.me;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.fluid.FluidVariant;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.storage.TransferVariant;
import org.jetbrains.annotations.Nullable;

/**
 * AE2 keys to MI's transfer variants and back, components included. AE2 fluid amounts are mB, as are MI's,
 * so amounts need no conversion.
 */
public final class MeKeys {

    private MeKeys() {}

    /** The AE2 key of an MI item or fluid variant, {@code null} for a blank one. */
    public static @Nullable AEKey of(TransferVariant<?> variant) {
        if (variant.isBlank()) return null;
        if (variant instanceof ItemVariant item) return AEItemKey.of(item.toStack());
        if (variant instanceof FluidVariant fluid) return AEFluidKey.of(fluid.toStack(1));
        return null;
    }

    public static ItemVariant item(AEItemKey key) {
        return ItemVariant.of(key.toStack());
    }

    public static FluidVariant fluid(AEFluidKey key) {
        return FluidVariant.of(key.toStack(1));
    }
}
