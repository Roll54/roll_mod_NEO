package com.roll_54.roll_mod_server.minestar;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannels;
import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod.minestar.friends.FriendEntry;
import com.roll_54.roll_mod.minestar.friends.FriendsService;
import com.roll_54.roll_mod_server.RollModServer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * {@code /fc <message>} — friends chat: the message goes to the sender's Minestar friends who are on
 * this server, and to the sender. What the Friends button over the chat box sends through.
 *
 * <p>Formatted like {@link ChatHandler}'s global and local lines — LuckPerms prefix and suffix
 * around the name — with a {@code [F]} tag in front, and delivered as a {@link ChatChannel#FRIENDS}
 * line so it shows under the Friends button. Muted players are refused, as in the other channels.
 */
@EventBusSubscriber(modid = RollModServer.MODID)
public final class FriendChatCommand {

    private FriendChatCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("fc")
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
            sender.sendSystemMessage(Component.translatable("msg.roll_mod.chat.friends.muted").withStyle(ChatFormatting.RED));
            return;
        }
        FriendsService.withFriends(sender, friends -> {
            if (sender.hasDisconnected()) return;
            if (friends == null) {
                sender.sendSystemMessage(Component.translatable("msg.roll_mod.chat.friends.unavailable")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            MinecraftServer server = sender.server;
            List<ServerPlayer> online = new ArrayList<>();
            for (FriendEntry friend : friends) {
                ServerPlayer player = server.getPlayerList().getPlayer(friend.id());
                if (player != null) online.add(player);
            }

            Component line = Component.literal("")
                    .append(Component.literal("[F] ").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(ChatHandler.lpPrefix(sender))
                    .append(sender.getDisplayName())
                    .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE));

            ChatChannels.deliver(sender, ChatChannel.FRIENDS, line);
            online.forEach(friend -> ChatChannels.deliver(friend, ChatChannel.FRIENDS, line, sender.getUUID()));
            if (online.isEmpty()) {
                // On the friends channel, so it still shows while the chat is filtered to Friends.
                ChatChannels.deliver(sender, ChatChannel.FRIENDS,
                        Component.translatable("msg.roll_mod.chat.friends.nobody").withStyle(ChatFormatting.GRAY));
            }

            RollMod.LOGGER.info(String.format(Locale.US, "[FRIENDS] %s (%s) -> \"%s\" recipients=[%s]",
                    sender.getGameProfile().getName(), sender.getUUID(), text,
                    online.stream().map(p -> p.getGameProfile().getName()).collect(Collectors.joining(", "))));
        });
    }
}
