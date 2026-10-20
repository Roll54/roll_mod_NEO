package com.roll_54.roll_mod_server.minestar;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannels;
import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * {@code /tc <message>} — trade chat: buying and selling, to everyone on the server, kept apart from
 * global talk. What the Trade button over the chat box sends through.
 *
 * <p>Formatted like {@link ChatHandler}'s global line — LuckPerms prefix before the name —
 * with a {@code [T]} tag in front, and delivered as a {@link ChatChannel#TRADE} line so it shows under
 * the Trade button. Muted players are refused, as in the other channels.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public final class TradeChatCommand {

    private TradeChatCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tc")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                            send(sender, StringArgumentType.getString(ctx, "message").trim());
                            return 1;
                        })));
    }

    private static void send(ServerPlayer sender, String text) {
        if (text.isEmpty()) return;
        if (MuteStore.isMuted(sender.getUUID())) {
            sender.sendSystemMessage(Component.translatable("msg.roll_mod.chat.trade.muted").withStyle(ChatFormatting.RED));
            return;
        }

        Component line = Component.literal("")
                .append(Component.literal("[T] ").withStyle(ChatFormatting.GOLD))
                .append(ChatHandler.lpPrefix(sender))
                .append(sender.getDisplayName())
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(text).withStyle(ChatFormatting.YELLOW));

        List<ServerPlayer> recipients = sender.server.getPlayerList().getPlayers();
        recipients.forEach(player -> ChatChannels.deliver(player, ChatChannel.TRADE, line, sender.getUUID()));

        RollMod.LOGGER.info(String.format(Locale.US, "[TRADE] %s (%s) -> \"%s\" recipients=[%s]",
                sender.getGameProfile().getName(), sender.getUUID(), text,
                recipients.stream().map(p -> p.getGameProfile().getName()).collect(Collectors.joining(", "))));
    }
}
