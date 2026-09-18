package com.roll_54.roll_mod_client.client.skin.item;

import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import com.roll_54.roll_mod.cosmetics.SkinKind;
import com.roll_54.roll_mod.cosmetics.client.ClientItemSkinCache;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Picks the GeckoLib renderer for whatever helmet skin a player is wearing.
 *
 * <p>Called from every skinnable helmet's {@code GeoRenderProvider}, which GeckoLib consults once
 * per armour piece per frame — so it opens with the same cheap early-out as the item path. Returning
 * {@code null} is a clean no-op: GeckoLib's {@code InternalUtil} treats it as "not mine" and lets
 * vanilla draw the armour exactly as before.
 */
public final class HelmetSkinRenderers {
    private HelmetSkinRenderers() {}

    private static final Map<ResourceLocation, SkinArmorRenderer> RENDERERS = new ConcurrentHashMap<>();

    /**
     * The renderer for {@code wearer}'s active skin on {@code stack}, or {@code null} to leave the
     * armour alone.
     *
     * <p>One renderer per skin, shared across every player wearing it. That is safe because
     * GeckoLib calls {@code prepForRender} to bind the entity, stack and slot immediately before
     * each draw, and armour rendering is single-threaded.
     */
    @Nullable
    public static SkinArmorRenderer forWearer(@Nullable LivingEntity wearer, ItemStack stack) {
        if (!ClientItemSkinCache.anyPlayerHasSkins()) {
            return null;
        }
        if (!(wearer instanceof AbstractClientPlayer player)) {
            return null;
        }
        ResourceLocation skinId = ClientItemSkinCache.activeFor(player.getUUID(), stack.getItem());
        if (skinId == null) {
            return null;
        }
        ItemSkinDefinition skin = ItemSkinRegistry.get(skinId);
        if (skin == null || skin.kind() != SkinKind.GEO_ARMOR) {
            return null;
        }
        return RENDERERS.computeIfAbsent(skinId, id -> new SkinArmorRenderer(skin));
    }

    /**
     * Drops every cached renderer.
     *
     * <p>{@code GeoArmorRenderer}'s constructor bakes {@code ModelLayers.PLAYER_INNER_ARMOR}, so a
     * cached renderer holds {@code ModelPart}s belonging to the previous {@code EntityModelSet}. They
     * have to go whenever the model set is rebuilt, or a resource reload leaves the skin drawing
     * against stale geometry.
     */
    public static void clear() {
        RENDERERS.clear();
    }
}
