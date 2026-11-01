package com.roll_54.roll_mod_server.minestar;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatChannels;
import com.roll_54.roll_mod.minestar.chat.emoji.EmojiRegistry;
import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod.minestar.interactions.InteractionSettings;
import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

/**
 * {@code /pm <player> <message>} — a private message, delivered as a PM line to both sides, so it
 * shows in the PM tab. What the PM tab over the chat box sends through.
 *
 * <p>Each line names the other side, which is who the receiver's PM tab replies to. Formatted like
 * the other channels (LuckPerms prefix around the name). Muted players are refused; a receiver who
 * ignores the sender never gets it, though the sender still sees their own line, as in the other
 * channels.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public final class PmChatCommand {

    private PmChatCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pm")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    send(ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"),
                                            StringArgumentType.getString(ctx, "message").trim());
                                    return 1;
                                }))));
    }

    private static void send(ServerPlayer sender, ServerPlayer target, String text) {
        if (text.isEmpty()) return;
        if (sender == target) {
            sender.sendSystemMessage(Component.translatable("msg.roll_mod.chat.pm.self").withStyle(ChatFormatting.RED));
            return;
        }
        if (MuteStore.isMuted(sender.getUUID())) {
            sender.sendSystemMessage(Component.translatable("msg.roll_mod.chat.pm.muted").withStyle(ChatFormatting.RED));
            return;
        }

        Component body = EmojiRegistry.text(text).withStyle(ChatFormatting.LIGHT_PURPLE);
        Component outgoing = Component.literal("")
                .append(Component.translatable("msg.roll_mod.chat.pm.tag").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.translatable("msg.roll_mod.chat.pm.to", name(target)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(body);
        Component incoming = Component.literal("")
                .append(Component.translatable("msg.roll_mod.chat.pm.tag").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.translatable("msg.roll_mod.chat.pm.from", name(sender)).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(body);

        String targetName = target.getGameProfile().getName();
        String senderName = sender.getGameProfile().getName();
        ChatChannels.deliverPm(sender, outgoing, targetName);
        if (!InteractionSettings.ignores(target.getUUID(), sender.getUUID())) {
            ChatChannels.deliverPm(target, incoming, senderName);
        }

        RollMod.LOGGER.info(String.format(Locale.US, "[PM] %s -> %s: \"%s\"", senderName, targetName, text));
    }

    private static Component name(ServerPlayer player) {
        return Component.literal("").append(ChatHandler.lpPrefix(player)).append(player.getDisplayName());
    }
}
