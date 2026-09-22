package com.roll_54.roll_mod.minestar.kits;

import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import com.roll_54.roll_mod.util.Durations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Who may take which kit, and when they may take it again.
 *
 * <p>Every rule lives here rather than in the command or the packet handler, so the hub can ask the
 * same questions before it offers a button — the shape {@code WarpService} set.
 */
public final class KitService {

    private KitService() {}

    /* --------------------------------------------- access ------------------------------------------- */

    /**
     * Whether this player may claim this kit.
     *
     * <p>Without LuckPerms nobody holds any node, which would hide every kit — useless in a
     * single-player world or a dev run — so an operator still gets them all.
     */
    public static boolean canUse(ServerPlayer player, Kit kit) {
        if (!LuckPermsCompat.LOADED) return player.hasPermissions(2);
        return player.hasPermissions(2) || LuckPermsCompat.canUseKit(player, kit.name());
    }

    /** The kits this player may see, in file order. */
    public static List<Kit> visibleTo(ServerPlayer player) {
        List<Kit> visible = new ArrayList<>();
        for (Kit kit : KitStore.all(player.server)) {
            if (canUse(player, kit)) visible.add(kit);
        }
        return visible;
    }

    /* -------------------------------------------- cooldown ------------------------------------------ */

    /**
     * The wait on this kit, in ticks — the kit file's value, for everybody.
     *
     * <p>No rank shortens it, no permission waives it, and an operator is not exempt either. The
     * cooldown is the only thing standing between a kit and an infinite supply of what is in it, so
     * a way around it would be a way around the kit. A rank that should wait less wants a kit of
     * its own, with its own node and its own cooldown.
     */
    public static long cooldownTicks(Kit kit) {
        return kit.cooldownTicks();
    }

    /** Milliseconds before this player may claim this kit again; {@code 0} when it is ready. */
    public static long remainingMillis(ServerPlayer player, Kit kit) {
        long ticks = cooldownTicks(kit);
        if (ticks <= 0) return 0L;

        long last = KitCooldowns.lastClaimed(player.getUUID(), kit.name());
        if (last <= 0) return 0L;

        return Math.max(0L, ticks * 50L - (System.currentTimeMillis() - last));
    }

    /* --------------------------------------------- claim -------------------------------------------- */

    /**
     * Hands the kit over, if the player may have it.
     *
     * <p>What will not fit is dropped at their feet rather than refused: a player who claims a kit
     * with a full inventory means to have it, and losing half of it to a silent refusal is worse
     * than a pile of items on the floor.
     *
     * @return whether anything was given.
     */
    public static boolean claim(ServerPlayer player, String name) {
        Kit kit = KitStore.byName(player.server, name);
        if (kit == null) {
            refuse(player, "msg.roll_mod.kit.unknown", name);
            return false;
        }
        if (!canUse(player, kit)) {
            refuse(player, "msg.roll_mod.kit.denied", kit.name());
            return false;
        }

        long remaining = remainingMillis(player, kit);
        if (remaining > 0) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.cooldown",
                    kit.name(), Durations.format(remaining))
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        for (ItemStack stack : kit.items()) {
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy) && !copy.isEmpty()) {
                player.drop(copy, false);
            }
        }

        KitCooldowns.claimed(player.getUUID(), kit.name(), System.currentTimeMillis());
        player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.claimed", kit.name()));
        KitViewers.resync(player.server);
        return true;
    }

    /* --------------------------------------------- create ------------------------------------------- */

    /**
     * Captures the creator's inventory as a kit.
     *
     * <p>Main inventory and hotbar only, as FTB Essentials did — armour and the offhand are what the
     * operator is wearing while they build the kit, not part of it.
     */
    public static boolean create(ServerPlayer player, String rawName, long cooldownTicks) {
        String name = Kit.clampName(rawName);
        if (!Kit.validName(name)) {
            refuse(player, "msg.roll_mod.kit.badName", rawName);
            return false;
        }

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) items.add(stack.copy());
        }
        if (items.isEmpty()) {
            refuse(player, "msg.roll_mod.kit.empty", name);
            return false;
        }

        KitStore.put(player.server, new Kit(name, Math.max(0L, cooldownTicks), "", List.copyOf(items)));
        // A recreated kit is a different kit; carrying the old waits over would be a trap.
        KitCooldowns.forget(name);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.created",
                name, items.size(), cooldownTicks));
        KitViewers.resync(player.server);
        return true;
    }

    public static boolean delete(ServerPlayer player, String rawName) {
        String name = Kit.clampName(rawName);
        if (!KitStore.remove(player.server, name)) {
            refuse(player, "msg.roll_mod.kit.unknown", rawName);
            return false;
        }
        KitCooldowns.forget(name);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.kit.deleted", name));
        KitViewers.resync(player.server);
        return true;
    }

    private static void refuse(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED));
    }
}
