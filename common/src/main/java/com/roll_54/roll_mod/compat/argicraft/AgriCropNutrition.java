package com.roll_54.roll_mod.compat.argicraft;

/**
 * Mutagen and N/P/K pools mixed into AgriCraft's {@code CropBlockEntity} (see
 * {@code CropBlockEntityMixin}), next to {@link AgriHerbicide}.
 */
public interface AgriCropNutrition {

    /** Current mutagen, after the bleed since it was last touched. */
    int roll_mod$getMutagen();

    void roll_mod$addMutagen(int amount);

    int roll_mod$getNitrogen();

    int roll_mod$getPhosphorus();

    int roll_mod$getPotassium();

    void roll_mod$addNutrients(int nitrogen, int phosphorus, int potassium);

    /** Each pool can pay {@link CropBreedingSettings#NUTRIENT_COST_PER_HARVEST}. */
    boolean roll_mod$isNutrientSupplied();

    /** Pays one harvest's N/P/K. Returns false (and takes nothing) when not supplied. */
    boolean roll_mod$consumeNutrients();
}
