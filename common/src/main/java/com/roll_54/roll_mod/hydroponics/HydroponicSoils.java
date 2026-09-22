package com.roll_54.roll_mod.hydroponics;

import com.agricraft.agricraft.api.codecs.AgriSoil;
import com.agricraft.agricraft.api.codecs.AgriSoilCondition;
import com.agricraft.agricraft.api.codecs.AgriSoilVariant;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.HydroponicGardenBedBlock;
import com.roll_54.roll_mod.blocks.entity.HydroponicGardenBedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The {@link AgriSoil} instances the Hydroponic Garden Bed reports to AgriCraft.
 *
 * <p>AgriCraft resolves soil from the blockstate alone, so a configurable soil cannot be expressed
 * as datapack entries without one blockstate per setting combination. Instead
 * {@code AgriApiSoilMixin} hands these objects straight to AgriCraft.
 *
 * <p>Everything here is prebuilt and the whole table is only 7 x 6 x 6 + 1 entries. That matters:
 * {@code AgriApi.getSoil} sits on the crop growth path <i>and</i> on {@code CropBlock#canSurvive},
 * so {@link #soilAt} must not allocate — which is why the cache holds the {@link Optional}s
 * themselves rather than the soils.
 */
public final class HydroponicSoils {

    /**
     * What a bed reports when it is switched off, unpowered or starved: plantable, but no help.
     *
     * <p>This is deliberately never {@code Optional.empty()}. {@code CropBlock#canSurvive} is just
     * {@code getSoil(below).isPresent()}, so reporting "not a soil" the moment power drops would pop
     * every crop off the farm.
     */
    public static final Optional<AgriSoil> INERT = Optional.of(build(
            AgriSoilCondition.Humidity.ARID,
            AgriSoilCondition.Acidity.NEUTRAL,
            AgriSoilCondition.Nutrients.NONE,
            1.0D));

    private static final Optional<AgriSoil>[][][] SUPPLIED = buildTable();

    private HydroponicSoils() {
    }

    /**
     * The soil AgriCraft should see at {@code pos}, or {@code null} if that is not one of our beds
     * and AgriCraft should carry on with its own registry lookup.
     */
    @Nullable
    public static Optional<AgriSoil> soilAt(BlockGetter getter, BlockPos pos) {
        // Cheap blockstate test first: this runs for every crop growth tick and every canSurvive
        // check in the world, and a block entity lookup is far more expensive than a state read.
        if (!(getter.getBlockState(pos).getBlock() instanceof HydroponicGardenBedBlock)) {
            return null;
        }
        BlockEntity blockEntity = getter.getBlockEntity(pos);
        if (!(blockEntity instanceof HydroponicGardenBedBlockEntity bed)) {
            // Legitimately happens on render-region BlockGetters and during chunk load. Never
            // report "no soil" for our own block, or every crop above it would pop off.
            return INERT;
        }
        if (!bed.isSupplied()) {
            return INERT;
        }
        HydroponicSettings settings = bed.settings();
        return SUPPLIED[settings.ph() - 1][settings.water() - 1][settings.fertility() - 1];
    }

    @SuppressWarnings("unchecked")
    private static Optional<AgriSoil>[][][] buildTable() {
        Optional<AgriSoil>[][][] table = new Optional[HydroponicSettings.PH_MAX]
                [HydroponicSettings.WATER_MAX][HydroponicSettings.FERTILITY_MAX];
        for (int ph = 0; ph < HydroponicSettings.PH_MAX; ph++) {
            for (int water = 0; water < HydroponicSettings.WATER_MAX; water++) {
                for (int fertility = 0; fertility < HydroponicSettings.FERTILITY_MAX; fertility++) {
                    table[ph][water][fertility] = Optional.of(build(
                            AgriSoilCondition.Humidity.values()[water],
                            AgriSoilCondition.Acidity.values()[ph],
                            AgriSoilCondition.Nutrients.values()[fertility],
                            HydroponicSettings.GROWTH_MODIFIER));
                }
            }
        }
        return table;
    }

    private static AgriSoil build(AgriSoilCondition.Humidity humidity,
                                  AgriSoilCondition.Acidity acidity,
                                  AgriSoilCondition.Nutrients nutrients,
                                  double growthModifier) {
        // All three conditions must be set explicitly: AgriSoil.Builder defaults them to INVALID,
        // which every growth-condition check reads as barren.
        return AgriSoil.builder()
                .humidity(humidity)
                .acidity(acidity)
                .nutrients(nutrients)
                .growthModifier(growthModifier)
                // Kept truthful so AgriSoil#isVariant still answers correctly for anything that
                // inspects one of these outside of the getSoil path.
                .variants(new AgriSoilVariant(
                        new ExtraCodecs.TagOrElementLocation(RollMod.id("hydroponic_garden_bed"), false),
                        List.of()))
                .build();
    }
}
