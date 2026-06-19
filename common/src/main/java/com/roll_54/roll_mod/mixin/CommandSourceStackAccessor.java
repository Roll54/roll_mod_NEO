package com.roll_54.roll_mod.mixin;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code source} ({@link CommandSource}) of a {@link CommandSourceStack}.
 *
 * <p>This is the original output sink / issuer of a command. {@code /execute as <entity>}
 * swaps the stack's <em>entity</em> via {@code withEntity(...)} but preserves this
 * {@code source}, so comparing it against the entity lets us tell a command the player
 * typed directly (source == entity) from one run on their behalf via {@code /execute as}.
 */
@Mixin(CommandSourceStack.class)
public interface CommandSourceStackAccessor {

    @Accessor("source")
    CommandSource roll_mod$getSource();
}
