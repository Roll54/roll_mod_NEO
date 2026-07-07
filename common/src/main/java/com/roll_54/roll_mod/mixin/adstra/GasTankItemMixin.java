package com.roll_54.roll_mod.mixin.adstra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import earth.terrarium.common_storage_lib.storage.base.StorageIO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Ad Astra's GasTankItem.distributeSequential does not null-check the destination
 * fluid storage it looks up per inventory slot, then passes it into TransferUtil.move,
 * which dereferences it (to.insert(...)) -> crash ("Ticking player").
 *
 * <p>common_storage_lib's FluidApi.ITEM lookup returns null for any item without a fluid
 * capability, so scanning an inventory that holds an ordinary item triggers this.
 *
 * <p>Lives in the common module so it applies to BOTH the integrated server (single player)
 * and the dedicated server; the server module must not also register it (double-wrap).
 * Guard the null destination so distribution just skips that slot.
 * Targeted by string because Ad Astra is not on the common compile classpath.
 */
@Mixin(targets = "earth.terrarium.adastra.common.items.GasTankItem", remap = false)
public class GasTankItemMixin {

    @WrapOperation(
            method = "distributeSequential",
            at = @At(
                    value = "INVOKE",
                    target = "Learth/terrarium/common_storage_lib/storage/util/TransferUtil;" +
                             "move(Learth/terrarium/common_storage_lib/storage/base/StorageIO;" +
                             "Learth/terrarium/common_storage_lib/storage/base/StorageIO;" +
                             "Ljava/lang/Object;JZ)J"
            ),
            remap = false
    )
    private long roll_mod$skipNullDestination(StorageIO<?> from, StorageIO<?> to, Object resource,
                                              long amount, boolean simulate,
                                              Operation<Long> original) {
        if (to == null) {
            return 0L; // no fluid storage on this item -> skip, loop continues to next slot
        }
        return original.call(from, to, resource, amount, simulate);
    }
}
