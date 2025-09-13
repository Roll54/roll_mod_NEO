package com.roll_54.roll_mod;

import com.roll_54.roll_mod.client.ClientSetup;
import com.roll_54.roll_mod.net.NetRegistration;
import com.roll_54.roll_mod.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Roll_mod.MODID)
public final class Roll_mod {
    public static final String MODID = "roll_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public Roll_mod(ModContainer container) {
        // Модова шина подій (lifecycle)
        IEventBus modBus = container.getEventBus();

        // === Реєстри, які реально потрібні для машини ===
        ModMenus.MENUS.register(modBus);            // меню (container) для GUI
        modBus.addListener(NetRegistration::register); // мережеві payload-и (C2S switch mode)

        // === Lifecycle ===
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);

        LOGGER.info("[{}] init complete.", MODID);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // Тут можна ініціалізувати інтеграції/дані, якщо потрібно.
        // Напр., якщо маєш власні тег-скани, capabilities, тощо.
        LOGGER.info("[{}] common setup", MODID);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // Прив’язка меню -> екран (GUI)
        ClientSetup.init();
        LOGGER.info("[{}] client setup", MODID);
    }
}
