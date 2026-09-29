package com.roll_54.roll_mod;

import com.roll_54.roll_mod.compat.MBD2.RollMBD2Plugin;
import com.roll_54.roll_mod.compat.MBD2.machine.RollMBD2Machines;
import com.roll_54.roll_mod.compat.MBD2.recipe.ExampleLavaRecipe;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import com.roll_54.roll_mod.cosmetics.config.CosmeticsConfig;
import com.roll_54.roll_mod.data.RMMAttachment;
import com.roll_54.roll_mod.minestar.dailytasks.DailyRewardRegistry;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskRegistry;
import com.roll_54.roll_mod.minestar.dailytasks.gui.DailyTasksUI;
import com.roll_54.roll_mod.items.modulardrill.gui.ModularDrillConfigUI;
import com.roll_54.roll_mod.minestar.hub.gui.HubUI;
import com.roll_54.roll_mod.minestar.CleanDropConfig;
import com.roll_54.roll_mod.registry.MachineModelRegistry;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.items.armor.ModArmorMaterials;
import com.roll_54.roll_mod.registry.*;
import com.roll_54.roll_mod.economy.ModRegistry;
import com.roll_54.roll_mod.economy.api.ftb.reward.CurrencyRewardType;
import com.roll_54.roll_mod.economy.api.ftb.task.CurrencyTaskType;
import com.roll_54.roll_mod.economy.config.CurrencyConfig;
import com.roll_54.roll_mod.economy.plot.command.PlotCommand;
import com.roll_54.roll_mod.economy.plot.gui.PlotPurchaseUI;
import com.roll_54.roll_mod.economy.vendingblock.block.events.BreakEvent;
import com.roll_54.roll_mod.economy.vendingblock.command.AuctionCommand;
import com.roll_54.roll_mod.economy.vendingblock.gui.auction.AuctionUI;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
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
        // Must happen in the constructor: MBD2 iterates the trait registry from FMLConstructModEvent
        // (to load machines) and again on RegisterCapabilitiesEvent, both after this point.
        RollMBD2Plugin.registerTraitTypes();
        // Before AddPackFindersEvent, which is when MI's runtime datagen reads the models.
        MachineModelRegistry.register();
        eventBus.register(new ExampleLavaRecipe());
        eventBus.register(new RollMBD2Machines());
        SoundRegistry.SOUND_EVENTS.register(eventBus);
        ComponentsRegistry.COMPONENTS.register(eventBus);
        RMMAttachment.ATTACHMENT_TYPES.register(eventBus);
        eventBus.addListener(com.roll_54.roll_mod.radiation.RadiationComponentDefaults::modifyComponents);
        ModConfigs.init();
        // Cosmetics keep their own storage and their own config: they are permanent entitlements
        // that must outlive any world, and must not depend on an economy being configured at all.
        CosmeticsConfig.init();
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, CleanDropConfig.SPEC);
        MenuTypes.register(eventBus);
        RecipeRegistry.register(eventBus);
        AttributeRegistry.ATTRIBUTES.register(eventBus);

        registerEconomy(eventBus, container);

        LOGGER.info("[{}] init complete.", MODID);
    }

    /**
     * The economy — currency, the vending block, the auction house and plot sales — which used to be
     * a separate mod with its own {@code @Mod} entrypoint. Its registries keep their own classes;
     * only the wiring moved here, because a mod gets exactly one entrypoint.
     */
    private void registerEconomy(IEventBus eventBus, ModContainer container) {
        // Fully qualified: the economy's registry classes share their simple names with this mod's
        // own (ItemRegistry, BlockRegistry, ...), which arrive here through the registry wildcard.
        ModRegistry.register(eventBus);            // the /rollmod money currency-type argument
        com.roll_54.roll_mod.economy.vendingblock.registry.ItemRegistry.register(eventBus);
        com.roll_54.roll_mod.economy.vendingblock.registry.BlockRegistry.register(eventBus);
        com.roll_54.roll_mod.economy.vendingblock.registry.BlockEntityRegistry.register(eventBus);
        com.roll_54.roll_mod.economy.vendingblock.registry.TabRegistry.register(eventBus);
        eventBus.addListener(this::registerEconomyCapabilities);

        CurrencyConfig.init();
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,
                com.roll_54.roll_mod.economy.vendingblock.Config.Client.SPEC);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,
                com.roll_54.roll_mod.economy.vendingblock.Config.Server.SPEC);

        // PlayerJoinListener is an @EventBusSubscriber already; registering it here as well is what
        // made the old mod greet players twice on login.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(BreakEvent.class);
    }

    private void registerEconomyCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                com.roll_54.roll_mod.economy.vendingblock.registry.BlockEntityRegistry.VENDOR_BE.get(),
                (vendorBlockEntity, side) -> {
                    if (side == null) {
                        return vendorBlockEntity.getPublicItemHandler();
                    } else if (side == Direction.DOWN) {
                        return vendorBlockEntity.getExtractItemHandler();
                    } else {
                        return vendorBlockEntity.getInsertItemHandler();
                    }
                });
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        // Тут можна ініціалізувати інтеграції/дані, якщо потрібно.

        // Soft dependency: these two classes touch ftbquests types, so they must not be loaded at
        // all when it is absent.
        if (ModList.get().isLoaded("ftbquests")) {
            CurrencyTaskType.init();
            CurrencyRewardType.init();
        }

        event.enqueueWork(() -> {
            // Both of these must happen on both dists. The registry has to match on client and
            // server because the daily-tasks screen syncs task positions in it, and the client
            // builds the same UI to receive its sync values.
            DailyTaskRegistry.bootstrap();
            DailyRewardRegistry.bootstrap();
            // Same reasoning: the skin definitions dereference item holders, so they cannot be
            // built in a static initialiser, and both dists need the identical set — the server
            // validates against it, the client bakes models from it.
            ItemSkinRegistry.bootstrap();
            // The hub is the only block-less screen now: the daily tasks, the auction house and
            // the plot map are tabs of it, and their commands open it on the right one. They no
            // longer register ids of their own.
            PlayerUIMenuType.register(HubUI.UI_ID,
                    player -> (PlayerUIMenuType.PlayerUIHolder) HubUI::createUI);
            // The modular drill's config screen — block-less like the hub, opened by keybind.
            PlayerUIMenuType.register(ModularDrillConfigUI.UI_ID,
                    player -> (PlayerUIMenuType.PlayerUIHolder) ModularDrillConfigUI::createUI);
        });
        LOGGER.info("[{}] common setup", MODID);
    }
}
