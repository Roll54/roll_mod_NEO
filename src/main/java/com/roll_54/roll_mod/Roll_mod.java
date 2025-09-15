package com.roll_54.roll_mod;

import com.roll_54.roll_mod.ModArmor.ModArmorMaterials;
import com.roll_54.roll_mod.init.ItemGroups;
import com.roll_54.roll_mod.init.ItemRegistry;
import com.roll_54.roll_mod.mi.MIConditionsBootstrap;
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
        ItemRegistry.register(modBus);
        ItemGroups.register(modBus);
        ModArmorMaterials.register(modBus);
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onClientSetup);

        LOGGER.info("[{}] init complete.", MODID);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // Тут можна ініціалізувати інтеграції/дані, якщо потрібно.
        LOGGER.info("[{}] common setup", MODID);
        MIConditionsBootstrap.init();
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // Прив’язка меню -> екран (GUI)
        LOGGER.info("[{}] client setup", MODID);
    }
}
