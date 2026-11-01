package com.roll_54.roll_mod.mixin;

import com.roll_54.roll_mod.minestar.chat.ChatDevFeed;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Copies what commands answer into the chat's DEV tab ({@link ChatDevFeed#output}). A silent source
 * (functions, {@code execute ... run} with nobody to tell) answers no one, so it is not copied either.
 */
@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackDevFeedMixin {

    @Shadow @Final private boolean silent;

    @Inject(method = "sendSuccess", at = @At("HEAD"))
    private void rollmod$devFeedSuccess(Supplier<Component> message, boolean allowLogging, CallbackInfo ci) {
        if (!silent) ChatDevFeed.output((CommandSourceStack) (Object) this, message.get(), false);
    }

    @Inject(method = "sendFailure", at = @At("HEAD"))
    private void rollmod$devFeedFailure(Component message, CallbackInfo ci) {
        if (!silent) ChatDevFeed.output((CommandSourceStack) (Object) this, message, true);
    }
}
