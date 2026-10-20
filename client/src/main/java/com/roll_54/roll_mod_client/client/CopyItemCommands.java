package com.roll_54.roll_mod_client.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.function.Function;

/**
 * {@code /rollmod copyid} and {@code /rollmod copyname} — print the held item's full registry id
 * or just its path (no mod id) in chat (click to copy again) and put it on the clipboard straight away.
 *
 * <p>Client-side because only the client can touch the clipboard. Any other {@code /rollmod ...}
 * is unknown to the client dispatcher, so NeoForge still forwards it to the server.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class CopyItemCommands {

    private CopyItemCommands() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rollmod")
                .then(copy("copyid", stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()))
                .then(copy("copyname", stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> copy(String name, Function<ItemStack, String> extractor) {
        return Commands.literal(name).executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return 0;

            ItemStack stack = mc.player.getMainHandItem();
            if (stack.isEmpty()) stack = mc.player.getOffhandItem();
            if (stack.isEmpty()) {
                mc.player.sendSystemMessage(Component.translatable("msg.roll_mod.copy.empty_hand")
                        .withStyle(ChatFormatting.RED));
                return 0;
            }

            String value = extractor.apply(stack);
            mc.keyboardHandler.setClipboard(value);

            Component copyable = Component.literal(value).withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("chat.copy.click"))));
            mc.player.sendSystemMessage(Component.translatable("msg.roll_mod.copy.done", copyable)
                    .withStyle(ChatFormatting.GRAY));
            return 1;
        });
    }
}
