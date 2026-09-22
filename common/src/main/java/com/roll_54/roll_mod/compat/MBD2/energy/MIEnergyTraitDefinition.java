package com.roll_54.roll_mod.compat.MBD2.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.EnergyApi;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.common.gui.MBDSprites;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.IUIProviderTrait.TraitUILayoutType;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.common.trait.ToggleAutoIO;
import com.lowdragmc.mbd2.common.trait.forgeenergy.EnergyStorageList;
import com.lowdragmc.mbd2.common.trait.forgeenergy.EnergyStorageWrapper;
import com.lowdragmc.mbd2.common.trait.TraitDefinitionType;
import com.lowdragmc.mbd2.utils.EnergyFormattingUtil;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MBD2 trait definition exposing a native Modern Industrialization EU store on a machine. Modelled on
 * MBD2's built-in {@code ForgeEnergyCapabilityTraitDefinition}, retargeted from Forge Energy to
 * {@link EnergyApi#SIDED}. Registered automatically through the {@link LDLRegister} scan on {@link #TYPE}.
 */
public class MIEnergyTraitDefinition extends SimpleCapabilityTraitDefinition<MIEnergyStorage, Direction> {

    // Registered explicitly from RollMBD2Plugin (MBD2's @LDLRegister scan is not used, to avoid a
    // duplicate-registration conflict). The "trait" group comes from the Type(name, group) constructor.
    public static final SimpleCapabilityTraitDefinition.Type<MIEnergyStorage, Direction, MIEnergyTraitDefinition> TYPE =
            new SimpleCapabilityTraitDefinition.Type<>("mi_energy_storage", "trait") {
                @Override
                public MIEnergyTraitDefinition createDefinition() {
                    return new MIEnergyTraitDefinition();
                }

                @Override
                protected BlockCapability<MIEnergyStorage, Direction> getCapability() {
                    return EnergyApi.SIDED;
                }

                @Override
                protected MIEnergyStorage merge(List<MIEnergyStorage> contents) {
                    return new MergedMIEnergyStorage(List.copyOf(contents));
                }

                /**
                 * Exposes the very same buffer as Forge Energy as well as EU, so the machine can be
                 * powered from an MI cable or an FE cable interchangeably.
                 *
                 * <p>This costs nothing but the registration: {@link MIEnergyStore} already
                 * {@code extends EnergyStorage implements MIEnergyStorage} over one {@code int}, so
                 * there is one buffer and no conversion — 1 FE in is 1 EU stored. Mirrors the EU
                 * registration {@code super} performs, down to the per-side {@link IO} gating.
                 */
                @Override
                public void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
                    super.registerCapabilities(definition, event);
                    event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, definition.blockEntityType(),
                            (blockEntity, side) -> {
                                if (!(blockEntity instanceof IMachineBlockEntity machineBlockEntity)
                                        || !(machineBlockEntity.getMetaMachine() instanceof MBDMachine machine)) {
                                    return null;
                                }
                                List<IEnergyStorage> views = new ArrayList<>();
                                for (ITrait trait : machine.getAdditionalTraits()) {
                                    if (trait instanceof MIEnergyTrait energyTrait
                                            && energyTrait.getDefinition().type() == this) {
                                        MIEnergyTraitDefinition traitDefinition = energyTrait.getDefinition();
                                        views.add(new EnergyStorageWrapper(energyTrait.getStorage(),
                                                energyTrait.getCapabilityIO(side),
                                                traitDefinition.getMaxReceive(),
                                                traitDefinition.getMaxExtract()));
                                    }
                                }
                                if (views.isEmpty()) {
                                    return null;
                                }
                                return views.size() == 1
                                        ? views.get(0)
                                        : new EnergyStorageList(views.toArray(new IEnergyStorage[0]));
                            });
                }
            };

    @Configurable(name = "config.definition.trait.mi_energy_storage.capacity")
    @ConfigNumber(range = {1.0, 2.147483647E9})
    private int capacity = 100000;

    @Configurable(name = "config.definition.trait.mi_energy_storage.max_receive",
            tips = {"config.definition.trait.mi_energy_storage.max_receive.tooltip"})
    @ConfigNumber(range = {0.0, 2.147483647E9})
    private int maxReceive = 10000;

    @Configurable(name = "config.definition.trait.mi_energy_storage.max_extract",
            tips = {"config.definition.trait.mi_energy_storage.max_extract.tooltip"})
    @ConfigNumber(range = {0.0, 2.147483647E9})
    private int maxExtract = 10000;

    @Configurable(name = "config.definition.trait.mi_energy_storage.cable_tier",
            tips = {"config.definition.trait.mi_energy_storage.cable_tier.tooltip"})
    private String cableTier = "lv";

    @Configurable(name = "config.definition.trait.auto_io", subConfigurable = true,
            tips = {"config.definition.trait.mi_energy_storage.auto_io.tooltip"})
    private final ToggleAutoIO autoIO = new ToggleAutoIO();

    @Override
    public MIEnergyTrait createTrait(MBDMachine machine) {
        return new MIEnergyTrait(machine, this);
    }

    @Override
    public TraitDefinitionType<?> type() {
        return TYPE;
    }

    @Override
    public IGuiTexture getIcon() {
        return MBDSprites.ENERGY_ICON;
    }

    @Override
    public TraitUILayoutType getTraitUILayoutType() {
        return TraitUILayoutType.BAR;
    }

    @Override
    public void createTraitUITemplate(UIElement container) {
        ProgressBar progress = new ProgressBar();
        progress.barContainer.getLayout().paddingAll(0.0F);
        progress.barContainer.getStyle().background(MBDSprites.ENERGY_BG);
        progress.bar.getStyle().background(MBDSprites.ENERGY_BAR);
        progress.setProgress(1.0F);
        progress.label.setText("0/0 EU");
        progress.setId(this.uiId());
        progress.layout(layout -> layout.height(14.0F));
        container.addChild(progress);
    }

    @Override
    public void initTraitUI(ITrait trait, UI ui) {
        if (trait instanceof MIEnergyTrait energyTrait) {
            ui.selectId(this.uiId(), ProgressBar.class).forEach(energyBar -> {
                energyBar.bind(DataBindingBuilder.floatValS2C(() -> {
                    int max = energyTrait.getStorage().getMaxEnergyStored();
                    return max > 0 ? (float) energyTrait.getStorage().getEnergyStored() / (float) max : 0.0F;
                }).build());
                AtomicInteger stored = new AtomicInteger(energyTrait.getStorage().getEnergyStored());
                AtomicInteger maxStored = new AtomicInteger(energyTrait.getStorage().getMaxEnergyStored());
                SimpleBinding<Integer> storedValue = DataBindingBuilder.intValS2C(() -> energyTrait.getStorage().getEnergyStored())
                        .remoteSetter(stored::set)
                        .build();
                SimpleBinding<Integer> maxStoredValue = DataBindingBuilder.intValS2C(() -> energyTrait.getStorage().getMaxEnergyStored())
                        .remoteSetter(maxStored::set)
                        .build();
                energyBar.addSyncValue(storedValue.getSyncValue());
                energyBar.addSyncValue(maxStoredValue.getSyncValue());
                energyBar.label.bindDataSource(SupplierDataSource.of(() -> {
                    String storedVal = EnergyFormattingUtil.formatCompact(stored.get());
                    String maxStoredVal = EnergyFormattingUtil.formatCompact(maxStored.get());
                    return Component.literal(storedVal + "/" + maxStoredVal + " EU");
                }));
                energyBar.addEventListener("hoverTooltips", event -> {
                    event.hoverTooltips = new HoverTooltips(
                            List.of(Component.literal(
                                    EnergyFormattingUtil.formatCompact(stored.get())
                                            + "/"
                                            + EnergyFormattingUtil.formatCompact(maxStored.get())
                                            + " EU")),
                            null, null, null);
                    event.stopPropagation();
                });
            });
        }
    }

    /** Resolves the configured cable tier, falling back to LV if the string is invalid. */
    public CableTier resolveTier() {
        try {
            return CableTier.getTier(cableTier);
        } catch (RuntimeException e) {
            return CableTier.LV;
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getMaxReceive() {
        return maxReceive;
    }

    public void setMaxReceive(int maxReceive) {
        this.maxReceive = maxReceive;
    }

    public int getMaxExtract() {
        return maxExtract;
    }

    public void setMaxExtract(int maxExtract) {
        this.maxExtract = maxExtract;
    }

    public String getCableTier() {
        return cableTier;
    }

    public void setCableTier(String cableTier) {
        this.cableTier = cableTier;
    }

    public ToggleAutoIO getAutoIO() {
        return autoIO;
    }
}
