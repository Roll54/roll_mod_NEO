package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MuteStore;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Every moderation rule, in one place.
 *
 * <p>Follows {@code WarpService}'s doctrine: the packet handlers and the commands are thin, and
 * every permission check, every clamp and every side effect happens here. A caller cannot forget
 * something this class does not let it skip — which matters more than usual for a module whose
 * actions are hard to undo.
 */
public final class ModerationService {

    /** The warning that earns a ban, and every warning after it. */
    public static final int WARNS_BEFORE_BAN = 3;

    /** What that ban costs. */
    public static final long AUTO_BAN_MILLIS = 24L * 60L * 60L * 1000L;

    private ModerationService() {}

    /* --------------------------------------------- mute --------------------------------------------- */

    /** Mutes for {@code millis}, or indefinitely when {@code millis <= 0}. */
    public static void mute(ServerPlayer moderator, ServerPlayer target, long millis,
                            String rule, String note) {
        long until = millis <= 0L ? 0L : System.currentTimeMillis() + millis;
        MuteStore.mute(target.getUUID(), until, moderator.getGameProfile().getName(),
                reasonOf(rule, note));

        String duration = until == 0L
                ? Component.translatable("msg.roll_mod.mute.forever").getString()
                : Durations.format(millis);
        target.sendSystemMessage(Component.translatable("msg.roll_mod.mute.youAre", duration)
                .withStyle(ChatFormatting.RED));
        cite(target, rule);
        ModerationStatus.sendTo(target);

        ModerationLog.append(moderator.getGameProfile().getName(), "muted",
                target.getGameProfile().getName(), reasonOf(rule, note) + " for " + duration);
        announce(target.server, moderator, "msg.roll_mod.moderation.announce.muted",
                target.getGameProfile().getName(), duration);
        PunishmentLetters.send(target.server, target.getUUID(), PunishmentLetters.Kind.MUTE,
                moderator.getGameProfile().getName(), millis, rule, note, 0);
    }

    public static boolean unmute(ServerPlayer moderator, UUID target, String targetName) {
        if (!MuteStore.unmute(target)) return false;
        ModerationStatus.sendTo(moderator.server, target);
        ModerationLog.append(moderator.getGameProfile().getName(), "unmuted", targetName, "");
        return true;
    }

    /* --------------------------------------------- warn --------------------------------------------- */

    /**
     * Records a warning and returns the player's new total.
     *
     * <p>This is where the server's three-strike rule lives. The third warning and <em>every</em>
     * warning after it earns a day's ban — warnings never expire and are never reset by the ban
     * they trigger, so a fourth warning bans again. {@link WarnStore} deliberately has no way to
     * drop one; only an operator clearing them by hand can.
     */
    public static int warn(ServerPlayer moderator, ServerPlayer target, String rule, String note) {
        String targetName = target.getGameProfile().getName();
        int count = WarnStore.add(target.getUUID(), targetName,
                new WarnStore.Warn(System.currentTimeMillis(),
                        moderator.getGameProfile().getName(), rule, note));

        target.sendSystemMessage(Component.translatable("msg.roll_mod.moderation.warned", count)
                .withStyle(ChatFormatting.RED));
        cite(target, rule);
        ModerationStatus.sendTo(target);

        ModerationLog.append(moderator.getGameProfile().getName(), "warned", targetName,
                reasonOf(rule, note) + " (warning #" + count + ")");
        announce(target.server, moderator, "msg.roll_mod.moderation.announce.warned",
                targetName, String.valueOf(count));
        PunishmentLetters.send(target.server, target.getUUID(), PunishmentLetters.Kind.WARN,
                moderator.getGameProfile().getName(), 0L, rule, note, count);

        if (count >= WARNS_BEFORE_BAN) {
            // Automatic, so the audit line says "server" rather than crediting the moderator with a
            // ban they did not choose to issue.
            ban(target.server, "server", target.getUUID(), targetName, AUTO_BAN_MILLIS, rule,
                    "auto: warning #" + count);
        }
        return count;
    }

    /* ---------------------------------------------- ban --------------------------------------------- */

    /** Bans for {@code millis}, or permanently when {@code millis <= 0}, and kicks if they are on. */
    public static void ban(MinecraftServer server, String actor, UUID target, String targetName,
                           long millis, String rule, String reason) {
        long until = millis <= 0L ? 0L : System.currentTimeMillis() + millis;
        BanStore.ban(target, until, actor, rule, reason, targetName);

        ServerPlayer online = server.getPlayerList().getPlayer(target);
        if (online != null) {
            online.connection.disconnect(banScreen(BanStore.ban(target)));
        }

        String duration = until == 0L
                ? Component.translatable("msg.roll_mod.moderation.permanent").getString()
                : Durations.format(millis);
        ModerationLog.append(actor, "banned", targetName, reasonOf(rule, reason) + " for " + duration);
        RollMod.LOGGER.info("[Moderation] {} banned {} ({}) for {}.",
                actor, targetName, reasonOf(rule, reason), duration);
        broadcastToModerators(server, Component.translatable(
                "msg.roll_mod.moderation.announce.banned", actor, targetName, duration)
                .withStyle(ChatFormatting.GOLD));
        PunishmentLetters.send(server, target, PunishmentLetters.Kind.BAN, actor, millis, rule, reason, 0);
    }

    public static boolean unban(ServerPlayer moderator, UUID target, String targetName) {
        if (!BanStore.unban(target)) return false;
        ModerationLog.append(moderator.getGameProfile().getName(), "unbanned", targetName, "");
        return true;
    }

    /** The disconnect screen a banned player sees. */
    public static Component banScreen(@Nullable BanStore.Ban ban) {
        MutableComponent screen = Component.translatable("msg.roll_mod.moderation.kick.banned")
                .withStyle(ChatFormatting.RED);
        if (ban == null) return screen;

        if (!ban.rule().isBlank()) {
            screen.append("\n").append(RulesStore.label(ban.rule()).copy().withStyle(ChatFormatting.GRAY));
        }
        if (!ban.reason().isBlank()) {
            screen.append("\n")
                    .append(Component.literal(ban.reason()).withStyle(ChatFormatting.GRAY));
        }
        return screen.append("\n").append(ban.permanent()
                ? Component.translatable("msg.roll_mod.moderation.kick.permanent")
                        .withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable("msg.roll_mod.moderation.kick.expires",
                                Durations.format(ban.remaining(System.currentTimeMillis())))
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    /* ------------------------------------------- whitelist ------------------------------------------ */

    /**
     * Opens or closes the server.
     *
     * <p>Closing it sweeps everyone who cannot get past it. The sweep iterates a <em>copy</em> of
     * the player list: disconnecting mutates the live one, and a plain for-each over it throws.
     */
    public static void setWhitelist(MinecraftServer server, String actor, boolean enabled) {
        WhitelistStore.setEnabled(enabled);
        ModerationLog.append(actor, enabled ? "closed the server" : "opened the server", "", "");

        if (enabled) {
            Component message = whitelistScreen();
            for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
                if (!ModerationPermissions.bypassesWhitelist(player)) {
                    player.connection.disconnect(message);
                }
            }
        }
        broadcastToModerators(server, Component.translatable(enabled
                        ? "msg.roll_mod.moderation.announce.closed"
                        : "msg.roll_mod.moderation.announce.opened", actor)
                .withStyle(ChatFormatting.GOLD));
    }

    /** The disconnect screen a turned-away player sees, with the operator's own wording. */
    public static Component whitelistScreen() {
        return LegacyText.display(WhitelistStore.kickMessage());
    }

    /* -------------------------------------------- helpers ------------------------------------------- */

    /** Tells the player which rule(s) they broke, when any were cited. */
    private static void cite(ServerPlayer target, String rule) {
        if (rule == null || rule.isBlank()) return;
        target.sendSystemMessage(Component.translatable("msg.roll_mod.moderation.rule", RulesStore.label(rule))
                .withStyle(ChatFormatting.GRAY));
    }

    /** What goes in the file for whoever reads it: the rule id and the moderator's own words. */
    private static String reasonOf(String rule, String note) {
        if (rule == null || rule.isBlank()) return note == null ? "" : note;
        if (note == null || note.isBlank()) return rule;
        return rule + ": " + note;
    }

    private static void announce(MinecraftServer server, ServerPlayer moderator, String key,
                                 String targetName, String detail) {
        broadcastToModerators(server, Component.translatable(key,
                        moderator.getGameProfile().getName(), targetName, detail)
                .withStyle(ChatFormatting.GOLD));
    }

    /**
     * Pings everyone who can act on it — the {@code WarpService.notifyModerators} loop, so a
     * moderator who is not an operator still hears.
     */
    static void broadcastToModerators(MinecraftServer server, Component message) {
        for (ServerPlayer staff : server.getPlayerList().getPlayers()) {
            if (ModerationPermissions.canModerate(staff)) staff.sendSystemMessage(message);
        }
    }

}
