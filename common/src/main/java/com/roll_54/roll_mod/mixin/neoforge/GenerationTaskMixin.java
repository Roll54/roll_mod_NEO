package com.roll_54.roll_mod.mixin.neoforge;

import com.roll_54.roll_mod.config.MyConfig;
import net.neoforged.neoforge.server.command.generation.GenerationTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Makes NeoForge's pregeneration batch size and refill threshold configurable ({@code pregen} in roll_mod.json5).
 * Both are {@code static final int}s that javac inlines, so they are replaced at their use sites.
 * The 32/8 defaults leave worldgen threads idle on many-core machines.
 */
@Mixin(GenerationTask.class)
public abstract class GenerationTaskMixin {
    private static final int NEOFORGE_BATCH_SIZE = 32;
    private static final int NEOFORGE_QUEUE_THRESHOLD = 8;

    @ModifyConstant(method = "tryEnqueueTasks", constant = @Constant(intValue = NEOFORGE_BATCH_SIZE))
    private int roll_mod$batchSize(int original) {
        return roll_mod$batch();
    }

    @ModifyConstant(method = "acceptChunkResult", constant = @Constant(intValue = NEOFORGE_QUEUE_THRESHOLD))
    private int roll_mod$queueThreshold(int original) {
        MyConfig cfg = MyConfig.INSTANCE;
        if (cfg == null) return original;
        return Math.min(Math.max(0, cfg.pregen.queueThreshold), roll_mod$batch() - 1);
    }

    private static int roll_mod$batch() {
        MyConfig cfg = MyConfig.INSTANCE;
        return cfg == null ? NEOFORGE_BATCH_SIZE : Math.max(1, cfg.pregen.batchSize);
    }
}
