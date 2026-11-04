package com.roll_54.roll_mod.gtmogs.api.worldgen;

import com.roll_54.roll_mod.compat.gtmogs.OreMaterials;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record GTLayerPattern(List<Layer> layers) {

    public static final Codec<GTLayerPattern> CODEC = Codec.list(Layer.CODEC)
            .xmap(GTLayerPattern::new, pattern -> pattern.layers);

    public Layer rollNext(@Nullable Layer previous, RandomSource random) {
        int totalWeight = 0;
        for (Layer layer : layers)
            if (layer != previous)
                totalWeight += layer.weight;
        int rolled = random.nextInt(totalWeight);

        for (Layer layer : layers) {
            if (layer == previous)
                continue;
            rolled -= layer.weight;
            if (rolled < 0)
                return layer;
        }
        return null;
    }

    public static Builder builder(RuleTest... rules) {
        return new Builder(rules);
    }

    public static class Builder {

        private final List<Layer> layers = new ArrayList<>();
        private final RuleTest[] rules;

        protected Builder(RuleTest... rules) {
            this.rules = rules;
        }

        public Builder layer(Consumer<Layer.Builder> builder) {
            Layer.Builder layerBuilder = new Layer.Builder(rules);
            builder.accept(layerBuilder);
            layers.add(layerBuilder.build());
            return this;
        }

        public GTLayerPattern build() {
            return new GTLayerPattern(layers);
        }
    }

    /**
     * @param targets spotless:on
     */
    public record Layer(List<List<TargetBlockState>> targets, int minSize, int maxSize, int weight) {

        // spotless:off
        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(OreMaterials.TARGETS_CODEC).fieldOf("targets").forGetter(layer -> layer.targets),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("min_size").forGetter(layer -> layer.minSize),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("max_size").forGetter(layer -> layer.maxSize),
                Codec.intRange(0, Integer.MAX_VALUE).fieldOf("weight").forGetter(layer -> layer.weight)
        ).apply(instance, Layer::new));

        public Stream<VeinGenerator.VeinEntry> asVeinEntries() {
            return targets.stream()
                    .map(target -> VeinGenerator.mapTarget(target, weight));
        }

        public List<TargetBlockState> rollBlock(RandomSource random) {
            if (targets.size() == 1)
                return targets.get(0);
            return targets.get(random.nextInt(targets.size()));
        }

        public static class Builder {

            private final List<List<TargetBlockState>> targets = new ArrayList<>();
            private int minSize = 1;
            private int maxSize = 1;
            private int weight = 1;
            private final RuleTest[] rules;

            protected Builder(RuleTest... rules) {
                this.rules = rules;
            }

            public Builder block(Supplier<? extends Block> block) {
                return state(block.get().defaultBlockState());
            }

            public Builder state(Supplier<? extends BlockState> state) {
                return state(state.get());
            }

            public Builder state(BlockState state) {
                this.targets.add(Arrays.stream(this.rules).map(rule -> OreConfiguration.target(rule, state)).toList());
                return this;
            }

            public Builder weight(int weight) {
                this.weight = weight;
                return this;
            }

            public Builder size(int min, int max) {
                this.minSize = min;
                this.maxSize = max;
                return this;
            }

            public Layer build() {
                return new Layer(targets, minSize, maxSize, weight);
            }
        }
    }
}
