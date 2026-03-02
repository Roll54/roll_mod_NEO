package com.roll_54.roll_mod;

import com.roll_54.roll_mod.blocks.entity.render.PedestalBlockEntityRenderer;
import com.roll_54.roll_mod.data.RMMAttachment;
import com.roll_54.roll_mod.data.RMMComponents;
import com.roll_54.roll_mod.modArmor.ModArmorMaterials;
import com.roll_54.roll_mod.init.*;
import com.roll_54.roll_mod.mi.MIConditionsBootstrap;
import com.roll_54.roll_mod.screen.screen.GrowthChamberScreen;
import com.roll_54.roll_mod.screen.screen.PedestalScreen;
import com.roll_54.roll_mod.screen.screen.ResearchWorkbenchScreen;
import com.roll_54.roll_mod.util.RMMItemProperties;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
        GeneratedOreRegistry.register(eventBus);
        ItemGroups.register(eventBus);
//        PYOreDataGen.register(eventBus);
//        PYItems.register(eventBus);
        BlockEntites.register(eventBus);
        ModEffects.register(eventBus);
        ModArmorMaterials.register(eventBus);
        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onClientSetup);
        SoundRegistry.SOUND_EVENTS.register(eventBus);
        RMMComponents.COMPONENTS.register(eventBus);
        RMMAttachment.ATTACHMENT_TYPES.register(eventBus);
        ModConfigs.init();
        MenuTypes.register(eventBus);
        RecipeRegister.register(eventBus);

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
       // ItemBlockRenderTypes.setRenderLayer(BlockRegistry.RESEARCH_WORKBENCH.get(), renderType -> renderType == RenderType.cutout());

        RMMItemProperties.addCustomProperties();


    }
    @EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
    public static class ClientModEvents{
        /// тута регаєм ВСІ клієнт штукі, типу GUI, або ґеколіба
        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(BlockEntites.PEDESTAL_BE.get(), PedestalBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event){
            event.register(MenuTypes.PEDESTAL_MENU.get(), PedestalScreen::new);
            event.register(MenuTypes.GROWTH_CHAMBER_MENU.get(), GrowthChamberScreen::new);
            event.register(MenuTypes.RESEARCH_WORKBENCH_MENU.get(), ResearchWorkbenchScreen::new);
        }

    }

}
