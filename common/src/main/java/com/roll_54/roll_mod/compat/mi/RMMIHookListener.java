package com.roll_54.roll_mod.compat.mi;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.MachineRegistry;
import net.swedz.tesseract.neoforge.compat.mi.hook.MIHookEntrypoint;
import net.swedz.tesseract.neoforge.compat.mi.hook.MIHookListener;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineCasingsMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineRecipeTypesMIHookContext;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MultiblockMachinesMIHookContext;

@MIHookEntrypoint
public final class RMMIHookListener implements MIHookListener {

    static {
        // Tesseract loads entrypoints with Class.forName, which initializes the class, and it does
        // so long before MI is constructed -- the only window early enough for the block-imitation
        // casings, since MI resolves them while registering its own machines. See
        // MachineRegistry#blockImitationCasings for why the MIHookListener callbacks below are too
        // late for these.
        try {
            MachineRegistry.blockImitationCasings();
        } catch (Throwable t) {
            // Never let this break entrypoint registration: without it the multiblocks using these
            // casings fail to register, but the rest of the mod still loads.
            RollMod.LOGGER.error("[MI] Failed to register block imitation casings early.", t);
        }
    }

    public void machineCasings(MachineCasingsMIHookContext hook) {
        MachineRegistry.casings(hook);
    }

    public void machineRecipeTypes(MachineRecipeTypesMIHookContext hook) {
        MachineRegistry.recipeTypes(hook);
    }

    public void multiblockMachines(MultiblockMachinesMIHookContext hook) {
        MachineRegistry.multiblocks(hook);
    }
}
