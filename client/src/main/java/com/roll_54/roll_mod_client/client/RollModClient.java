package com.roll_54.roll_mod_client.client;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod_client.blocks.entity.render.PedestalBlockEntityRenderer;
import com.roll_54.roll_mod_client.blocks.entity.render.RocketControllerBlockEntityRenderer;
import com.roll_54.roll_mod_client.blocks.entity.render.model.rocket.normal.NormalRocketModel;
import com.roll_54.roll_mod_client.blocks.entity.render.model.rocket.small.SmallRocketModel;
import com.roll_54.roll_mod_client.blocks.entity.render.model.rocket.tiny.TinyRocketModel;
import com.roll_54.roll_mod_client.client.gecko.ClownHatRenderer;
import com.roll_54.roll_mod_client.client.gecko.GeckoArmorRenderer;
import com.roll_54.roll_mod_client.client.gecko.HazmatHelmetRenderer;
import com.roll_54.roll_mod_client.client.gecko.MultiProtectingGraviChestPlateRenderer;
import com.roll_54.roll_mod_client.client.screen.HudRender;
import com.roll_54.roll_mod_client.gui.screen.CropManagerScreen;
import com.roll_54.roll_mod_client.gui.screen.GrowthChamberScreen;
import com.roll_54.roll_mod_client.gui.screen.PedestalScreen;
import com.roll_54.roll_mod_client.gui.screen.ResearchWorkbenchScreen;
import com.roll_54.roll_mod_client.gui.screen.RocketControllerScreen;
import com.roll_54.roll_mod.items.armor.geckolib.GeoArmorRendererRegistry;
import com.roll_54.roll_mod.registry.BlockEntites;
import com.roll_54.roll_mod.registry.BlockRegistry;
import com.roll_54.roll_mod.registry.ItemRegistry;
import com.roll_54.roll_mod_client.registry.KeyMappingRegistry;
import com.roll_54.roll_mod.registry.MenuTypes;
import com.roll_54.roll_mod_client.util.RMMItemProperties;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;

import java.util.function.Supplier;

@Mod(value = RollModClient.MODID, dist = Dist.CLIENT)
public final class RollModClient {
    public static final String MODID = "roll_mod_client";

    public RollModClient(ModContainer container) {
        IEventBus eventBus = container.getEventBus();
        eventBus.addListener(this::onClientSetup);
        RollMod.LOGGER.info("[{}] init complete.", MODID);
    }

    @SuppressWarnings("deprecation")
    private void onClientSetup(final FMLClientSetupEvent event) {
        RollMod.LOGGER.info("[{}] client setup", MODID);

        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.LATEX_DANDELION.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.SULFUR_BERRY_BLOCK.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.ROLL_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.LEDOK_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.LORP_OOO_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.YAN_PLUSH.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(BlockRegistry.ROCKET_CONTROLLER_BLOCK.get(), RenderType.cutout());

        event.enqueueWork(RMMItemProperties::addCustomProperties);
        event.enqueueWork(RollModClient::registerArmorRenderers);
    }

    /** Wires common GeoItem armor to their client renderers via the common-side registry. */
    private static void registerArmorRenderers() {
        registerArmor(ItemRegistry.EXAMPLE_ARMOR_HELMET.get(), GeckoArmorRenderer::new);
        registerArmor(ItemRegistry.CLOWN_HAT.get(), ClownHatRenderer::new);
        registerArmor(ItemRegistry.HAZMAT_HELMET.get(), HazmatHelmetRenderer::new);
        registerArmor(ItemRegistry.MULTI_PROTECTING_GRAVI_CHESTPLATE.get(), MultiProtectingGraviChestPlateRenderer::new);
    }

    private static void registerArmor(Item item, Supplier<? extends HumanoidModel<?>> rendererFactory) {
        GeoArmorRendererRegistry.register(item, consumer -> consumer.accept(new GeoRenderProvider() {
            private HumanoidModel<?> renderer;

            @Override
            public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
                if (this.renderer == null) {
                    this.renderer = rendererFactory.get();
                }
                return this.renderer;
            }
        }));
    }

    @EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(BlockEntites.PEDESTAL_BE.get(), PedestalBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(BlockEntites.ROCKET_CONTROLLER_BE.get(), RocketControllerBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(TinyRocketModel.LAYER_LOCATION, TinyRocketModel::createBodyLayer);
            event.registerLayerDefinition(SmallRocketModel.LAYER_LOCATION, SmallRocketModel::createBodyLayer);
            event.registerLayerDefinition(NormalRocketModel.LAYER_LOCATION, NormalRocketModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(MenuTypes.PEDESTAL_MENU.get(), PedestalScreen::new);
            event.register(MenuTypes.GROWTH_CHAMBER_MENU.get(), GrowthChamberScreen::new);
            event.register(MenuTypes.RESEARCH_WORKBENCH_MENU.get(), ResearchWorkbenchScreen::new);
            event.register(MenuTypes.ROCKET_CONTROLLER_MENU.get(), RocketControllerScreen::new);
            event.register(MenuTypes.CROP_MANAGER_MENU.get(), CropManagerScreen::new);
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(KeyMappingRegistry.CHESTPLATE_TOGGLE_ONE.get());
            event.register(KeyMappingRegistry.CHESTPLATE_TOGGLE_TWO.get());
            event.register(KeyMappingRegistry.CHESTPLATE_TOGGLE_THREE.get());
        }

        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.SELECTED_ITEM_NAME, RollMod.id("activation_status_of_gc"), HudRender::onRenderHud);
        }
    }
}
