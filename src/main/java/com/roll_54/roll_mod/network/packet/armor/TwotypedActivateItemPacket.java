package com.roll_54.roll_mod.network.packet.armor;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.ByteBufCodecs;
import com.roll_54.roll_mod.data.TwoStateToggleableItem;

public record TwotypedActivateItemPacket(EquipmentSlot slot, int activationType, boolean activated) implements CustomPacketPayload {
    public static final Type<TwotypedActivateItemPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "twotype_activate_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TwotypedActivateItemPacket> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(EquipmentSlot.class),
            TwotypedActivateItemPacket::slot,
            ByteBufCodecs.INT,
            TwotypedActivateItemPacket::activationType,
            ByteBufCodecs.BOOL,
            TwotypedActivateItemPacket::activated,
            TwotypedActivateItemPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handle(TwotypedActivateItemPacket payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            ItemStack item = player.getItemBySlot(payload.slot);
            if (item.getItem() instanceof TwoStateToggleableItem toggleable) {
                switch (payload.activationType) {
                    case 1 -> toggleable.setActivatedFirst(player, item, payload.activated);
                    case 2 -> toggleable.setActivatedSecond(player, item, payload.activated);
                    default -> {
                    }
                }
            }
        });
    }
}
