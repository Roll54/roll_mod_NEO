package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.home.HomeService;
import com.roll_54.roll_mod.minestar.hub.home.PlayerHome;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Client → server: everything the homes tab can ask for. Sent optimistically — every rule is
 * re-checked in {@link HomeService}, so a spoofed packet achieves nothing.
 *
 * @param home the target, unused by {@link Action#CREATE}
 * @param text the home's name for {@code CREATE}, the invitee's nickname for {@code INVITE}, the
 *     FTB home's name for {@code MIGRATE}, empty otherwise. One field rather than two because no
 *     action ever needs both.
 */
public record HomeActionPacket(Action action, UUID home, String text)
        implements CustomPacketPayload {

    public enum Action { CREATE, DELETE, TELEPORT, INVITE, ACCEPT, DECLINE, LEAVE, MIGRATE }

    public static final Type<HomeActionPacket> TYPE = new Type<>(RollMod.id("home_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HomeActionPacket> STREAM_CODEC =
            StreamCodec.of(HomeActionPacket::encode, HomeActionPacket::decode);

    private static final UUID NONE = new UUID(0, 0);

    /**
     * Text is clamped here rather than trusted from the caller: the create form's field has no
     * length cap, and {@code writeUtf} throws when the string is over the limit — which colour codes
     * make far easier to hit, since each one spends two of the twenty-four characters. The server
     * trims again in {@code PlayerHome.at}; this only keeps the encoder from failing.
     */
    private static String clamp(String text) {
        String clean = text == null ? "" : text.strip();
        return clean.length() <= PlayerHome.MAX_NAME ? clean
                : clean.substring(0, PlayerHome.MAX_NAME);
    }

    /** Creating with the name of an existing home moves that one here instead. */
    public static HomeActionPacket create(String name) {
        return new HomeActionPacket(Action.CREATE, NONE, clamp(name));
    }

    public static HomeActionPacket delete(UUID home) {
        return new HomeActionPacket(Action.DELETE, home, "");
    }

    public static HomeActionPacket teleport(UUID home) {
        return new HomeActionPacket(Action.TELEPORT, home, "");
    }

    public static HomeActionPacket invite(UUID home, String nickname) {
        return new HomeActionPacket(Action.INVITE, home, clamp(nickname));
    }

    public static HomeActionPacket accept(UUID home) {
        return new HomeActionPacket(Action.ACCEPT, home, "");
    }

    public static HomeActionPacket decline(UUID home) {
        return new HomeActionPacket(Action.DECLINE, home, "");
    }

    public static HomeActionPacket leave(UUID home) {
        return new HomeActionPacket(Action.LEAVE, home, "");
    }

    /**
     * Brings an FTB Essentials home across. Addressed by name, not by id: an FTB home has no
     * {@code PlayerHome} and so no real id — the one its row carries is synthesised for the list's
     * benefit and resolves to nothing on the server.
     */
    public static HomeActionPacket migrate(String ftbName) {
        return new HomeActionPacket(Action.MIGRATE, NONE, clamp(ftbName));
    }

    private static void encode(RegistryFriendlyByteBuf buf, HomeActionPacket packet) {
        buf.writeVarInt(packet.action.ordinal());
        buf.writeUUID(packet.home);
        buf.writeUtf(packet.text, PlayerHome.MAX_NAME);
    }

    private static HomeActionPacket decode(RegistryFriendlyByteBuf buf) {
        Action action = Action.values()[buf.readVarInt() % Action.values().length];
        return new HomeActionPacket(action, buf.readUUID(), buf.readUtf(PlayerHome.MAX_NAME));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HomeActionPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action) {
                case CREATE -> HomeService.create(player, payload.text);
                case DELETE -> HomeService.delete(player, payload.home);
                case TELEPORT -> HomeService.teleport(player, payload.home);
                case INVITE -> HomeService.invite(player, payload.home, payload.text);
                case ACCEPT -> HomeService.accept(player, payload.home);
                case DECLINE -> HomeService.decline(player, payload.home);
                case LEAVE -> HomeService.leave(player, payload.home);
                case MIGRATE -> HomeService.migrate(player, payload.text);
            }
        });
    }
}
