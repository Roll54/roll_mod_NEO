package com.roll_54.roll_mod.netherstorm;

import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * Gives armor from other mods the {@link ComponentsRegistry#STORM_PROTECTED} component by default.
 * Wearing any armor piece whose component is {@code true} protects the player from the Nether storm
 * (see {@link StormHandler#isPlayerProtectedFromStorm}). Items whose owning mod is absent are
 * silently skipped.
 *
 * <p>Wired to the mod event bus from {@code RollMod}'s constructor (this is a mod-bus event).
 */
public class StormComponentDefaults {

    private static final String[] PROTECTIVE_ITEMS = {
            "advanced_ae:quantum_helmet",
            "advanced_ae:quantum_chestplate",
            "advanced_ae:quantum_leggings",
            "advanced_ae:quantum_boots",
    };

    public static void modifyComponents(ModifyDefaultComponentsEvent event) {
        for (String id : PROTECTIVE_ITEMS) {
            ResourceLocation rl = ResourceLocation.parse(id);
            BuiltInRegistries.ITEM.getOptional(rl).ifPresent(item ->
                    event.modify(item, builder ->
                            builder.set(ComponentsRegistry.STORM_PROTECTED.get(), true)));
        }
    }
}
