package com.roll_54.roll_mod.registry;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.mi.MIMachineModels;

/**
 * This pack's Modern Industrialization machine models — one place to declare what every MI machine
 * renders with, instead of the three booleans buried in each machine's KubeJS call.
 *
 * <p>A machine's block and block entity still come from wherever they came from before; only the
 * model is declared here. Because MI keys models by the machine's internal name, an entry here
 * takes over from whatever that machine registered for itself, KubeJS included — so a machine can
 * be moved onto a hand-written model without touching the script that creates it.
 *
 * <p>See {@link MIMachineModels} for what the builder can express and why it reaches past MI's own
 * {@code addMachineModel} helper.
 *
 * <h2>Adding a machine</h2>
 *
 * <pre>{@code
 * MIMachineModels.machine("space_research_station", "space_research_station_controller_casing")
 *         .folder("block/machines/srs")   // roll_mod:block/machines/srs/overlay_<face>[_active]
 *         .faces("front", "top")
 *         .overlay("back", "block/machines/srs/custom_back")
 *         .noOverlayOnOutputSide()
 *         .register();
 * }</pre>
 *
 * <p>Every texture named has to exist, or the model bakes with a missing-texture face — datagen
 * will not tell you, because MI resolves overlays at bake time rather than at registration.
 */
public final class MachineModelRegistry {

    private MachineModelRegistry() {



    }




    /**
     * Called from the mod constructor, which is early enough: MI's runtime datagen reads these at
     * {@code AddPackFindersEvent}. Nothing is declared yet — add calls here as machines move over.
     */
    public static void register() {
        RollMod.LOGGER.debug("[MI] Machine model registry ready.");
    }
}
