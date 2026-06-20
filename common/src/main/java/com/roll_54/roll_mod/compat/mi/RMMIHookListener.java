package com.roll_54.roll_mod.compat.mi;

import com.roll_54.roll_mod.registry.MachineRegistry;
import net.swedz.tesseract.neoforge.compat.mi.hook.MIHookEntrypoint;
import net.swedz.tesseract.neoforge.compat.mi.hook.MIHookListener;
import net.swedz.tesseract.neoforge.compat.mi.hook.context.listener.MachineCasingsMIHookContext;

@MIHookEntrypoint
public final class RMMIHookListener implements MIHookListener {
    public void machineCasings(MachineCasingsMIHookContext hook) {
        MachineRegistry.casings(hook);
    }
}
