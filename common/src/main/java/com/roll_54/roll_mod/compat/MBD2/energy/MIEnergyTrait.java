package com.roll_54.roll_mod.compat.MBD2.energy;

import aztech.modern_industrialization.api.energy.EnergyApi;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.AutoIO;
import com.lowdragmc.mbd2.common.trait.IAutoIOTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Live per-machine EU trait. Stores EU (exposed to MI cables via {@link EnergyApi#SIDED}) and feeds
 * MBD2 recipes through the reused {@code forge_energy} recipe capability (its {@code Integer} amount
 * is interpreted as EU). Modelled on MBD2's {@code ForgeEnergyCapabilityTrait}.
 */
public class MIEnergyTrait extends SimpleCapabilityTrait<MIEnergyStorage, Direction> implements IAutoIOTrait {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MIEnergyTrait.class);

    @Persisted
    @DescSynced
    public final MIEnergyStore storage;

    private final MIEnergyRecipeHandler recipeHandler = new MIEnergyRecipeHandler();
    private final Map<BlockPos, EnumMap<Direction, BlockCapabilityCache<MIEnergyStorage, Direction>>> nearbyCache = new HashMap<>();

    public MIEnergyTrait(MBDMachine machine, MIEnergyTraitDefinition definition) {
        super(machine, definition);
        this.storage = createStorage();
        this.storage.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public MIEnergyTraitDefinition getDefinition() {
        return (MIEnergyTraitDefinition) super.getDefinition();
    }

    protected MIEnergyStore createStorage() {
        MIEnergyTraitDefinition definition = getDefinition();
        return new MIEnergyStore(definition.getCapacity(), definition.getMaxReceive(),
                definition.getMaxExtract(), 0, definition.resolveTier());
    }

    @Override
    public MIEnergyStorage getCapContent(IO capabilityIO) {
        return new MIEnergyStorageWrapper(storage, capabilityIO,
                getDefinition().getMaxReceive(), getDefinition().getMaxExtract());
    }

    @Override
    public void onLoadingTraitInPreview() {
        storage.receiveEnergy(getDefinition().getCapacity() / 2, false);
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Nullable
    @Override
    public AutoIO getAutoIO() {
        return getDefinition().getAutoIO().isEnable() ? getDefinition().getAutoIO() : null;
    }

    public BlockCapabilityCache<MIEnergyStorage, Direction> getNearbyCache(ServerLevel serverLevel, BlockPos pos, Direction side) {
        return nearbyCache
                .computeIfAbsent(pos, blockPos -> new EnumMap<>(Direction.class))
                .computeIfAbsent(side, direction -> BlockCapabilityCache.create(EnergyApi.SIDED, serverLevel, pos, direction));
    }

    @Override
    public void handleAutoIO(BlockPos port, Direction side, IO io) {
        if (getMachine().getLevel() instanceof ServerLevel serverLevel) {
            if (io.support(IO.IN)) {
                MIEnergyStorage source = getNearbyCache(serverLevel, port, side).getCapability();
                if (source == null) {
                    return;
                }
                source.extract(storage.receive(source.extract(getDefinition().getMaxReceive(), true), false), false);
            }
            if (io.support(IO.OUT)) {
                MIEnergyStorage target = getNearbyCache(serverLevel, port, side).getCapability();
                if (target == null) {
                    return;
                }
                target.receive(storage.extract(target.receive(getDefinition().getMaxExtract(), true), false), false);
            }
        }
    }

    public MIEnergyStore getStorage() {
        return storage;
    }

    /**
     * Ties the EU buffer into MBD2's recipe engine. Reuses the built-in {@code forge_energy} recipe
     * capability, so recipe energy costs (its {@code Integer} content) are consumed as EU 1:1.
     */
    public class MIEnergyRecipeHandler extends RecipeHandlerTrait<Integer> {
        protected MIEnergyRecipeHandler() {
            super(MIEnergyTrait.this, ForgeEnergyRecipeCapability.CAP);
        }

        @Override
        public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
            if (!this.compatibleWith(io)) {
                return left;
            }
            int required = left.stream().reduce(0, Integer::sum);
            MIEnergyStore capability = simulate ? MIEnergyTrait.this.storage.copy() : MIEnergyTrait.this.storage;
            if (io == IO.IN) {
                required -= capability.extractEnergy(required, simulate);
            } else {
                required -= capability.receiveEnergy(required, simulate);
            }
            return required > 0 ? List.of(required) : null;
        }
    }
}
