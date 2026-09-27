package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.moderation.BanStore;
import com.roll_54.roll_mod.minestar.moderation.ModerationLog;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.minestar.moderation.ModerationService;
import com.roll_54.roll_mod.minestar.moderation.RulesStore;
import com.roll_54.roll_mod.minestar.moderation.WarnStore;
import com.roll_54.roll_mod.minestar.moderation.WhitelistStore;
import com.roll_54.roll_mod.util.Durations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Client → server: every button on the moderation tab.
 *
 * <p>Sent optimistically. The handler re-checks the permission for each action — mute and warn need
 * {@code canModerate}, ban, unban and the whitelist need the stricter {@code canBan}, because closing
 * the server kicks everyone — so a spoofed packet from a player who cannot see the tab does nothing.
 *
 * @param millis a duration for mute and ban ({@code 0} = no end); for {@link Action#SET_WHITELIST},
 *               non-zero means "close the server"
 * @param rule   the rules broken, comma-joined ({@code "1.2,2.5"}) — the moderation tab lets a
 *               moderator cite several; blank for none. Unknown ids are dropped on arrival
 * @param note   the moderator's own words; for {@link Action#SET_WHITELIST_MESSAGE}, the message
 */
public record ModerationActionPacket(Action action, UUID target, long millis, String rule, String note)
        implements CustomPacketPayload {

    /** Append only: the codec sends the ordinal. */
    public enum Action { MUTE, UNMUTE, WARN, BAN, UNBAN, SET_WHITELIST, SET_WHITELIST_MESSAGE }

    /** Room for {@link #MAX_RULES} comma-joined rule ids. */
    private static final int MAX_RULE = 256;
    /** The most rules one punishment may cite. */
    private static final int MAX_RULES = 16;
    private static final int MAX_NOTE = 256;

    /** Longest a GUI mute or ban may be; anything longer is what "no end" is for. */
    private static final long MAX_MILLIS = 3650L * 24L * 60L * 60L * 1000L;

    private static final UUID NONE = new UUID(0, 0);

    public static final Type<ModerationActionPacket> TYPE = new Type<>(RollMod.id("moderation_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModerationActionPacket> STREAM_CODEC =
            StreamCodec.of(ModerationActionPacket::encode, ModerationActionPacket::decode);

    public static ModerationActionPacket of(Action action, UUID target, long millis, String rule, String note) {
        return new ModerationActionPacket(action, target, millis, clamp(rule, MAX_RULE), clamp(note, MAX_NOTE));
    }

    public static ModerationActionPacket whitelist(boolean closed) {
        return of(Action.SET_WHITELIST, NONE, closed ? 1L : 0L, "", "");
    }

    public static ModerationActionPacket whitelistMessage(String message) {
        return of(Action.SET_WHITELIST_MESSAGE, NONE, 0L, "", message);
    }

    private static String clamp(String text, int max) {
        String clean = text == null ? "" : text.strip();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    /** The cited rules that exist, each once, at most {@link #MAX_RULES}, re-joined. */
    private static String knownRules(String joined) {
        return RulesStore.join(RulesStore.ids(joined).stream()
                .filter(id -> RulesStore.byId(id) != null)
                .distinct()
                .limit(MAX_RULES)
                .toList());
    }

    private static void encode(RegistryFriendlyByteBuf buf, ModerationActionPacket p) {
        buf.writeVarInt(p.action.ordinal());
        buf.writeUUID(p.target);
        buf.writeVarLong(Math.max(0L, p.millis));
        buf.writeUtf(p.rule, MAX_RULE);
        buf.writeUtf(p.note, MAX_NOTE);
    }

    private static ModerationActionPacket decode(RegistryFriendlyByteBuf buf) {
        Action action = Action.values()[Math.floorMod(buf.readVarInt(), Action.values().length)];
        return new ModerationActionPacket(action, buf.readUUID(),
                Math.min(MAX_MILLIS, buf.readVarLong()), buf.readUtf(MAX_RULE), buf.readUtf(MAX_NOTE));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ModerationActionPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer moderator) apply(moderator, payload);
        });
    }

    private static void apply(ServerPlayer moderator, ModerationActionPacket p) {
        boolean strict = switch (p.action) {
            case BAN, UNBAN, SET_WHITELIST, SET_WHITELIST_MESSAGE -> true;
            default -> false;
        };
        if (strict ? !ModerationPermissions.canBan(moderator)
                : !ModerationPermissions.canModerate(moderator)) {
            return;
        }

        String moderatorName = moderator.getGameProfile().getName();
        String rules = knownRules(p.rule);
        ServerPlayer online = moderator.server.getPlayerList().getPlayer(p.target);
        String targetName = nameOf(p.target, online);

        switch (p.action) {
            case MUTE -> {
                if (online == null) { offline(moderator, targetName); return; }
                ModerationService.mute(moderator, online, p.millis, rules, p.note);
                tell(moderator, Component.translatable("msg.roll_mod.mute.muted", targetName,
                        duration(p.millis)));
            }
            case UNMUTE -> tell(moderator, ModerationService.unmute(moderator, p.target, targetName)
                    ? Component.translatable("msg.roll_mod.mute.unmuted", targetName)
                    : Component.translatable("msg.roll_mod.mute.notMuted", targetName));
            case WARN -> {
                if (online == null) { offline(moderator, targetName); return; }
                if (online == moderator) return;
                int count = ModerationService.warn(moderator, online, rules, p.note);
                tell(moderator, Component.translatable("msg.roll_mod.moderation.warnedBy",
                        targetName, count));
                if (count >= ModerationService.WARNS_BEFORE_BAN) {
                    tell(moderator, Component.translatable("msg.roll_mod.moderation.autoBanned",
                            targetName).withStyle(ChatFormatting.GOLD));
                }
            }
            case BAN -> {
                // Not yourself: the one ban the GUI can issue that nobody could then lift from it.
                if (p.target.equals(moderator.getUUID()) || p.target.equals(NONE)) return;
                ModerationService.ban(moderator.server, moderatorName, p.target, targetName,
                        p.millis, rules, p.note);
                tell(moderator, Component.translatable("msg.roll_mod.moderation.bannedBy",
                        targetName, duration(p.millis)));
            }
            case UNBAN -> tell(moderator, ModerationService.unban(moderator, p.target, targetName)
                    ? Component.translatable("msg.roll_mod.moderation.unbanned", targetName)
                    : Component.translatable("msg.roll_mod.moderation.notBanned", targetName));
            case SET_WHITELIST -> {
                boolean close = p.millis != 0L;
                if (close == WhitelistStore.enabled()) return;
                ModerationService.setWhitelist(moderator.server, moderatorName, close);
            }
            case SET_WHITELIST_MESSAGE -> {
                if (p.note.isBlank()) return;
                WhitelistStore.setKickMessage(p.note);
                ModerationLog.append(moderatorName, "set the closed-server message", "", p.note);
                tell(moderator, Component.translatable("msg.roll_mod.moderation.whitelist.message",
                        ModerationService.whitelistScreen()));
            }
        }
    }

    /**
     * The target's name from the server's own records, never from the packet: online first, then the
     * name a ban or a warning was filed under.
     */
    private static String nameOf(UUID id, ServerPlayer online) {
        if (online != null) return online.getGameProfile().getName();
        BanStore.Ban ban = BanStore.ban(id);
        if (ban != null && !ban.name().isBlank()) return ban.name();
        String warned = WarnStore.name(id);
        if (warned != null && !warned.isBlank()) return warned;
        return id.toString();
    }

    private static String duration(long millis) {
        return millis <= 0L
                ? Component.translatable("msg.roll_mod.moderation.permanent").getString()
                : Durations.format(millis);
    }

    private static void offline(ServerPlayer moderator, String name) {
        tell(moderator, Component.translatable("msg.roll_mod.moderation.offline", name)
                .withStyle(ChatFormatting.RED));
    }

    private static void tell(ServerPlayer moderator, Component message) {
        moderator.sendSystemMessage(message);
    }
}
