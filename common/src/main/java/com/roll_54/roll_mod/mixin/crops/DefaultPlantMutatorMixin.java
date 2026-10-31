package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.codecs.AgriMutation;
import com.agricraft.agricraft.api.genetic.AgriMutationHandler;
import com.roll_54.roll_mod.compat.argicraft.CropBreeding;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Species mutation needs mutagen on both parents ({@link CropBreeding#canMutate}). {@code evaluate} is
 * AgriCraft's mutation roll; without the gate it never rolls, so the child keeps one of the parents'
 * species. A hit is recorded so the cross-breed engine can give the new species default genes.
 *
 * <p>Outside a cross-breed (no context) it is left alone.
 */
@Mixin(AgriMutationHandler.DefaultPlantMutator.class)
public abstract class DefaultPlantMutatorMixin {

    @Inject(method = "evaluate", at = @At("HEAD"), cancellable = true)
    private void roll_mod$requireMutagen(AgriMutation mutation, RandomSource random,
                                         CallbackInfoReturnable<Optional<String>> cir) {
        CropBreeding.Context context = CropBreeding.current();
        if (context != null && !context.mutagenOk) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "evaluate", at = @At("RETURN"))
    private void roll_mod$recordMutation(AgriMutation mutation, RandomSource random,
                                         CallbackInfoReturnable<Optional<String>> cir) {
        CropBreeding.Context context = CropBreeding.current();
        if (context != null) {
            cir.getReturnValue().ifPresent(child -> context.mutatedTo = child);
        }
    }
}
