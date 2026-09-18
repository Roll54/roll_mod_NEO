package com.roll_54.roll_mod.network.packet.skin;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.cosmetics.ItemSkinService;
import com.roll_54.roll_mod.cosmetics.SkinCategory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: the player picked a skin for a slot in the hub, or cleared one.
 *
 * <p>Carries no authority. {@link ItemSkinService#setActive} re-checks ownership, and derives the
 * slot from the skin's own definition rather than trusting {@link #category} — which is only read
 * for {@link Action#CLEAR}, where there is no skin to derive it from. A hand-crafted packet
 * therefore gets a player nothing they could not already do through the UI.
 */
public record SkinActionPacket(Action action, SkinCategory category, ResourceLocation skinId)
        implements CustomPacketPayload {

    public enum Action { SET, CLEAR }

    public static final Type<SkinActionPacket> TYPE = new Type<>(RollMod.id("skin_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkinActionPacket> STREAM_CODEC =
            StreamCodec.of(SkinActionPacket::encode, SkinActionPacket::decode);

    /** Clearing a slot needs no skin id, but the record still has to carry something. */
    public static SkinActionPacket clear(SkinCategory category) {
        return new SkinActionPacket(Action.CLEAR, category, RollMod.id("none"));
    }

    public static SkinActionPacket set(SkinCategory category, ResourceLocation skinId) {
        return new SkinActionPacket(Action.SET, category, skinId);
    }

    private static void encode(RegistryFriendlyByteBuf buf, SkinActionPacket packet) {
        buf.writeByte(packet.action.ordinal());
        SkinCategory.STREAM_CODEC.encode(buf, packet.category);
        buf.writeResourceLocation(packet.skinId);
    }

    private static SkinActionPacket decode(RegistryFriendlyByteBuf buf) {
        // Modulo rather than an index, so a malformed byte picks a valid action instead of throwing
        // out of the decoder — the convention the warp and home packets already follow.
        Action action = Action.values()[Math.floorMod(buf.readByte(), Action.values().length)];
        return new SkinActionPacket(action, SkinCategory.STREAM_CODEC.decode(buf), buf.readResourceLocation());
    }

    public static void handle(SkinActionPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action) {
                case SET -> ItemSkinService.setActive(player, payload.skinId);
                case CLEAR -> ItemSkinService.clearActive(player, payload.category);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
