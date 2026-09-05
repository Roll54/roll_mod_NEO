package com.roll_54.roll_mod.compat.mi;

import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.machines.models.MachineCasing;
import aztech.modern_industrialization.machines.models.MachineCasings;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Machine casings registered into Modern Industrialization's own namespace.
 *
 * <p>The Java counterpart of KubeJS' {@code MIMachineEvents.registerCasings} /
 * {@code event.registerBlockImitation(name, block)}: a casing that carries no model of its own and
 * simply renders as an existing block, so a multiblock's hull looks like the block players build it
 * out of.
 *
 * <h2>Why not Tesseract's {@code registerImitateBlock}</h2>
 *
 * <p>{@link net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineCasingsMIHookContext#registerImitateBlock}
 * keys the casing under <em>this</em> mod's namespace ({@code roll_mod:turbine_casing}), and also
 * queues a casing model for datagen — neither of which a block imitation wants. MI resolves a bare
 * casing name through {@code MachineCasings.get}, which expands it with {@code MI.id}, so a casing
 * that scripts and multiblock shapes refer to as {@code "turbine_casing"} has to live at
 * {@code modern_industrialization:turbine_casing}. That is what MI's own KubeJS event does, and
 * what {@link #blockImitation} does here.
 *
 * <h2>Timing</h2>
 *
 * <p>Call from {@code MIHookListener#machineCasings}, which fires while MI is setting its own
 * casings up. The imitated block is resolved lazily, so it does not have to be registered yet.
 */
public final class MICasings {

    private MICasings() {}

    /** {@link #blockImitation(String, ResourceLocation)} with the block as {@code namespace:path}. */
    public static MachineCasing blockImitation(String name, String block) {
        return blockImitation(name, ResourceLocation.parse(block));
    }

    /**
     * Registers {@code modern_industrialization:<name>} as a casing rendering as {@code block}.
     *
     * @param name the casing name, without a namespace — it is always MI's, as with MI's own
     *             {@code registerBlockImitation}
     * @param block the block to imitate, looked up when the model is baked
     */
    public static MachineCasing blockImitation(String name, ResourceLocation block) {
        if (name.contains(":")) {
            throw new IllegalArgumentException("Casing name cannot contain ':': " + name);
        }

        ResourceLocation key = MI.id(name);
        MachineCasing existing = MachineCasings.registeredCasings.get(key);
        if (existing != null) {
            // MI throws on a duplicate; a casing it (or another mod) ships already is not an error
            // for us, it just means ours is redundant.
            RollMod.LOGGER.debug("[MI] Casing '{}' is already registered, leaving it alone.", key);
            return existing;
        }

        RollMod.LOGGER.debug("[MI] Registering casing '{}' imitating block '{}'.", key, block);
        return MachineCasings.createBlockImitation(key, () -> imitated(key, block));
    }

    private static Block imitated(ResourceLocation casing, ResourceLocation block) {
        Block imitated = BuiltInRegistries.BLOCK.get(block);
        if (imitated == Blocks.AIR) {
            // A defaulted registry hands back air rather than failing, which would bake the casing
            // invisible with nothing in the log to explain it.
            RollMod.LOGGER.warn("[MI] Casing '{}' imitates block '{}', which is not registered.",
                    casing, block);
        }
        return imitated;
    }
}
