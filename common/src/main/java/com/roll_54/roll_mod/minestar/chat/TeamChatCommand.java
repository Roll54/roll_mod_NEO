package com.roll_54.roll_mod.minestar.chat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.ftb.TeamsFacade;
import com.roll_54.roll_mod.minestar.data.MuteStore;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Team chat, {@code /tm <message>}: what the Team button over the chat box sends through. Reaches the online
 * members of the sender's FTB Teams team (a party team, not the solo "player team"), and nobody else — no chat
 * event is posted, so no bridge carries it out. "@Name" pings like everywhere else ({@link ChatMentions}).
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class TeamChatCommand {

    private TeamChatCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tm")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            send(ctx.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(ctx, "message").trim());
                            return 1;
                        })));
    }

    private static void send(ServerPlayer sender, String text) {
        if (text.isEmpty()) return;
        if (MuteStore.isMuted(sender.getUUID())) {
            ChatChannels.deliver(sender, ChatChannel.TEAM,
                    Component.translatable("msg.roll_mod.chat.team.muted").withStyle(ChatFormatting.RED));
            return;
        }
        TeamsFacade.Party team = TeamsFacade.get().party(sender).orElse(null);
        if (team == null) {
            // On the team channel, so it still shows while the chat is filtered to Team.
            ChatChannels.deliver(sender, ChatChannel.TEAM,
                    Component.translatable("msg.roll_mod.chat.team.none").withStyle(ChatFormatting.GRAY));
            return;
        }

        List<ServerPlayer> online = new ArrayList<>();
        for (UUID member : team.members()) {
            ServerPlayer player = sender.server.getPlayerList().getPlayer(member);
            if (player != null) online.add(player);
        }

        Component line = Component.literal("")
                .append(Component.translatable("msg.roll_mod.chat.team.tag").withStyle(ChatFormatting.DARK_AQUA))
                .append(sender.getDisplayName())
                .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                .append(ChatMentions.text(sender.server, text, ChatFormatting.AQUA));

        online.forEach(player -> ChatChannels.deliver(player, ChatChannel.TEAM, line, sender.getUUID()));
        ChatMentions.ping(sender, ChatChannel.TEAM, text, online);
        if (online.size() <= 1) {
            ChatChannels.deliver(sender, ChatChannel.TEAM,
                    Component.translatable("msg.roll_mod.chat.team.nobody").withStyle(ChatFormatting.GRAY));
        }

        RollMod.LOGGER.info(String.format(Locale.US, "[TEAM] %s (%s) team=%s -> \"%s\" recipients=[%s]",
                sender.getGameProfile().getName(), sender.getUUID(), team.teamId(), text,
                online.stream().map(p -> p.getGameProfile().getName()).collect(Collectors.joining(", "))));
    }
}
