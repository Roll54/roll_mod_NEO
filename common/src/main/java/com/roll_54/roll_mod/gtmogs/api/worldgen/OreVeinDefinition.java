package com.roll_54.roll_mod.gtmogs.api.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.indicators.IndicatorGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.indicators.SurfaceIndicatorGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins.*;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
@Accessors(chain = true, fluent = true)
public class OreVeinDefinition {

    // spotless:off
    public static final Codec<OreVeinDefinition> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IntProvider.NON_NEGATIVE_CODEC.fieldOf("cluster_size").forGetter(OreVeinDefinition::clusterSize),
            Codec.floatRange(0.0F, 1.0F).fieldOf("density").forGetter(OreVeinDefinition::density),
            Codec.INT.fieldOf("weight").forGetter(OreVeinDefinition::weight),
            IWorldGenLayer.CODEC.fieldOf("layer").forGetter(OreVeinDefinition::layer),
            ResourceKey.codec(Registries.DIMENSION).listOf().fieldOf("dimension_filter").forGetter(ft -> new ArrayList<>(ft.dimensionFilter)),
            HeightRangePlacement.CODEC.fieldOf("height_range").forGetter(OreVeinDefinition::heightRange),
            Codec.floatRange(0.0F, 1.0F).fieldOf("discard_chance_on_air_exposure").forGetter(OreVeinDefinition::discardChanceOnAirExposure),
            RegistryCodecs.homogeneousList(Registries.BIOME).lenientOptionalFieldOf("biomes", HolderSet.empty()).forGetter(OreVeinDefinition::biomes),
            BiomeWeightModifier.CODEC.optionalFieldOf("weight_modifier", BiomeWeightModifier.EMPTY).forGetter(ext -> ext.biomeWeightModifier),
            IndicatorGenerator.DIRECT_CODEC.listOf().optionalFieldOf("indicators", List.of()).forGetter(OreVeinDefinition::indicatorGenerators),
            VeinGenerator.DIRECT_CODEC.fieldOf("generator").forGetter(ft -> ft.veinGenerator),
            Codec.unboundedMap(ResourceKey.codec(Registries.DIMENSION), DimensionOverride.CODEC)
                    .optionalFieldOf("dimension_overrides", Map.of()).forGetter(OreVeinDefinition::dimensionOverrides)
    ).apply(instance, OreVeinDefinition::new));

    public static final Codec<Holder<OreVeinDefinition>> CODEC = RegistryFixedCodec.create(GTRegistries.ORE_VEIN_REGISTRY);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<OreVeinDefinition>> STREAM_CODEC = ByteBufCodecs.holderRegistry(GTRegistries.ORE_VEIN_REGISTRY);
    // spotless:on

    private final InferredProperties inferredProperties = new InferredProperties();

    @Getter
    private IntProvider clusterSize;
    @Getter
    private float density;
    @Getter
    private int weight;
    @Getter
    private IWorldGenLayer layer;
    @Getter
    @Setter
    private Set<ResourceKey<Level>> dimensionFilter;
    @Getter
    private HeightRangePlacement heightRange;
    @Getter
    @Setter
    private float discardChanceOnAirExposure;
    @Getter
    private HolderSet<Biome> biomes;
    @Getter
    @Setter
    private BiomeWeightModifier biomeWeightModifier;

    @Getter
    @Setter
    private List<IndicatorGenerator> indicatorGenerators;

    @Getter
    @Setter
    private VeinGenerator veinGenerator;

    /**
     * roll_mod: per-world replacements for {@link #layer}, {@link #heightRange} and {@link #biomes}, so a vein
     * that generates in several worlds of {@link #dimensionFilter} is one file rather than a copy per world.
     */
    @Getter
    @Setter
    private Map<ResourceKey<Level>, DimensionOverride> dimensionOverrides;

    @ApiStatus.Internal
    @Nullable
    @Setter
    private HolderGetter<Biome> biomeLookup;

    public OreVeinDefinition(OreVeinDefinition other) {
        this(other.clusterSize, other.density, other.weight, other.layer,
                Set.copyOf(other.dimensionFilter), other.heightRange, other.discardChanceOnAirExposure,
                other.biomes, other.biomeWeightModifier, new ArrayList<>(other.indicatorGenerators),
                other.veinGenerator, Map.copyOf(other.dimensionOverrides), other.biomeLookup);
    }

    public OreVeinDefinition(IntProvider clusterSize, float density, int weight, IWorldGenLayer layer,
                             List<ResourceKey<Level>> dimensionFilter, HeightRangePlacement heightRange,
                             float discardChanceOnAirExposure, HolderSet<Biome> biomes,
                             BiomeWeightModifier biomeWeightModifier,
                             List<IndicatorGenerator> indicatorGenerators,
                             @Nullable VeinGenerator veinGenerator,
                             Map<ResourceKey<Level>, DimensionOverride> dimensionOverrides) {
        this(clusterSize, density, weight,
                layer, new HashSet<>(dimensionFilter), heightRange, discardChanceOnAirExposure, biomes,
                biomeWeightModifier, indicatorGenerators,
                veinGenerator, dimensionOverrides, null);
    }

    public OreVeinDefinition(IntProvider clusterSize, float density, int weight, IWorldGenLayer layer,
                             Set<ResourceKey<Level>> dimensionFilter, HeightRangePlacement heightRange,
                             float discardChanceOnAirExposure, HolderSet<Biome> biomes,
                             BiomeWeightModifier biomeWeightModifier,
                             List<IndicatorGenerator> indicatorGenerators,
                             @Nullable VeinGenerator veinGenerator,
                             Map<ResourceKey<Level>, DimensionOverride> dimensionOverrides,
                             @Nullable HolderGetter<Biome> biomeLookup) {
        this.clusterSize = clusterSize;
        this.density = density;
        this.weight = weight;
        this.layer = layer;
        this.dimensionFilter = dimensionFilter;
        this.heightRange = heightRange;
        this.discardChanceOnAirExposure = discardChanceOnAirExposure;
        this.biomes = biomes;
        this.biomeWeightModifier = biomeWeightModifier;
        this.indicatorGenerators = indicatorGenerators != null ? indicatorGenerators : new ArrayList<>();
        this.veinGenerator = veinGenerator;
        this.dimensionOverrides = dimensionOverrides != null ? dimensionOverrides : Map.of();
        this.biomeLookup = biomeLookup;
    }

    /** The layer this vein generates in within {@code level}: its override there, else {@link #layer}. */
    public IWorldGenLayer layer(ResourceKey<Level> level) {
        DimensionOverride override = dimensionOverrides.get(level);
        return override != null && override.layer().isPresent() ? override.layer().get() : layer;
    }

    /** Explicit: Lombok skips this setter because {@link #heightRange(ResourceKey)} shares its name and arity. */
    public OreVeinDefinition heightRange(HeightRangePlacement heightRange) {
        this.heightRange = heightRange;
        return this;
    }

    /** The height range in {@code level}: its override there, else {@link #heightRange}. */
    public HeightRangePlacement heightRange(ResourceKey<Level> level) {
        DimensionOverride override = dimensionOverrides.get(level);
        return override != null && override.heightRange().isPresent() ? override.heightRange().get() : heightRange;
    }

    public boolean isForBiome(ResourceKey<Level> level, Holder<Biome> biome) {
        DimensionOverride override = dimensionOverrides.get(level);
        HolderSet<Biome> set = override != null && override.biomes().isPresent() ? override.biomes().get() : biomes;
        if (set == null) return true;
        return set.size() == 0 || set.contains(biome);
    }

    public int weightForBiome(Holder<Biome> biome) {
        return weight + biomeWeightModifier.applyAsInt(biome);
    }

    public OreVeinDefinition clusterSize(IntProvider clusterSize) {
        this.clusterSize = clusterSize;
        return this;
    }

    public OreVeinDefinition clusterSize(int clusterSize) {
        this.clusterSize = ConstantInt.of(clusterSize);
        return this;
    }

    public OreVeinDefinition density(float density) {
        this.density = density;
        return this;
    }

    public OreVeinDefinition weight(int weight) {
        this.weight = weight;
        return this;
    }

    public OreVeinDefinition layer(IWorldGenLayer layer) {
        this.layer = layer;
        if (this.dimensionFilter == null || this.dimensionFilter.isEmpty()) {
            dimensions(layer.getLevels());
        }
        return this;
    }

    public OreVeinDefinition dimensions(Set<ResourceKey<Level>> dimensions) {
        this.dimensionFilter = dimensions;
        return this;
    }

    public OreVeinDefinition biomes(TagKey<Biome> biomes) {
        if (biomeLookup == null) {
            GTRegistries.builtinRegistry().registry(GTRegistries.ORE_VEIN_REGISTRY)
                    .map(reg -> reg.getKey(this))
                    .ifPresentOrElse(id -> {
                        GTMOGS.LOGGER.error("Tried to modify ore vein `{}`'s biomes after registry has been frozen!",
                                id);
                    }, () -> {
                        GTMOGS.LOGGER.error("Tried to modify an ore vein's biomes after registry has been frozen!");
                    });
            return this;
        }
        this.biomes = biomeLookup.getOrThrow(biomes);
        return this;
    }

    public OreVeinDefinition biomes(HolderSet<Biome> biomes) {
        this.biomes = Objects.requireNonNullElseGet(biomes, HolderSet::empty);
        return this;
    }

    public OreVeinDefinition heightRangeUniform(int min, int max) {
        heightRange(HeightRangePlacement.uniform(VerticalAnchor.absolute(min), VerticalAnchor.absolute(max)));
        inferredProperties.heightRange = IntIntPair.of(min, max);
        return this;
    }

    public OreVeinDefinition heightRangeTriangle(int min, int max) {
        heightRange(HeightRangePlacement.triangle(VerticalAnchor.absolute(min), VerticalAnchor.absolute(max)));
        inferredProperties.heightRange = IntIntPair.of(min, max);
        return this;
    }

    public OreVeinDefinition surfaceIndicator(Consumer<SurfaceIndicatorGenerator> config) {
        var indicator = new SurfaceIndicatorGenerator();

        config.accept(indicator);
        return indicator(indicator);
    }

    public OreVeinDefinition indicator(IndicatorGenerator indicator) {
        if (this.indicatorGenerators == null) {
            this.indicatorGenerators = new ArrayList<>();
        }
        this.indicatorGenerators.add(indicator);
        return this;
    }

    public OreVeinDefinition standardVeinGenerator(Consumer<StandardVeinGenerator> config) {
        var veinGenerator = new StandardVeinGenerator();

        config.accept(veinGenerator);
        this.veinGenerator = veinGenerator;

        return this;
    }

    public OreVeinDefinition layeredVeinGenerator(Consumer<LayeredVeinGenerator> config) {
        var veinGenerator = new LayeredVeinGenerator();

        config.accept(veinGenerator);
        this.veinGenerator = veinGenerator;

        return this;
    }

    public OreVeinDefinition geodeVeinGenerator(Consumer<GeodeVeinGenerator> config) {
        var veinGenerator = new GeodeVeinGenerator();

        config.accept(veinGenerator);
        this.veinGenerator = veinGenerator;

        return this;
    }

    public OreVeinDefinition dikeVeinGenerator(Consumer<DikeVeinGenerator> config) {
        var veinGenerator = new DikeVeinGenerator();
        if (inferredProperties.heightRange != null) {
            veinGenerator.minYLevel(inferredProperties.heightRange.firstInt());
            veinGenerator.maxYLevel(inferredProperties.heightRange.secondInt());
        }

        config.accept(veinGenerator);
        this.veinGenerator = veinGenerator;

        return this;
    }

    public OreVeinDefinition veinedVeinGenerator(Consumer<VeinedVeinGenerator> config) {
        var veinGenerator = new VeinedVeinGenerator();
        if (inferredProperties.heightRange != null) {
            veinGenerator.minYLevel(inferredProperties.heightRange.firstInt());
            veinGenerator.maxYLevel(inferredProperties.heightRange.secondInt());
        }

        config.accept(veinGenerator);
        this.veinGenerator = veinGenerator;

        return this;
    }

    public OreVeinDefinition classicVeinGenerator(Consumer<ClassicVeinGenerator> config) {
        var veinGenerator = new ClassicVeinGenerator();

        config.accept(veinGenerator);
        this.veinGenerator = veinGenerator;

        return this;
    }

    public OreVeinDefinition cuboidVeinGenerator(Consumer<CuboidVeinGenerator> config) {
        var veinGenerator = new CuboidVeinGenerator();
        if (inferredProperties.heightRange != null) {
            veinGenerator.minY(inferredProperties.heightRange.firstInt());
            veinGenerator.maxY(inferredProperties.heightRange.secondInt());
        }

        config.accept(veinGenerator);
        this.veinGenerator = veinGenerator;

        return this;
    }

    @Tolerate
    @Nullable
    public VeinGenerator veinGenerator(ResourceLocation id) {
        if (veinGenerator == null) {
            // noinspection DataFlowIssue
            veinGenerator = WorldGeneratorUtils.VEIN_GENERATOR_FUNCTIONS.containsKey(id) ?
                    WorldGeneratorUtils.VEIN_GENERATOR_FUNCTIONS.get(id).get() : null;
        }
        return veinGenerator;
    }

    @SuppressWarnings("SameParameterValue")

    public boolean canGenerate() {
        if (this.veinGenerator() instanceof NoopVeinGenerator) {
            return false;
        }
        return this.weight() > 0 || !this.biomeWeightModifier().isEmpty();
    }

    private static class InferredProperties {

        public IntIntPair heightRange = null;
    }

    /**
     * roll_mod: what a vein does differently in one world of its {@code dimension_filter}. Every field is
     * optional; a missing one falls back to the vein's own.
     */
    public record DimensionOverride(Optional<IWorldGenLayer> layer,
                                    Optional<HeightRangePlacement> heightRange,
                                    Optional<HolderSet<Biome>> biomes) {

        // spotless:off
        public static final Codec<DimensionOverride> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                IWorldGenLayer.CODEC.optionalFieldOf("layer").forGetter(DimensionOverride::layer),
                HeightRangePlacement.CODEC.codec().optionalFieldOf("height_range").forGetter(DimensionOverride::heightRange),
                RegistryCodecs.homogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(DimensionOverride::biomes)
        ).apply(instance, DimensionOverride::new));
        // spotless:on
    }
}
