package com.roll_54.roll_mod.cosmetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One player's cosmetic state, stored as an attachment on the player.
 *
 * <p>Deliberately split in two rather than kept as a single nested map. {@link #active} is the only
 * half other clients need in order to draw this player, and it is read on the render hot path, so
 * it stays as small and flat as possible. {@link #unlocked} is only ever read by the owner's hub
 * and never leaves them — a client physically cannot see another player's unlock list, the same
 * principle the homes feature uses with its separate view record.
 *
 * @param unlocked skin ids this player owns
 * @param active   slot → skin id, at most one skin rendering per slot. Keyed by slot rather than by
 *                 item id so that choosing Murasama once covers every sword the player will ever
 *                 hold, including ones from mods that did not exist when they chose it.
 */
public record PlayerItemSkins(Set<ResourceLocation> unlocked, Map<SkinCategory, ResourceLocation> active) {

    public static final PlayerItemSkins EMPTY = new PlayerItemSkins(Set.of(), Map.of());

    public static final Codec<PlayerItemSkins> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("unlocked", List.of())
                    .forGetter(skins -> List.copyOf(skins.unlocked())),
            // Plain optionalFieldOf (not the strict variant) is deliberate: this map used to be keyed
            // by item id, and those old keys cannot decode as a SkinCategory. Lenient decoding turns
            // a pre-rework attachment into an empty one instead of failing the player's login.
            Codec.unboundedMap(SkinCategory.CODEC, ResourceLocation.CODEC).optionalFieldOf("active", Map.of())
                    .forGetter(PlayerItemSkins::active)
    ).apply(instance, (unlocked, active) -> new PlayerItemSkins(Set.copyOf(unlocked), active)));

    public PlayerItemSkins {
        unlocked = Set.copyOf(unlocked);
        active = Collections.unmodifiableMap(mutableCopy(active));
    }

    /**
     * {@link Map#copyOf} would lose the enum ordering, and this map is iterated on the render path.
     *
     * <p>Always built through {@code putAll} rather than {@link EnumMap#EnumMap(Map)}: that copy
     * constructor cannot infer the key type from an empty non-{@code EnumMap}, and throws.
     */
    private static Map<SkinCategory, ResourceLocation> mutableCopy(Map<SkinCategory, ResourceLocation> source) {
        EnumMap<SkinCategory, ResourceLocation> copy = new EnumMap<>(SkinCategory.class);
        copy.putAll(source);
        return copy;
    }

    public boolean hasUnlocked(ResourceLocation skinId) {
        return this.unlocked.contains(skinId);
    }

    @Nullable
    public ResourceLocation activeFor(SkinCategory category) {
        return this.active.get(category);
    }

    public PlayerItemSkins withUnlocked(ResourceLocation skinId) {
        if (this.unlocked.contains(skinId)) {
            return this;
        }
        Set<ResourceLocation> next = new LinkedHashSet<>(this.unlocked);
        next.add(skinId);
        return new PlayerItemSkins(next, this.active);
    }

    public PlayerItemSkins withoutUnlocked(ResourceLocation skinId) {
        if (!this.unlocked.contains(skinId)) {
            return this;
        }
        Set<ResourceLocation> nextUnlocked = new LinkedHashSet<>(this.unlocked);
        nextUnlocked.remove(skinId);
        // A skin that is no longer owned must stop rendering, or revoking it would be cosmetic only.
        Map<SkinCategory, ResourceLocation> nextActive = mutableCopy(this.active);
        nextActive.values().removeIf(skinId::equals);
        return new PlayerItemSkins(nextUnlocked, nextActive);
    }

    public PlayerItemSkins withActive(SkinCategory category, ResourceLocation skinId) {
        if (skinId.equals(this.active.get(category))) {
            return this;
        }
        Map<SkinCategory, ResourceLocation> next = mutableCopy(this.active);
        next.put(category, skinId);
        return new PlayerItemSkins(this.unlocked, next);
    }

    public PlayerItemSkins withoutActive(SkinCategory category) {
        if (!this.active.containsKey(category)) {
            return this;
        }
        Map<SkinCategory, ResourceLocation> next = mutableCopy(this.active);
        next.remove(category);
        return new PlayerItemSkins(this.unlocked, next);
    }
}
