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
import com.roll_54.roll_mod.data.TriStateToggleAbleItem;

public record MultiProtectingGraviChestItemPacket(EquipmentSlot slot, int activationType, boolean activated) implements CustomPacketPayload {
    public static final Type<MultiProtectingGraviChestItemPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "twotype_activate_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MultiProtectingGraviChestItemPacket> STREAM_CODEC = StreamCodec.composite(
            NeoForgeStreamCodecs.enumCodec(EquipmentSlot.class),
            MultiProtectingGraviChestItemPacket::slot,
            ByteBufCodecs.INT,
            MultiProtectingGraviChestItemPacket::activationType,
            ByteBufCodecs.BOOL,
            MultiProtectingGraviChestItemPacket::activated,
            MultiProtectingGraviChestItemPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handle(MultiProtectingGraviChestItemPacket payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            ItemStack item = player.getItemBySlot(payload.slot);
            if (item.getItem() instanceof TriStateToggleAbleItem toggleable) {
                switch (payload.activationType) {
                    case 0 -> toggleable.setActivatedById(item, 0, payload.activated);
                    case 1 -> toggleable.setActivatedById(item, 1, payload.activated);
                    case 2 -> toggleable.setActivatedById(item, 2, payload.activated);
                    default -> {
                    }
                }
            }
        });
    }
}
