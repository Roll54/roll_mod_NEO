package com.roll_54.roll_mod;

import com.roll_54.roll_mod.data.ModComponents;
import com.roll_54.roll_mod.modArmor.ModArmorMaterials;
import com.roll_54.roll_mod.PYDatagen.PYOreDataGen;
import com.roll_54.roll_mod.init.*;
import com.roll_54.roll_mod.mi.MIConditionsBootstrap;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.swedz.tesseract.neoforge.capabilities.CapabilitiesListeners;
import net.swedz.tesseract.neoforge.compat.mi.TesseractMI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mod(RollMod.MODID)
public final class RollMod {
    public static final String MODID = "roll_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public RollMod(ModContainer container) {


        IEventBus eventBus = container.getEventBus();
        //need to work with energy
        TesseractMI.init(MODID);
        eventBus.addListener(RegisterCapabilitiesEvent.class, (event) -> CapabilitiesListeners.triggerAll(MODID, event));

        ItemRegistry.register(eventBus);
        BlockRegistry.register(eventBus);
        ItemGroups.register(eventBus);
        PYOreDataGen.register(eventBus);
//        PYItems.register(eventBus);
        ModEffects.EFFECTS.register(eventBus);
        ModArmorMaterials.register(eventBus);
        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onClientSetup);
        SoundRegistry.SOUND_EVENTS.register(eventBus);
        ModComponents.COMPONENTS.register(eventBus);

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
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.LATEX_DANDELION.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.SULFUR_BERRY_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.ROLL_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.LEDOK_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.LORP_OOO_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.YAN_PLUSH.get(), RenderType.cutout());

    }

    }


