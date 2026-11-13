package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.registry.ModConfigs;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Locale;

/**
 * Client → server: one server setting changed from the moderation tab's Settings mode — the switches for what was
 * ported from Eden_LD (join history, mentions) and the chat's other knobs. Only for staff who may ban
 * ({@link ModerationPermissions#canBan}); anyone else is ignored. Written to {@code MyConfig} and saved.
 *
 * @param key   which setting, one of {@link Key}
 * @param value its new value, as text
 */
public record ServerSettingPacket(String key, String value) implements CustomPacketPayload {

    public static final Type<ServerSettingPacket> TYPE = new Type<>(RollMod.id("server_setting"));

    public static final StreamCodec<FriendlyByteBuf, ServerSettingPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.key, 64);
                buf.writeUtf(p.value, 256);
            },
            buf -> new ServerSettingPacket(buf.readUtf(64), buf.readUtf(256)));

    /** The settings the tab offers. */
    public enum Key {
        JOIN_HISTORY_MINUTES, JOIN_HISTORY_LINES, MENTIONS, HIDE_ADVANCEMENTS, DISCORD_MARKER, WIXIE_MINUTES;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static ServerSettingPacket of(Key key, Object value) {
        return new ServerSettingPacket(key.name(), String.valueOf(value));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerSettingPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !ModerationPermissions.canBan(player)) return;
            MyConfig config = ModConfigs.MAIN;
            if (config == null) return;
            try {
                apply(config, Key.valueOf(payload.key), payload.value);
            } catch (IllegalArgumentException e) {
                return;
            }
            ConfigApi.save(config);
            RollMod.LOGGER.info("[Moderation] {} set {} to {}", player.getGameProfile().getName(), payload.key, payload.value);
        });
    }

    /** Writes {@code value} into {@code config}; also used on the client to show the change at once. */
    public static void apply(MyConfig config, Key key, String value) {
        switch (key) {
            case JOIN_HISTORY_MINUTES -> config.chat_changes.joinHistoryMinutes.validateAndSet(clamp(value, 0, 24 * 60));
            case JOIN_HISTORY_LINES -> config.chat_changes.joinHistoryLines.validateAndSet(clamp(value, 0, 500));
            case MENTIONS -> config.chat_changes.mentions = Boolean.parseBoolean(value);
            case HIDE_ADVANCEMENTS -> config.chat_changes.hideAdvancementMessages = Boolean.parseBoolean(value);
            case DISCORD_MARKER -> config.chat_changes.discordLineMarker = value.strip();
            // Ticks between Wixie's tips, given in minutes; never under one, or it would post every tick.
            case WIXIE_MINUTES -> config.messages.TIME_TO_BROADCAST = clamp(value, 1, 24 * 60) * 1200;
        }
    }

    private static int clamp(String value, int min, int max) {
        int parsed;
        try {
            parsed = Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
        return Math.max(min, Math.min(max, parsed));
    }
}
