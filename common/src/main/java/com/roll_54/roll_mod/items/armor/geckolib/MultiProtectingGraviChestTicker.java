package com.roll_54.roll_mod.items.armor.geckolib;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Drives {@link MultiProtectingGraviChestItem}'s per-tick energy drain and boost for the players who
 * actually need it. Instead of every copy of the item ticking in every inventory, players are tracked
 * in {@link #ACTIVE} while their chest slot holds the chestplate with at least one state switched on,
 * and only they are ticked.
 *
 * <p>Membership is refreshed whenever the chest slot changes — that includes toggling a state, since
 * the states are item components and vanilla compares stacks with their components — on login and on
 * respawn, and dropped on logout. The tick re-checks the slot as well, so a stale entry removes itself.
 * Server thread only.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class MultiProtectingGraviChestTicker {

    private static final Map<UUID, ServerPlayer> ACTIVE = new HashMap<>();

    private MultiProtectingGraviChestTicker() {}

    /** Adds or removes the player depending on what is in their chest slot right now. */
    public static void refresh(ServerPlayer player) {
        if (isActive(player.getItemBySlot(EquipmentSlot.CHEST))) {
            ACTIVE.put(player.getUUID(), player);
        } else {
            ACTIVE.remove(player.getUUID());
        }
    }

    private static boolean isActive(ItemStack stack) {
        return stack.getItem() instanceof MultiProtectingGraviChestItem item
                && (item.isActivatedById(stack, 0) || item.isActivatedById(stack, 1) || item.isActivatedById(stack, 2));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<ServerPlayer> it = ACTIVE.values().iterator();
        while (it.hasNext()) {
            ServerPlayer player = it.next();
            ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (player.isRemoved() || !isActive(stack)) {
                it.remove();
                continue;
            }
            ((MultiProtectingGraviChestItem) stack.getItem()).wornTick(stack, player);
        }
    }

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.CHEST && event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // Respawning builds a new ServerPlayer; point the entry at it.
        if (event.getEntity() instanceof ServerPlayer player) {
            refresh(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }
}
