package com.roll_54.roll_mod.compat.gtmogs;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.gtmogs.api.worldgen.IWorldGenLayer;
import com.roll_54.roll_mod.gtmogs.api.worldgen.WorldGenLayers;
import com.roll_54.roll_mod.registry.GeneratedOreRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * roll_mod's ore materials ({@link OreDefinitions}) in vein files: a vein names a material once and gets that
 * material's ore in whatever rock layer it lands in, the way GregTech's material veins do.
 *
 * <p>Targets: any entry of a vein's target list may be {@code {"material": "sodalite"}}. It expands to one
 * target per {@link BlockSubLayer} the material has, each guarded by the rule test of that layer's
 * {@link WorldGenLayers} entry — {@code end_sodalite} where the rock is end stone, {@code stone_X} on stone,
 * {@code deepslate_X} on deepslate and so on. Rock variants (granite, tuff, basalt...) are then swapped
 * in at placement by {@link OreStoneVariants}, as for any other vein.
 *
 * <p>Indicators: {@code "materials"} on a surface indicator places the sample of the layer whose world the vein
 * is generating in ({@link #sampleFor}).
 */
public final class OreMaterials {

    /**
     * World-gen layers each block sub-layer's ore belongs to. The first one's rule test guards the ore; all of
     * them together say which worlds the sub-layer's ore (and sample) is for.
     */
    private static final Map<BlockSubLayer, List<WorldGenLayers>> LAYERS = new EnumMap<>(Map.of(
            BlockSubLayer.STONE, List.of(WorldGenLayers.STONE, WorldGenLayers.MINING_OVERWORLD_STONE),
            BlockSubLayer.DEEPSLATE, List.of(WorldGenLayers.DEEPSLATE, WorldGenLayers.MINING_OVERWORLD_DEEPSLATE),
            BlockSubLayer.NETHERRACK, List.of(WorldGenLayers.NETHERRACK),
            BlockSubLayer.END, List.of(WorldGenLayers.ENDSTONE),
            BlockSubLayer.MOON, List.of(WorldGenLayers.MOON_STONE),
            BlockSubLayer.MARS, List.of(WorldGenLayers.MARS_STONE),
            BlockSubLayer.VENUS, List.of(WorldGenLayers.VENUS_STONE),
            BlockSubLayer.MERCURY, List.of(WorldGenLayers.MERCURY_STONE)));

    private static final Map<String, OreDefinition> BY_ID = OreDefinitions.ORE_DEFINITIONS.stream()
            .collect(Collectors.toUnmodifiableMap(OreDefinition::id, Function.identity()));

    /** A material id from {@link OreDefinitions}, e.g. {@code "sodalite"}. */
    public static final Codec<OreDefinition> MATERIAL_CODEC = Codec.STRING.comapFlatMap(
            id -> {
                OreDefinition def = BY_ID.get(id);
                return def != null ? DataResult.success(def) :
                        DataResult.error(() -> "Unknown roll_mod ore material '" + id + "' (see OreDefinitions)");
            },
            OreDefinition::id);

    private static final Codec<OreDefinition> MATERIAL_TARGET_CODEC = RecordCodecBuilder.create(instance -> instance
            .group(MATERIAL_CODEC.fieldOf("material").forGetter(Function.identity()))
            .apply(instance, Function.identity()));

    /**
     * A vein's target list, where any entry may be {@code {"material": id}} in place of a {@code {state, target}}
     * pair. A material expands where it stands into {@link #targets(OreDefinition)}, so list order (the first
     * target that matches wins) is kept. Written back as the expanded list.
     */
    public static final Codec<List<TargetBlockState>> TARGETS_CODEC = Codec
            .either(TargetBlockState.CODEC, MATERIAL_TARGET_CODEC).listOf()
            .xmap(entries -> entries.stream()
                            .flatMap(entry -> entry.map(Stream::of, def -> targets(def).stream()))
                            .toList(),
                    targets -> targets.stream().map(Either::<TargetBlockState, OreDefinition>left).toList());

    private OreMaterials() {}

    /** The material with this {@link OreDefinition#id()}, or null. */
    public static OreDefinition byId(String id) {
        return BY_ID.get(id);
    }

    /** The material's ore in each of its sub-layers, guarded by that sub-layer's rock rule. */
    public static List<TargetBlockState> targets(OreDefinition def) {
        List<TargetBlockState> targets = new ArrayList<>();
        for (BlockSubLayer base : def.bases()) {
            List<WorldGenLayers> layers = LAYERS.get(base);
            Block ore = block(base.id() + "_" + def.id());
            if (layers == null || ore == null) continue;
            targets.add(OreConfiguration.target(layers.getFirst().getTarget(), ore.defaultBlockState()));
        }
        return targets;
    }

    /**
     * The material's surface sample for a vein in {@code layer} of {@code level}: the sample of the sub-layer that
     * layer is, else of the first sub-layer whose ore belongs to that world. Empty when the material has none there.
     */
    public static Optional<Block> sampleFor(OreDefinition def, IWorldGenLayer layer, ResourceKey<Level> level) {
        for (BlockSubLayer base : def.bases()) {
            List<WorldGenLayers> layers = LAYERS.get(base);
            if (layers == null || !layers.contains(layer)) continue;
            Block sample = block(GeneratedOreRegistry.oreSampleId(base, def.id()));
            if (sample != null) return Optional.of(sample);
        }
        for (BlockSubLayer base : def.bases()) {
            List<WorldGenLayers> layers = LAYERS.get(base);
            if (layers == null || layers.stream().noneMatch(l -> l.isApplicableForLevel(level))) continue;
            Block sample = block(GeneratedOreRegistry.oreSampleId(base, def.id()));
            if (sample != null) return Optional.of(sample);
        }
        return Optional.empty();
    }

    /** True when ores of {@code base} generate in any of {@code worlds}. */
    public static boolean runsIn(BlockSubLayer base, Set<ResourceKey<Level>> worlds) {
        List<WorldGenLayers> layers = LAYERS.get(base);
        return layers != null && layers.stream().anyMatch(layer -> worlds.stream().anyMatch(layer::isApplicableForLevel));
    }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.getOptional(RollMod.id(path)).orElse(null);
    }
}
