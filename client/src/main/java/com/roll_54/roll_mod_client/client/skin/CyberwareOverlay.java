package com.roll_54.roll_mod_client.client.skin;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * One cyberware -> player-overlay binding: when {@code item} is installed in {@code slot}, the
 * {@code region}'s model parts are re-rendered with the wide/slim texture. The item is held as a
 * {@link Supplier} because {@code CyberwareSkinLayer} lives client-side while the items are registered
 * in the common module.
 */
public record CyberwareOverlay(Supplier<? extends Item> item,
                               CyberwareSlot slot,
                               BodyRegion region,
                               ResourceLocation wide,
                               ResourceLocation slim) {

    /** Texture for the player's skin model variant (slim = 3px arms, wide = 4px arms). */
    public ResourceLocation texture(PlayerSkin.Model model) {
        return model == PlayerSkin.Model.SLIM ? slim : wide;
    }
}
