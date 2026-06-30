package com.roll_54.roll_mod.radiation;

import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Attaches the {@link ComponentsRegistry#RADIATION_RESISTANCE} component to storm-protective armor
 * that belongs to other mods (the extended_industrialization nano suits listed in the
 * {@code roll_mod:storm_protective} tag). roll_mod's own hazmat pieces get the component directly at
 * registration in {@code ItemRegistry}; this handler covers the items we don't register ourselves so
 * they protect against radiation too. Items whose owning mod is absent are silently skipped.
 *
 * <p>Wired to the mod event bus from {@code RollMod}'s constructor (this is a mod-bus event).
 */
public class RadiationComponentDefaults {

    /** Per-piece resistance applied to each external storm-protective armor item (full set = 1.0 = immune). */
    private static final float PIECE_RESISTANCE = 0.25f;

    private static final String[] PROTECTIVE_ITEMS = {
            "extended_industrialization:nano_helmet",
            "extended_industrialization:nano_gravichestplate",
            "extended_industrialization:nano_leggings",
            "extended_industrialization:nano_boots",
            "extended_industrialization:nano_quantum_helmet",
            "extended_industrialization:nano_quantum_chestplate",
            "extended_industrialization:nano_quantum_leggings",
            "extended_industrialization:nano_quantum_boots",
    };

    public static void modifyComponents(ModifyDefaultComponentsEvent event) {
        for (String id : PROTECTIVE_ITEMS) {
            ResourceLocation rl = ResourceLocation.parse(id);
            BuiltInRegistries.ITEM.getOptional(rl).ifPresent(item ->
                    event.modify(item, builder ->
                            builder.set(ComponentsRegistry.RADIATION_RESISTANCE.get(), PIECE_RESISTANCE)));
        }
    }
}
