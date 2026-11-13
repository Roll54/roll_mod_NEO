package com.roll_54.roll_mod.minestar.chat;

import com.roll_54.roll_mod.minestar.chat.emoji.EmojiRegistry;
import com.roll_54.roll_mod.minestar.interactions.InteractionSettings;
import com.roll_54.roll_mod.network.packet.MentionPingPacket;
import com.roll_54.roll_mod.registry.ModConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "@Name" in a chat message, like a mention on Discord: the name of an online player is drawn highlighted,
 * and that player hears a ping ({@link MentionPingPacket}) if they got the message. Ported from Eden_LD's
 * mentions; on in {@code chat_changes.mentions}.
 */
public final class ChatMentions {

    /** "@" at the start or after a space, then a Minecraft name. */
    private static final Pattern MENTION = Pattern.compile("(?<![\\w@])@([A-Za-z0-9_]{1,16})(?![A-Za-z0-9_])");

    private ChatMentions() {}

    private static boolean enabled() {
        return ModConfigs.MAIN == null || ModConfigs.MAIN.chat_changes.mentions;
    }

    /**
     * The typed text as a chat component in {@code color} (emojis and links drawn by {@link EmojiRegistry#text}),
     * with each "@Name" of an online player highlighted and spelled as the player spells it.
     */
    public static MutableComponent text(MinecraftServer server, String typed, ChatFormatting color) {
        if (!enabled() || typed.indexOf('@') < 0) return EmojiRegistry.text(typed).withStyle(color);
        MutableComponent out = Component.empty();
        Matcher matcher = MENTION.matcher(typed);
        int from = 0;
        while (matcher.find()) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(matcher.group(1));
            if (player == null) continue;
            if (matcher.start() > from) out.append(EmojiRegistry.text(typed.substring(from, matcher.start())).withStyle(color));
            String name = player.getGameProfile().getName();
            out.append(Component.literal("@" + name).withStyle(style -> style
                    .withColor(ChatFormatting.AQUA)
                    .withInsertion("@" + name + " ")
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, player.getDisplayName()))));
            from = matcher.end();
        }
        if (from < typed.length()) out.append(EmojiRegistry.text(typed.substring(from)).withStyle(color));
        return out;
    }

    /** The online players "@Name"d in {@code typed}, each once. */
    public static Set<ServerPlayer> mentioned(MinecraftServer server, String typed) {
        Set<ServerPlayer> out = new LinkedHashSet<>();
        if (!enabled() || typed.indexOf('@') < 0) return out;
        Matcher matcher = MENTION.matcher(typed);
        while (matcher.find()) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(matcher.group(1));
            if (player != null) out.add(player);
        }
        return out;
    }

    /**
     * The ping for everyone {@code typed} mentions among {@code recipients} — those who got the line — except
     * the writer and anyone ignoring them.
     */
    public static void ping(ServerPlayer sender, ChatChannel channel, String typed, Collection<ServerPlayer> recipients) {
        for (ServerPlayer player : mentioned(sender.server, typed)) {
            if (player == sender || !recipients.contains(player)) continue;
            if (InteractionSettings.ignores(player.getUUID(), sender.getUUID())) continue;
            PacketDistributor.sendToPlayer(player, new MentionPingPacket(channel));
        }
    }
}
