package com.roll_54.roll_mod.gtmogs.api.registry.registrate;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.core.mixins.registrate.AbstractRegistrateAccessor;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.NoConfigBuilder;
import com.tterrag.registrate.util.OneTimeEventReceiver;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class GTRegistrate extends AbstractRegistrate<GTRegistrate> {

    private static final Map<String, GTRegistrate> EXISTING_REGISTRATES = new Object2ObjectOpenHashMap<>();

    private final AtomicBoolean registered = new AtomicBoolean(false);

    protected GTRegistrate(String modId) {
        super(modId);
    }

    public ResourceLocation makeResourceLocation(String path) {
        return ResourceLocation.fromNamespaceAndPath(this.getModid(), path);
    }

    /**
     * Get or create a new {@link GTRegistrate} and register event listeners for registration and data generation.
     * A new {@code GTRegistrate} instance is only made if one doesn't already exist in the cache.
     *
     * @param modId The mod ID for which objects will be registered
     * @return The {@link GTRegistrate} instance
     */
    public static GTRegistrate create(String modId) {
        return innerCreate(modId, true);
    }

    /**
     * Get or create a new {@link GTRegistrate} and register event listeners for registration and data generation.
     * A new {@code GTRegistrate} instance is only made if one doesn't already exist in the cache.
     * <br>
     * Completely skips all mod id validity messages and defaults to GT's bus instead. <b>ADDON DEVS DO NOT USE.</b>
     *
     * @param modId The mod ID for which objects will be registered
     * @return The {@link GTRegistrate} instance
     */
    @ApiStatus.Internal
    public static GTRegistrate createIgnoringListenerErrors(String modId) {
        return innerCreate(modId, false);
    }

    private static GTRegistrate innerCreate(String modId, boolean strict) {
        if (EXISTING_REGISTRATES.containsKey(modId)) {
            return EXISTING_REGISTRATES.get(modId);
        }
        var registrate = new GTRegistrate(modId);
        Optional<IEventBus> modEventBus = ModList.get().getModContainerById(modId).map(ModContainer::getEventBus);
        if (strict) {
            modEventBus.ifPresentOrElse(registrate::registerEventListeners, () -> {
                String message = "# [GTRegistrate] Failed to register eventListeners for mod " + modId +
                        ", This should be reported to this mod's dev #";
                String hashtags = "#".repeat(message.length());
                GTMOGS.LOGGER.fatal(hashtags);
                GTMOGS.LOGGER.fatal(message);
                GTMOGS.LOGGER.fatal(hashtags);
            });
        } else {
            registrate.registerEventListeners(modEventBus.orElse(GTMOGS.gtModBus));
        }
        EXISTING_REGISTRATES.put(modId, registrate);
        return registrate;
    }

    @Override
    public GTRegistrate registerEventListeners(IEventBus bus) {
        if (!registered.getAndSet(true)) {
            if (((AbstractRegistrateAccessor) this).getModEventBus() == null) {
                ((AbstractRegistrateAccessor) this).setModEventBus(bus);
            }
            // recreate the super method so we can register the event listener with LOW priority.
            Consumer<RegisterEvent> onRegister = this::onRegister;
            Consumer<RegisterEvent> onRegisterLate = this::onRegisterLate;
            bus.addListener(EventPriority.LOW, onRegister);
            bus.addListener(EventPriority.LOWEST, onRegisterLate);

            // Fired multiple times when ever tabs need contents rebuilt (changing op tab perms for example)
            bus.addListener(this::onBuildCreativeModeTabContents);
            // Register events fire multiple times, so clean them up on common setup
            OneTimeEventReceiver.addModListener(this, FMLCommonSetupEvent.class, $ -> {
                OneTimeEventReceiver.unregister(this, onRegister, RegisterEvent.class);
                OneTimeEventReceiver.unregister(this, onRegisterLate, RegisterEvent.class);
            });
            // No Registrate datagen: it would run inside roll_mod's own (:client:runData) and write marker
            // blockstates, loot and lang into roll_mod's generated resources. roll_mod keeps those by hand.
        }
        return this;
    }

    protected <P> NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> createCreativeModeTab(P parent, String name,
                                                                                             Consumer<CreativeModeTab.Builder> config) {
        return this.generic(parent, name, Registries.CREATIVE_MODE_TAB, () -> {
            var builder = CreativeModeTab.builder()
                    .icon(() -> getAll(Registries.ITEM).stream().findFirst().map(ItemEntry::cast)
                            .map(ItemEntry::asStack).orElse(new ItemStack(Items.AIR)));
            config.accept(builder);
            return builder.build();
        });
    }

    @Nullable
    private RegistryEntry<CreativeModeTab, ? extends CreativeModeTab> currentTab;
    private static final Map<RegistryEntry<?, ?>, @Nullable RegistryEntry<CreativeModeTab, ? extends CreativeModeTab>> TAB_LOOKUP = new IdentityHashMap<>();

    public void creativeModeTab(@Nullable RegistryEntry<CreativeModeTab, ? extends CreativeModeTab> currentTab) {
        this.currentTab = currentTab;
    }

    protected <R, T extends R> RegistryEntry<R, T> accept(String name, ResourceKey<? extends Registry<R>> type,
                                                          Builder<R, T, ?, ?> builder,
                                                          NonNullSupplier<? extends T> creator,
                                                          NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory) {
        RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);

        if (this.currentTab != null) {
            TAB_LOOKUP.put(entry, this.currentTab);
        }

        return entry;
    }

    public <P> NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> defaultCreativeTab(P parent, String name,
                                                                                       Consumer<CreativeModeTab.Builder> config) {
        return createCreativeModeTab(parent, name, config);
    }
}
