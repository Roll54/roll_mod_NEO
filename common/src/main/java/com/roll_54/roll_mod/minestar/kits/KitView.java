package com.roll_54.roll_mod.minestar.kits;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * One kit as the hub sees it: enough to draw a row, and nothing the client has no business knowing.
 *
 * <p>The whole item list would be a packet per kit per resync for something the player only sees on
 * hover, so this carries the first stack as an icon and the count of the rest.
 */
public record KitView(String name, ItemStack icon, int itemCount, long remainingMillis) {

    public static KitView of(Kit kit, long remainingMillis) {
        ItemStack icon = kit.items().isEmpty() ? ItemStack.EMPTY : kit.items().get(0).copy();
        return new KitView(kit.name(), icon, kit.items().size(), remainingMillis);
    }

    public boolean ready() {
        return remainingMillis <= 0;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(name, 32);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, icon);
        buf.writeVarInt(itemCount);
        buf.writeVarLong(Math.max(0L, remainingMillis));
    }

    public static KitView decode(RegistryFriendlyByteBuf buf) {
        return new KitView(buf.readUtf(32), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                buf.readVarInt(), buf.readVarLong());
    }
}
