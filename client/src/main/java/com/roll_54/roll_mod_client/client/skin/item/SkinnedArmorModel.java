package com.roll_54.roll_mod_client.client.skin.item;

import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.items.armor.geckolib.SkinAnchorItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * Points GeckoLib at one cosmetic skin's geo model and texture.
 *
 * <p>Parameterised over {@link SkinAnchorItem} rather than over any armour class. javac emits erasure
 * bridges for the {@code T}-typed methods below that {@code checkcast} to that parameter, while
 * {@code GeoArmorRenderer#prepForRender} stores {@code stack.getItem()} unchecked — so the type here
 * has to be the type {@link SkinArmorRenderer} pins as its animatable, not the type of the helmet
 * being worn. Naming a real armour class here is what made wearing the clown hat with a skin throw
 * {@code ClassCastException}.
 *
 * <p>Both overloads of each getter are implemented on purpose: the one-argument forms are
 * {@code public abstract} on {@link GeoModel} (deprecated, but still mandatory to implement), while
 * the two-argument forms are the ones {@code GeoRenderer} actually calls at render time.
 */
public class SkinnedArmorModel extends GeoModel<SkinAnchorItem> {

    private final ResourceLocation model;
    private final ResourceLocation texture;

    public SkinnedArmorModel(ItemSkinDefinition skin) {
        this.model = skin.model();
        // GEO_ARMOR skins are required to carry a texture — ItemSkinDefinition validates it.
        this.texture = skin.texture().orElseThrow();
    }

    @Override
    @Deprecated
    public ResourceLocation getModelResource(SkinAnchorItem animatable) {
        return this.model;
    }

    @Override
    public ResourceLocation getModelResource(SkinAnchorItem animatable, GeoRenderer<SkinAnchorItem> renderer) {
        return this.model;
    }

    @Override
    @Deprecated
    public ResourceLocation getTextureResource(SkinAnchorItem animatable) {
        return this.texture;
    }

    @Override
    public ResourceLocation getTextureResource(SkinAnchorItem animatable, GeoRenderer<SkinAnchorItem> renderer) {
        return this.texture;
    }

    @Override
    public ResourceLocation getAnimationResource(SkinAnchorItem animatable) {
        return null;
    }
}
