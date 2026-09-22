package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.AgriApi;
import com.agricraft.agricraft.api.codecs.AgriSoil;
import com.roll_54.roll_mod.hydroponics.HydroponicSoils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Lets the Hydroponic Garden Bed be a soil whose properties are configurable at runtime.
 *
 * <p>AgriCraft resolves soil by streaming the datapack soil registry and matching
 * {@code AgriSoil#isVariant(BlockState)} — the lookup sees a blockstate and nothing else, so a block
 * whose humidity/acidity/nutrients come from a block entity cannot be expressed as datapack entries
 * (it would take 7 x 6 x 6 blockstates and as many JSON files). Short-circuiting the lookup is the
 * only way to express it.
 *
 * <p>All three public {@code AgriApi#getSoil} overloads are thin wrappers around this one private
 * delegate — each just resolves the registry and tail-calls it — so injecting here covers every
 * caller, including {@code CropBlockEntity#getSoil} and {@code CropBlock#canSurvive}. Injecting at
 * HEAD also puts us in front of AgriCraft's {@code registry.isEmpty()} guard, so the bed keeps
 * working even when the soil registry is unavailable.
 *
 * <p>The descriptor in {@code method} is not optional: {@code AgriApi} declares four methods named
 * {@code getSoil}. Note also that this ties the mod's startup to that private signature —
 * {@code roll_mod.mixins.json} is {@code required} with {@code defaultRequire: 1}, so an AgriCraft
 * rename is a hard crash rather than silent breakage. That is the intended trade: AgriCraft is a
 * hard dependency of this mod, and a bed that silently stopped being soil would eat a player's farm.
 */
@Mixin(value = AgriApi.class, remap = false)
public abstract class AgriApiSoilMixin {

    @Inject(
            method = "getSoil(Ljava/util/Optional;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Ljava/util/Optional;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private static void roll_mod$hydroponicSoil(Optional<Registry<AgriSoil>> registry,
                                                BlockGetter getter,
                                                BlockPos pos,
                                                CallbackInfoReturnable<Optional<AgriSoil>> cir) {
        Optional<AgriSoil> soil = HydroponicSoils.soilAt(getter, pos);
        if (soil != null) {
            cir.setReturnValue(soil);
        }
    }
}
