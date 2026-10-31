package com.roll_54.roll_mod.compat.argicraft;

import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.genetic.AgriAllele;
import com.agricraft.agricraft.api.genetic.AgriGene;
import com.agricraft.agricraft.api.genetic.AgriGenePair;
import com.agricraft.agricraft.api.genetic.AgriGenome;
import com.roll_54.roll_mod.mixin.crops.AgriGenomeAccessor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * Gene ceilings and breeding gates (crops wiki §11.7).
 *
 * <p>A cross-breed's child alleles may only climb to the <em>ceiling</em> both parents qualify for:
 * {@value CropBreedingSettings#CEILING_DEFAULT} by default, {@value CropBreedingSettings#CEILING_HERBICIDE}
 * with herbicide, {@value CropBreedingSettings#CEILING_NUTRIENTS} with N/P/K on top, and
 * {@value CropBreedingSettings#CEILING_MUTAGEN} with mutagen on top of that between two crops of the same
 * species. A species mutation needs mutagen on both parents and gives the child default genes.
 *
 * <p>AgriCraft only has both parent crops in scope in {@code AgriCrossBreedEngine.doCombine}; the
 * mutators further down only see genomes. {@code AgriCrossBreedEngineMixin} therefore opens a
 * {@link Context} there, which {@code DefaultPlantMutatorMixin} reads and marks.
 */
public final class CropBreeding {

    /** One cross-breed in progress. */
    public static final class Context {
        public final boolean mutagenOk;
        public final int ceiling;
        /** The mutation's child id if the species roll hit, else null. */
        @Nullable
        public String mutatedTo;

        Context(boolean mutagenOk, int ceiling) {
            this.mutagenOk = mutagenOk;
            this.ceiling = ceiling;
        }
    }

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private CropBreeding() {
    }

    public static Context open(AgriCrop parentA, AgriCrop parentB) {
        Context context = new Context(canMutate(parentA, parentB), pairCeiling(parentA, parentB));
        CONTEXT.set(context);
        return context;
    }

    @Nullable
    public static Context current() {
        return CONTEXT.get();
    }

    public static void close() {
        CONTEXT.remove();
    }

    // ── Per-parent qualifications ───────────────────────────────────────

    public static int herbicide(AgriCrop crop) {
        return crop instanceof AgriHerbicide herbicide ? herbicide.roll_mod$getHerbicideAmount() : 0;
    }

    public static int mutagen(AgriCrop crop) {
        return crop instanceof AgriCropNutrition nutrition ? nutrition.roll_mod$getMutagen() : 0;
    }

    public static boolean nutrientSupplied(AgriCrop crop) {
        return crop instanceof AgriCropNutrition nutrition && nutrition.roll_mod$isNutrientSupplied();
    }

    /** The highest ceiling this crop qualifies for as a parent; the pair gets the lower of the two. */
    public static int parentCeiling(AgriCrop crop) {
        if (herbicide(crop) < CropBreedingSettings.HERBICIDE_GATE) {
            return CropBreedingSettings.CEILING_DEFAULT;
        }
        if (!nutrientSupplied(crop)) {
            return CropBreedingSettings.CEILING_HERBICIDE;
        }
        if (mutagen(crop) < CropBreedingSettings.MUTAGEN_GATE) {
            return CropBreedingSettings.CEILING_NUTRIENTS;
        }
        return CropBreedingSettings.CEILING_MUTAGEN;
    }

    /** What keeps {@code crop} below the next ceiling, or null at the top. */
    @Nullable
    public static Component nextCeilingRequirement(AgriCrop crop) {
        return switch (parentCeiling(crop)) {
            case CropBreedingSettings.CEILING_DEFAULT -> Component.translatable(
                    "message.roll_mod.magnifying.ceiling.needs_herbicide", CropBreedingSettings.HERBICIDE_GATE);
            case CropBreedingSettings.CEILING_HERBICIDE -> Component.translatable(
                    "message.roll_mod.magnifying.ceiling.needs_nutrients", CropBreedingSettings.NUTRIENT_COST_PER_HARVEST);
            case CropBreedingSettings.CEILING_NUTRIENTS -> Component.translatable(
                    "message.roll_mod.magnifying.ceiling.needs_mutagen", CropBreedingSettings.MUTAGEN_GATE);
            default -> null;
        };
    }

    public static boolean canMutate(AgriCrop parentA, AgriCrop parentB) {
        return mutagen(parentA) >= CropBreedingSettings.MUTAGEN_GATE
                && mutagen(parentB) >= CropBreedingSettings.MUTAGEN_GATE;
    }

    /** The ceiling two parents give their child, assuming no species mutation. */
    public static int pairCeiling(AgriCrop parentA, AgriCrop parentB) {
        int ceiling = Math.min(parentCeiling(parentA), parentCeiling(parentB));
        if (ceiling > CropBreedingSettings.CEILING_NUTRIENTS
                && !Objects.equals(parentA.getPlantId(), parentB.getPlantId())) {
            // The mutagen ceiling is for refining one species, not for crossing two.
            ceiling = CropBreedingSettings.CEILING_NUTRIENTS;
        }
        return ceiling;
    }

    // ── Genome edits ────────────────────────────────────────────────────

    /**
     * Caps every stat allele of a freshly combined {@code child} at {@code max(ceiling, highest allele
     * either parent has for that stat)}. Drift moves an allele by at most one, so this only removes
     * gains past the ceiling and never lowers what the parents already carried.
     */
    public static void applyCeiling(AgriGenome child, AgriGenome parentA, AgriGenome parentB, int ceiling) {
        Map<String, AgriGenePair<Integer>> stats = ((AgriGenomeAccessor) child).roll_mod$getStats();
        Map<String, AgriGenePair<Integer>> statsA = ((AgriGenomeAccessor) parentA).roll_mod$getStats();
        Map<String, AgriGenePair<Integer>> statsB = ((AgriGenomeAccessor) parentB).roll_mod$getStats();
        stats.replaceAll((id, pair) -> {
            int cap = Math.max(ceiling, Math.max(highest(statsA.get(id)), highest(statsB.get(id))));
            return mapAlleles(pair, value -> Math.min(value, cap));
        });
    }

    /** Drops every stat allele above {@code limit} to it. Returns whether anything changed. */
    public static boolean regress(AgriGenome genome, int limit) {
        Map<String, AgriGenePair<Integer>> stats = ((AgriGenomeAccessor) genome).roll_mod$getStats();
        boolean changed = false;
        for (Map.Entry<String, AgriGenePair<Integer>> entry : stats.entrySet()) {
            if (highest(entry.getValue()) > limit) {
                entry.setValue(mapAlleles(entry.getValue(), value -> Math.min(value, limit)));
                changed = true;
            }
        }
        return changed;
    }

    /** The highest stat allele in the genome, i.e. whether any gene needs N/P/K upkeep. */
    public static int highestAllele(AgriGenome genome) {
        int highest = 0;
        for (AgriGenePair<Integer> pair : genome.getStatGenes()) {
            highest = Math.max(highest, highest(pair));
        }
        return highest;
    }

    private static int highest(@Nullable AgriGenePair<Integer> pair) {
        if (pair == null) {
            return 0;
        }
        return Math.max(pair.getDominant().trait(), pair.getRecessive().trait());
    }

    private static AgriGenePair<Integer> mapAlleles(AgriGenePair<Integer> pair, IntUnaryOperator change) {
        AgriGene<Integer> gene = pair.getGene();
        AgriAllele<Integer> first = gene.getAllele(change.applyAsInt(pair.getDominant().trait()));
        AgriAllele<Integer> second = gene.getAllele(change.applyAsInt(pair.getRecessive().trait()));
        return new AgriGenePair<>(gene, first, second);
    }
}
