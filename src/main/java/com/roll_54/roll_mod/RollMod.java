package com.roll_54.roll_mod;

import com.roll_54.roll_mod.blocks.entity.render.PedestalBlockEntityRenderer;
import com.roll_54.roll_mod.blocks.entity.render.RocketControllerBlockEntityRenderer;
import com.roll_54.roll_mod.blocks.entity.render.model.rocket.normal.NormalRocketModel;
import com.roll_54.roll_mod.blocks.entity.render.model.rocket.small.SmallRocketModel;
import com.roll_54.roll_mod.blocks.entity.render.model.rocket.tiny.TinyRocketModel;
import com.roll_54.roll_mod.client.screen.HudRender;
import com.roll_54.roll_mod.data.RMMAttachment;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.items.armor.ModArmorMaterials;
import com.roll_54.roll_mod.registry.*;
import com.roll_54.roll_mod.gui.screen.GrowthChamberScreen;
import com.roll_54.roll_mod.gui.screen.PedestalScreen;
import com.roll_54.roll_mod.gui.screen.ResearchWorkbenchScreen;
import com.roll_54.roll_mod.gui.screen.RocketControllerScreen;
import com.roll_54.roll_mod.util.RMMItemProperties;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Mod(RollMod.MODID)
public final class RollMod {
    public static final String MODID = "roll_mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public RollMod(ModContainer container) {


        IEventBus eventBus = container.getEventBus();
        //need to work with energy

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
        ComponentsRegistry.COMPONENTS.register(eventBus);
        RMMAttachment.ATTACHMENT_TYPES.register(eventBus);
        ModConfigs.init();
        MenuTypes.register(eventBus);
        RecipeRegistry.register(eventBus);
        AttributeRegistry.ATTRIBUTES.register(eventBus);


//        LOGGER.info("[{}] test bukvi", MODID);
//        TesseractMI.init("roll_mod");
//        LOGGER.info("[{}] test bukvi end", MODID);
    // somehow ruins js code


        LOGGER.info("[{}] init complete.", MODID);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // Тут можна ініціалізувати інтеграції/дані, якщо потрібно.
        LOGGER.info("[{}] common setup", MODID);

    }

    @SuppressWarnings("deprecation")
    private void onClientSetup(final FMLClientSetupEvent event) {
        // Прив’язка меню -> екран (GUI)
        LOGGER.info("[{}] client setup", MODID);

        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.LATEX_DANDELION.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.SULFUR_BERRY_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.ROLL_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.LEDOK_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.LORP_OOO_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.YAN_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.ROCKET_CONTROLLER_BLOCK.get(), RenderType.cutout());
       // ItemBlockRenderTypes.setRenderLayer(BlockRegistry.RESEARCH_WORKBENCH.get(), renderType -> renderType == RenderType.cutout());

        RMMItemProperties.addCustomProperties();


    }
    @EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
    public static class ClientModEvents{
        /// тута регаєм ВСІ клієнт штукі, типу GUI, або ґеколіба І КЕЙБІНДІВ
        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(BlockEntites.PEDESTAL_BE.get(), PedestalBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntites.ROCKET_CONTROLLER_BE.get(), RocketControllerBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(TinyRocketModel.LAYER_LOCATION,   TinyRocketModel::createBodyLayer);
            event.registerLayerDefinition(SmallRocketModel.LAYER_LOCATION,  SmallRocketModel::createBodyLayer);
            event.registerLayerDefinition(NormalRocketModel.LAYER_LOCATION, NormalRocketModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event){
            event.register(MenuTypes.PEDESTAL_MENU.get(), PedestalScreen::new);
            event.register(MenuTypes.GROWTH_CHAMBER_MENU.get(), GrowthChamberScreen::new);
            event.register(MenuTypes.RESEARCH_WORKBENCH_MENU.get(), ResearchWorkbenchScreen::new);
            event.register(MenuTypes.ROCKET_CONTROLLER_MENU.get(), RocketControllerScreen::new);
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event){
            event.register(KeyMappingRegistry.CHESTPLATE_TOGGLE_ONE.get());
            event.register(KeyMappingRegistry.CHESTPLATE_TOGGLE_TWO.get());
            event.register(KeyMappingRegistry.CHESTPLATE_TOGGLE_THREE.get());
        }

        @SubscribeEvent
        private static void registerGuiOverlays(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME, RollMod.id("activation_status_of_gc"), HudRender::onRenderHud);
        }

    }

}
