package com.roll_54.roll_mod.items.armor.geckolib;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A stand-in animatable for cosmetic helmet skins. Never obtainable, never rendered as itself.
 *
 * <p>{@code GeoArmorRenderer} is declared {@code <T extends Item & GeoItem>}, and it means it: while
 * rendering it casts its animatable to {@link GeoItem} to read the animation tick, and
 * {@code GeoModel#handleAnimations} calls {@code getAnimatableInstanceCache()} on it. A cosmetic skin
 * may be worn on <em>any</em> helmet — a vanilla diamond one included — and those are neither. So the
 * skin renderer pins this item as its animatable instead of the helmet actually being worn; the worn
 * stack, wearer and slot are untouched, which is all the rest of the renderer reads.
 *
 * <p>It has to be a real registered item rather than a lazily constructed singleton: {@link Item}'s
 * field initialiser calls {@code BuiltInRegistries.ITEM.createIntrusiveHolder(this)}, which throws
 * once the registry is frozen, and freezing also rejects intrusive holders that were never
 * registered. Hence a normal {@code DeferredRegister} entry, deliberately left out of every creative
 * tab.
 *
 * <p>No {@code createGeoRenderer} override on purpose. The default is a no-op and
 * {@code AnimatableInstanceCache} seeds its provider with {@code GeoRenderProvider.DEFAULT}, so
 * nothing can ever draw this item as armour in its own right.
 */
public class SkinAnchorItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public SkinAnchorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Skins are static models; the animation system only needs a valid owner, not a controller.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
