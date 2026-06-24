package com.roll_54.roll_mod;

import com.roll_54.roll_mod.data.RMMAttachment;
import com.roll_54.roll_mod.minestar.CleanDropConfig;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.items.armor.ModArmorMaterials;
import com.roll_54.roll_mod.registry.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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
        BlockEntites.register(eventBus);
        ModEffects.register(eventBus);
        ModArmorMaterials.register(eventBus);
        eventBus.addListener(this::onCommonSetup);
        SoundRegistry.SOUND_EVENTS.register(eventBus);
        ComponentsRegistry.COMPONENTS.register(eventBus);
        RMMAttachment.ATTACHMENT_TYPES.register(eventBus);
        eventBus.addListener(com.roll_54.roll_mod.radiation.RadiationComponentDefaults::modifyComponents);
        ModConfigs.init();
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, CleanDropConfig.SPEC);
        MenuTypes.register(eventBus);
        RecipeRegistry.register(eventBus);
        AttributeRegistry.ATTRIBUTES.register(eventBus);

        LOGGER.info("[{}] init complete.", MODID);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // Тут можна ініціалізувати інтеграції/дані, якщо потрібно.
        LOGGER.info("[{}] common setup", MODID);
    }
}
