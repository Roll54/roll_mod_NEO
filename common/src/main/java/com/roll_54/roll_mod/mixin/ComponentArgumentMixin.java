package com.roll_54.roll_mod.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.roll_54.roll_mod.util.ItemMentions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code <item:…>} tags in JSON text — {@code /tellraw}, {@code /title}, boss bar and scoreboard
 * names, every command that takes a component argument — expanded when the command's source is an
 * operator (see {@link ItemMentions}). Done on the parsed argument, before the command resolves
 * selectors and scores in it, so the result goes through that resolution like any other text.
 */
@Mixin(ComponentArgument.class)
public abstract class ComponentArgumentMixin {

    @Inject(method = "getComponent", at = @At("RETURN"), cancellable = true)
    private static void rollmod$itemMentions(CommandContext<CommandSourceStack> context, String name,
                                             CallbackInfoReturnable<Component> cir) {
        if (ItemMentions.allowed(context.getSource())) {
            cir.setReturnValue(ItemMentions.expand(cir.getReturnValue()));
        }
    }
}
