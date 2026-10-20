package com.roll_54.roll_mod_client.mixin.mi;

import aztech.modern_industrialization.MI;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

/**
 * Always loads MI's runtime-generated resources ({@code <gamedir>/modern_industrialization/generated_resources})
 * on the client, so the textures {@link MIClientForceRuntimeDatagenMixin} has MI regenerate replace the
 * pre-tinted ones in MI's jar. The AddPackFindersEvent lambda in MI's constructor checks
 * {@code loadRuntimeGeneratedResources} before adding the pack. The slice starts at that field, so the
 * {@code datagenOnStartup} check before it (dedicated-server data datagen) is left alone. Client-only
 * mixin config: dedicated servers keep MI's own setting.
 *
 * <p>The lambda is matched by its signature, {@code (Dist, AddPackFindersEvent)}, not its number: javac
 * renumbers lambdas whenever MI adds one before it ({@code lambda$new$11} in MI 2.4.1,
 * {@code lambda$new$13} in 2.5.6), and a pinned name crashed MI's class loading in 2.5.6.
 */
@Mixin(MI.class)
public class MIForceRuntimeResourcesMixin {
    @ModifyExpressionValue(method = "name=/^lambda\\$new\\$\\d+$/ desc=/^\\(Lnet\\/neoforged\\/api\\/distmarker\\/Dist;Lnet\\/neoforged\\/neoforge\\/event\\/AddPackFindersEvent;\\)V$/",
            slice = @Slice(from = @At(value = "FIELD", target = "Laztech/modern_industrialization/config/MIStartupConfig;loadRuntimeGeneratedResources:Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;")),
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;getAsBoolean()Z"))
    private static boolean roll_mod$alwaysLoadGenerated(boolean configured) {
        return true;
    }
}
