package com.roll_54.roll_mod.minestar.letters;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.minestar.moderation.ModerationLog;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.minestar.moderation.ModerationStatus;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writing, withdrawing and accepting letters. Every rule lives here, the {@code WarpService}
 * doctrine: the packets are thin and re-check nothing this class does not also check.
 */
public final class LetterService {

    /** What the compose form starts at, and what a letter sent with no expiry typed gets. */
    public static final long DEFAULT_LIFETIME = 7L * 24L * 60L * 60L * 1000L;

    private LetterService() {}

    /** How many letters this player has not accepted yet. */
    public static int unread(ServerPlayer player) {
        int count = 0;
        for (Letter letter : LetterStore.visibleTo(player.server, player.getUUID())) {
            if (!letter.acceptedBy(player.getUUID())) count++;
        }
        return count;
    }

    /**
     * Writes a letter. {@code delay} is how long until it goes out ({@code 0} = now); {@code lifetime}
     * how long it then stays up ({@code 0} = forever), counted from when it goes out.
     */
    public static boolean create(ServerPlayer author, String title, String body, long delay, long lifetime,
                                 List<LetterReward> rewards, String icon) {
        if (!ModerationPermissions.canWriteLetters(author)) return false;
        if (!commandRewardsAllowed(author, rewards)) return false;

        String cleanTitle = clamp(title, Letter.MAX_TITLE);
        if (cleanTitle.isBlank()) {
            author.sendSystemMessage(Component.translatable("msg.roll_mod.letters.needTitle")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        long now = System.currentTimeMillis();
        long sendAt = delay <= 0L ? now : now + delay;
        Letter letter = new Letter(UUID.randomUUID(), cleanTitle, clamp(body, Letter.MAX_BODY),
                author.getGameProfile().getName(), now, sendAt, lifetime <= 0L ? 0L : sendAt + lifetime,
                kept(rewards), LetterIcon.clean(icon), Map.of());
        LetterStore.put(author.server, letter);

        ModerationLog.append(author.getGameProfile().getName(),
                delay > 0L ? "scheduled a letter" : "sent a letter", "",
                "'" + cleanTitle + "' with " + letter.rewards().size() + " reward(s)");

        if (delay > 0L) {
            author.sendSystemMessage(Component.translatable("msg.roll_mod.letters.scheduled",
                    LegacyText.display(cleanTitle), Durations.format(delay)));
            changed(author.server);
        } else {
            author.sendSystemMessage(Component.translatable("msg.roll_mod.letters.sent",
                    LegacyText.display(cleanTitle)));
            deliver(author.server, letter);
        }
        return true;
    }

    /**
     * Sends {@code recipient} — and only them — a letter from the server itself: no author to check,
     * no permission, callable from anywhere on the server thread. What punishments use to leave the
     * player a written record (see {@code PunishmentLetters}).
     *
     * <p>An offline recipient finds it in the list at their next login, where the unread badge counts
     * it; {@code lifetime} is how long it waits for them ({@code 0} = forever).
     */
    public static void sendSystem(MinecraftServer server, UUID recipient, String author, String title,
                                  String body, String icon, long lifetime) {
        long now = System.currentTimeMillis();
        Letter letter = new Letter(UUID.randomUUID(), clamp(title, Letter.MAX_TITLE),
                clamp(body, Letter.MAX_BODY), clamp(author, 64), now, now,
                lifetime <= 0L ? 0L : now + lifetime, List.of(), LetterIcon.clean(icon), Map.of(),
                recipient);
        LetterStore.put(server, letter);
        ModerationLog.append("SYSTEM", "sent a letter to", recipient.toString(), "'" + letter.title() + "'");
        deliver(server, letter);
    }

    /**
     * Edits a letter in place. Whoever already accepted it keeps what they got and is not offered it
     * again — {@code accepted} is carried over — and everyone else sees the new version.
     *
     * <p>A letter that is still due takes its send time from {@code delay} again. One already out
     * keeps its send time, and {@code lifetime} counts from now: typing {@code 7d} on an expired letter
     * re-opens it for a week.
     */
    public static boolean update(ServerPlayer author, UUID id, String title, String body, long delay,
                                 long lifetime, List<LetterReward> rewards, String icon) {
        if (!ModerationPermissions.canWriteLetters(author)) return false;
        if (!commandRewardsAllowed(author, rewards)) return false;
        Letter letter = LetterStore.byId(author.server, id);
        if (letter == null) return false;

        String cleanTitle = clamp(title, Letter.MAX_TITLE);
        if (cleanTitle.isBlank()) {
            author.sendSystemMessage(Component.translatable("msg.roll_mod.letters.needTitle")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        long now = System.currentTimeMillis();
        boolean wasDue = letter.due(now);
        long sendAt = wasDue ? (delay <= 0L ? now : now + delay) : letter.sendAt();
        long base = wasDue ? sendAt : now;
        Letter edited = letter.edited(cleanTitle, clamp(body, Letter.MAX_BODY), sendAt,
                lifetime <= 0L ? 0L : base + lifetime, kept(rewards), LetterIcon.clean(icon));
        LetterStore.put(author.server, edited);

        ModerationLog.append(author.getGameProfile().getName(), "edited a letter", "",
                "'" + cleanTitle + "'");
        author.sendSystemMessage(Component.translatable("msg.roll_mod.letters.saved",
                LegacyText.display(cleanTitle)));

        // A due letter pulled forward to "now" goes out now, with its announcement.
        if (wasDue && !edited.due(now)) {
            deliver(author.server, edited);
        } else {
            changed(author.server);
        }
        return true;
    }

    public static boolean delete(ServerPlayer author, UUID id) {
        if (!ModerationPermissions.canWriteLetters(author)) return false;
        Letter letter = LetterStore.byId(author.server, id);
        if (letter == null || !LetterStore.remove(author.server, id)) return false;
        ModerationLog.append(author.getGameProfile().getName(), "withdrew a letter", "",
                "'" + letter.title() + "'");
        changed(author.server);
        return true;
    }

    /**
     * Takes the letter and pays out. Recorded <em>before</em> paying, so a second packet arriving
     * before the first finishes finds it already taken and pays nothing.
     */
    public static void accept(ServerPlayer reader, UUID id) {
        MinecraftServer server = reader.server;
        Letter letter = LetterStore.byId(server, id);
        if (letter == null || !letter.visible(System.currentTimeMillis())) return;
        if (!LetterStore.markAccepted(server, id, reader.getUUID())) return;

        for (LetterReward reward : letter.rewards()) {
            grant(reader, letter, reward);
        }
        if (!letter.rewards().isEmpty()) {
            reader.sendSystemMessage(Component.translatable("msg.roll_mod.letters.accepted",
                    LegacyText.display(letter.title())).withStyle(ChatFormatting.GREEN));
        }

        LetterViewers.syncTo(reader);
        LetterViewers.resyncModerators(server);
        ModerationStatus.sendTo(reader);
    }

    /**
     * COMMAND rewards run at permission level 4 when accepted (see {@link #grant}), so attaching one
     * takes level 4 — the letters permission alone (level 2 / the LuckPerms node) would otherwise be
     * a straight escalation: write a letter with {@code op @s}, accept it yourself.
     */
    private static boolean commandRewardsAllowed(ServerPlayer author, List<LetterReward> rewards) {
        if (rewards == null || author.hasPermissions(4)) return true;
        for (LetterReward reward : rewards) {
            if (reward.type() == LetterReward.Type.COMMAND) {
                author.sendSystemMessage(Component.translatable("msg.roll_mod.letters.commandNeedsOp")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
        }
        return true;
    }

    private static void grant(ServerPlayer reader, Letter letter, LetterReward reward) {
        switch (reward.type()) {
            case MONEY -> CurrencyService.deposit(reader, reward.currency(), reward.amount(), reader.server);
            case ITEM -> {
                ItemStack copy = reward.item().copy();
                if (!reader.getInventory().add(copy) && !copy.isEmpty()) {
                    reader.drop(copy, false);
                }
            }
            case COMMAND -> {
                // As the reader, so @s is them; at level 4, because the letter's author was trusted
                // with exactly that when they were given the letters permission. Output suppressed:
                // the reader should see what the command does, not its console chatter.
                CommandSourceStack source = reader.createCommandSourceStack()
                        .withPermission(4).withSuppressedOutput();
                reader.server.getCommands().performPrefixedCommand(source, reward.command());
                ModerationLog.append(letter.author(), "letter command ran for",
                        reader.getGameProfile().getName(), "/" + reward.command());
                RollMod.LOGGER.info("[Letters] '{}' ran /{} as {}.", letter.title(),
                        reward.command(), reader.getGameProfile().getName());
            }
        }
    }

    /** Resyncs, and tells everyone online the letter is for that it has arrived. */
    private static void deliver(MinecraftServer server, Letter letter) {
        changed(server);
        Component notice = Component.translatable("msg.roll_mod.letters.arrived",
                LegacyText.display(letter.title())).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (letter.isFor(player.getUUID())) player.sendSystemMessage(notice);
        }
    }

    /** The end of the last window {@link #tick} looked at; {@code 0} until the first tick. */
    private static long lastCheck;

    /**
     * Delivers scheduled letters whose time has come. Once a second, from the server tick.
     *
     * <p>Looks at the window since the last check rather than keeping a "delivered" flag on disk. The
     * window starts at the first tick after boot, so a letter whose time passed while the server was
     * down simply becomes readable without a chat announcement to nobody.
     */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        long now = System.currentTimeMillis();
        long from = lastCheck;
        lastCheck = now;
        if (from == 0L) return;
        for (Letter letter : LetterStore.all(server)) {
            if (letter.sendAt() > from && letter.sendAt() <= now && !letter.expired(now)) {
                deliver(server, letter);
            }
        }
    }

    private static List<LetterReward> kept(List<LetterReward> rewards) {
        List<LetterReward> kept = new ArrayList<>();
        for (LetterReward reward : rewards) {
            if (reward.valid() && kept.size() < Letter.MAX_REWARDS) kept.add(reward);
        }
        return List.copyOf(kept);
    }

    /** Pushes the new list to open hubs and the new unread count to everyone online. */
    private static void changed(MinecraftServer server) {
        LetterViewers.resync(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModerationStatus.sendTo(player);
        }
    }

    private static String clamp(String text, int max) {
        String clean = text == null ? "" : text.strip();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }
}
