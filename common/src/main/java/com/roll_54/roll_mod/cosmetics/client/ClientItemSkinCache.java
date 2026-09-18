package com.roll_54.roll_mod.cosmetics.client;

import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import com.roll_54.roll_mod.cosmetics.SkinCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What the client knows about everyone's cosmetic skins.
 *
 * <p>Lives in {@code common} rather than the client jar for the same reason
 * {@code ClientHomeCache} does: the hub tab that reads it is common code.
 *
 * <p>Written from the netty thread and read from the render thread. Rather than lock, every update
 * swaps in a whole new immutable map, so a reader always sees one consistent snapshot — the same
 * approach the homes and warps caches take.
 */
public final class ClientItemSkinCache {
    private ClientItemSkinCache() {}

    /**
     * player → slot → skin. Keyed by slot rather than by item, so one choice covers every sword in
     * the pack; the held item is mapped to its slot at lookup time by {@link SkinCategory#of}, which
     * memoises, so the render path still does no registry work.
     */
    private static volatile Map<UUID, Map<SkinCategory, ResourceLocation>> active = Map.of();

    /** This client's own unlocked skins. Never populated for anyone else. */
    private static volatile Set<ResourceLocation> unlocked = Set.of();

    /**
     * Hoisted out of {@link #activeFor} so the render hot path can bail on a single volatile read
     * when nobody in the session is wearing a skin — which is the overwhelmingly common case.
     */
    private static volatile boolean anyPlayerHasSkins;

    public static boolean anyPlayerHasSkins() {
        return anyPlayerHasSkins;
    }

    /** An empty {@code skins} map removes the player, which is how logout is broadcast. */
    public static void put(UUID player, Map<SkinCategory, ResourceLocation> skins) {
        Map<UUID, Map<SkinCategory, ResourceLocation>> next = new HashMap<>(active);
        if (skins.isEmpty()) {
            next.remove(player);
        } else {
            EnumMap<SkinCategory, ResourceLocation> copy = new EnumMap<>(SkinCategory.class);
            copy.putAll(skins);
            next.put(player, Collections.unmodifiableMap(copy));
        }
        active = Map.copyOf(next);
        anyPlayerHasSkins = !next.isEmpty();
    }

    /**
     * The skin {@code player} is rendering on {@code item}, or {@code null}. Signature unchanged by
     * the move to slots, so {@code ItemSkinResolver} needs no edit.
     */
    @Nullable
    public static ResourceLocation activeFor(UUID player, Item item) {
        Map<SkinCategory, ResourceLocation> skins = active.get(player);
        return skins == null ? null : ItemSkinRegistry.resolveActive(skins, item);
    }

    public static Map<SkinCategory, ResourceLocation> activeFor(UUID player) {
        return active.getOrDefault(player, Map.of());
    }

    public static void setUnlocked(Set<ResourceLocation> skins) {
        unlocked = Set.copyOf(skins);
    }

    public static Set<ResourceLocation> unlocked() {
        return unlocked;
    }

    /** On disconnect, so a new session does not inherit the last one's players. */
    public static void clear() {
        active = Map.of();
        unlocked = Set.of();
        anyPlayerHasSkins = false;
    }
}
