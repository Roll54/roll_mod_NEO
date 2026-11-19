package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.CommandHistory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Keeps the last 128 commands in {@code command_history.txt} (the chat's up-arrow history, kept between
 * sessions) instead of vanilla's 50.
 */
@Mixin(CommandHistory.class)
public abstract class CommandHistoryMixin {

    private static final int ROLL_MOD$KEEP = 128;

    @ModifyConstant(method = {"<init>", "addCommand"}, constant = @Constant(intValue = 50))
    private int roll_mod$keepMore(int vanilla) {
        return ROLL_MOD$KEEP;
    }
}
