package com.roll_54.roll_mod.compat.MBD2.machine;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.mbd2.common.machine.definition.config.event.MachineUIEvent;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Puts every Crop Manager tier's screen in the player's hub theme, like the mod's other screens.
 *
 * <p>MBD2 builds a machine screen from the definition's {@link com.lowdragmc.lowdraglib2.gui.ui.UITemplate},
 * whose stylesheets are fixed when the template is built — at mod construction, long before any
 * player exists. {@link MachineUIEvent} is the one point where the built UI and the player are both
 * known, so the template's sheets are swapped for the theme's here. The element tree is kept as-is:
 * MBD2 has already bound it to the machine by then.
 *
 * <p>Client copy only, as in {@link RollUi}: the server's copy is never drawn.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class CropManagerThemeHandler {

    private CropManagerThemeHandler() {}

    @SubscribeEvent
    public static void onMachineUI(MachineUIEvent event) {
        UI ui = event.getUi();
        if (ui == null || !event.getPlayer().level().isClientSide()) return;
        if (!RollMBD2Machines.isCropManager(event.getMachine().getDefinition().id())) return;
        event.setUi(UI.of(ui.rootElement, RollUi.sheets(), ui.dynamicSize));
    }
}
