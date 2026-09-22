package com.roll_54.roll_mod.compat.MBD2;

import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.MBD2.crops.CropHarvesterTraitDefinition;
import com.roll_54.roll_mod.compat.MBD2.energy.MIEnergyTraitDefinition;

/**
 * Registers our custom MBD2 trait types. MBD2's {@code @LDLRegister} classpath scan does not pick up
 * this mod's trait type (it is not surfaced in this mod's scan data), so we register it explicitly.
 *
 * <p>Called from {@code RollMod}'s constructor, which runs single-threaded during the mod construction
 * phase — before {@code RegisterCapabilitiesEvent}, where MBD2 iterates the trait registry to expose
 * each trait's capability. Using {@link com.lowdragmc.mbd2.api.registry.MBDRegistry#unfreeze()} /
 * {@code freeze()} makes it order-independent relative to MBD2's own trait registration.
 */
public final class RollMBD2Plugin {

    private RollMBD2Plugin() {}

    public static void registerTraitTypes() {
        register(MIEnergyTraitDefinition.TYPE);
        register(CropHarvesterTraitDefinition.TYPE);
    }

    private static void register(TraitDefinitionType<?> type) {
        var registry = MBDRegistries.TRAIT_DEFINITION_TYPES;
        if (registry.get(type.name) != null) {
            RollMod.LOGGER.info("[{}] MBD2 trait type '{}' already registered", RollMod.MODID, type.name);
            return;
        }
        registry.unfreeze();
        registry.register(type.name, type);
        registry.freeze();
        RollMod.LOGGER.info("[{}] registered MBD2 trait type '{}'", RollMod.MODID, type.name);
    }
}
