package com.roll_54.roll_mod.mixin.neoforge;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.roll_54.roll_mod.minestar.pregen.MultiGenerateCommand;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps NeoForge's {@code /neoforge generate} (one task for the whole server) for
 * {@link MultiGenerateCommand} (one task per dimension, concurrently).
 */
@Mixin(targets = "net.neoforged.neoforge.server.command.GenerateCommand")
public abstract class GenerateCommandMixin {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$multiWorld(CallbackInfoReturnable<ArgumentBuilder<CommandSourceStack, ?>> cir) {
        cir.setReturnValue(MultiGenerateCommand.register());
    }
}
