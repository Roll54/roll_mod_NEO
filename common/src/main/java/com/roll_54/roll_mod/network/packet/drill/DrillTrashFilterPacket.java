package com.roll_54.roll_mod.network.packet.drill;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleHelper;
import com.roll_54.roll_mod.items.modulardrill.ModularDrillItem;
import com.roll_54.roll_mod.items.modulardrill.ModuleType;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Client → server: the Trash Filter's ghost slots, sent once when the drill config GUI closes —
 * the same close-time commit as {@link DrillVolumePacket}. Slot order is kept; empty slots are
 * {@link ItemStack#EMPTY}. The server keeps only item and components at count 1, so the client
 * cannot conjure anything real through it.
 */
public record DrillTrashFilterPacket(List<ItemStack> slots) implements CustomPacketPayload {

    public static final Type<DrillTrashFilterPacket> TYPE = new Type<>(RollMod.id("drill_trash_filter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DrillTrashFilterPacket> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC
                    .apply(ByteBufCodecs.list(DrillModuleHelper.TRASH_FILTER_SLOTS))
                    .map(DrillTrashFilterPacket::new, DrillTrashFilterPacket::slots);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DrillTrashFilterPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            ItemStack stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof ModularDrillItem)) return;
            if (!DrillModuleHelper.get(stack).has(ModuleType.TRASH_FILTER)) return;

            List<ItemStack> slots = new ArrayList<>(DrillModuleHelper.TRASH_FILTER_SLOTS);
            for (int i = 0; i < DrillModuleHelper.TRASH_FILTER_SLOTS; i++) {
                ItemStack ghost = i < payload.slots().size() ? payload.slots().get(i) : ItemStack.EMPTY;
                slots.add(ghost.isEmpty() ? ItemStack.EMPTY : ghost.copyWithCount(1));
            }
            ItemContainerContents filter = ItemContainerContents.fromItems(slots);

            if (filter.equals(stack.getOrDefault(ComponentsRegistry.TRASH_FILTER.get(), ItemContainerContents.EMPTY))) {
                return;
            }
            stack.set(ComponentsRegistry.TRASH_FILTER.get(), filter);
        });
    }
}
