package com.roll_54.roll_mod.items.armor.geckolib;

import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bridges common-registered {@link software.bernie.geckolib.animatable.GeoItem} armor to
 * their client-only {@link GeoRenderProvider}s, which live in the client jar.
 *
 * <p>The client module registers one handler per item during client setup. On a dedicated
 * server no handler is registered and {@link #apply} is a no-op — {@code createGeoRenderer}
 * is only ever invoked client-side, so the common item never needs to reference a client
 * renderer class directly.
 */
public final class GeoArmorRendererRegistry {
    private GeoArmorRendererRegistry() {}

    private static final Map<Item, Consumer<Consumer<GeoRenderProvider>>> HANDLERS = new IdentityHashMap<>();

    /** Called from the client module to wire an item to its GeoRenderProvider factory. */
    public static void register(Item item, Consumer<Consumer<GeoRenderProvider>> handler) {
        HANDLERS.put(item, handler);
    }

    /** Called by each item's {@code createGeoRenderer}; no-op when no handler is registered. */
    public static void apply(Item item, Consumer<GeoRenderProvider> consumer) {
        Consumer<Consumer<GeoRenderProvider>> handler = HANDLERS.get(item);
        if (handler != null) {
            handler.accept(consumer);
        }
    }
}
