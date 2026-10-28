package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Letters no chat message may contain. They only exist in Russian, so a message with one breaks rule
 * {@value #RULE} (no Russian in public chats): it is never sent, and its author gets a real warning
 * from {@code "server"} — counted toward {@link ModerationService#WARNS_BEFORE_BAN} like any other.
 *
 * <p>Covers every way a player talks: the channel commands the chat buttons send ({@code /gc},
 * {@code /lc}, {@code /fc}, {@code /tc}), private and emote commands ({@link #CHAT_COMMANDS}), and
 * plain chat. The commands are caught on {@link CommandEvent}, before they run, so a blocked
 * {@code /gc} never posts its own chat event and is warned for once.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ProhibitedLetters {

    public static final String RULE = "2.1";

    /** Lower case; matched case-insensitively. */
    private static final Set<Integer> LETTERS = Set.of((int) 'э', (int) 'ы');

    /** Commands whose arguments are a message someone else reads; namespace and {@code /} stripped. */
    private static final Set<String> CHAT_COMMANDS = Set.of(
            "gc", "lc", "fc", "tc",
            "msg", "tell", "w", "r", "reply", "me", "say", "teammsg", "tm");

    private ProhibitedLetters() {}

    /** The first prohibited letter in {@code text}, as typed. */
    public static OptionalInt find(String text) {
        return text.codePoints()
                .filter(c -> LETTERS.contains(Character.toLowerCase(c)))
                .findFirst();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCommand(CommandEvent event) {
        ServerPlayer player = event.getParseResults().getContext().getSource().getPlayer();
        if (player == null) return;
        String input = event.getParseResults().getReader().getString().strip();
        if (input.startsWith("/")) input = input.substring(1);
        int space = input.indexOf(' ');
        if (space < 0) return;
        String root = input.substring(0, space).toLowerCase(Locale.ROOT);
        root = root.substring(root.indexOf(':') + 1);
        if (!CHAT_COMMANDS.contains(root)) return;

        OptionalInt letter = find(input.substring(space + 1));
        if (letter.isEmpty()) return;
        event.setCanceled(true);
        punish(player, letter.getAsInt(), "/" + root);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChat(ServerChatEvent event) {
        OptionalInt letter = find(event.getRawText());
        if (letter.isEmpty()) return;
        event.setCanceled(true);
        punish(event.getPlayer(), letter.getAsInt(), "chat");
    }

    private static void punish(ServerPlayer player, int letter, String where) {
        String typed = Character.toString(letter);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.moderation.prohibited_letter", typed)
                .withStyle(ChatFormatting.RED));
        ModerationService.warn("server", player, RULE, "auto: prohibited letter '" + typed + "' in " + where);
    }
}
