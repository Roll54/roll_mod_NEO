package com.roll_54.roll_mod_client.client.skin.item;

import com.roll_54.roll_mod.cosmetics.client.ClientItemSkinCache;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Decides whether a stack about to be drawn should be drawn as something else.
 *
 * <p>This sits on one of the hottest paths in the client — {@code ItemRenderer#renderStatic} runs
 * for every held item on every visible player, every item frame and most world rendering — so it is
 * written as a chain of early-outs ordered cheapest first. A session where nobody owns a skin costs
 * a single volatile boolean read per call.
 */
public final class ItemSkinResolver {
    private ItemSkinResolver() {}

    /**
     * The skin model to draw instead of {@code stack}'s own, or {@code null} to leave rendering
     * alone.
     *
     * <p>{@code holder} being non-null is what ties a skin to a player: both hand paths supply it
     * ({@code ItemInHandRenderer} in first person, {@code ItemInHandLayer} in third), which is
     * exactly why other players see the skin. Contexts with no holder — item frames, dropped
     * stacks — fall through to the real item, which is the sensible reading of a *player's*
     * cosmetic anyway.
     */
    @Nullable
    public static BakedModel resolve(@Nullable LivingEntity holder, ItemStack stack) {
        if (!ClientItemSkinCache.anyPlayerHasSkins()) {
            return null;
        }
        if (!(holder instanceof AbstractClientPlayer player)) {
            return null;
        }
        ResourceLocation skinId = ClientItemSkinCache.activeFor(player.getUUID(), stack.getItem());
        if (skinId == null) {
            return null;
        }
        return BakedSkinModels.get(skinId);
    }
}
