package com.roll_54.roll_mod.minestar.kits;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * One kit as the hub sees it: enough to draw a card, and nothing the client has no business knowing.
 *
 * <p>The whole item list would be a packet per kit per resync for something the player only sees on
 * hover, so this carries the first stack as an icon and the count of the rest.
 *
 * <p>As on Eden_LD: {@code remainingMillis} is {@code -1} for a one-time kit already claimed; {@code locked}
 * kits are shown but cannot be claimed (the player lacks their permission); {@code cooldownTicks} below zero is a
 * one-time kit, zero a kit that can be claimed any time.
 */
public record KitView(String name, ItemStack icon, int itemCount, long remainingMillis, String title,
                      String description, long cooldownTicks, boolean locked) {

    public static KitView of(Kit kit, long remainingMillis) {
        return of(kit, remainingMillis, false);
    }

    public static KitView of(Kit kit, long remainingMillis, boolean locked) {
        ItemStack icon = kit.items().isEmpty() ? ItemStack.EMPTY : kit.items().get(0).copy();
        return new KitView(kit.name(), icon, kit.items().size(), remainingMillis, kit.title(), kit.description(),
                kit.cooldownTicks(), locked);
    }

    public boolean ready() {
        return !locked && remainingMillis == 0;
    }

    /** A one-time kit this player has had. */
    public boolean claimedForGood() {
        return remainingMillis < 0;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(name, 32);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, icon);
        buf.writeVarInt(itemCount);
        buf.writeLong(remainingMillis < 0 ? -1L : remainingMillis);
        buf.writeUtf(title, 128);
        buf.writeUtf(description, Kit.MAX_DESCRIPTION * 2);
        buf.writeLong(cooldownTicks);
        buf.writeBoolean(locked);
    }

    public static KitView decode(RegistryFriendlyByteBuf buf) {
        return new KitView(buf.readUtf(32), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                buf.readVarInt(), buf.readLong(), buf.readUtf(128), buf.readUtf(Kit.MAX_DESCRIPTION * 2),
                buf.readLong(), buf.readBoolean());
    }
}
