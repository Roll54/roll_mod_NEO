package com.roll_54.roll_mod.blocks.entity;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.EnergyApi;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import com.roll_54.roll_mod.blocks.RollSolarPanelBlock;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.registry.BlockEntites;
import aztech.modern_industrialization.util.Tickable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jspecify.annotations.Nullable;

public class RollSolarPanelBlockEntity extends BlockEntity implements Tickable {

    private @Nullable BlockCapabilityCache<MIEnergyStorage, Direction> outputCache;

    /** Energy this panel can still hand out this tick. Refilled in {@link #tick()}; never stored. */
    private long energyThisTick = 0;

    /**
     * MI energy source exposed to cables via {@link EnergyApi#SIDED} (see CapabilityRegistry).
     * Pure source (no insertion); serves up to {@link #energyThisTick}, connects to any cable tier.
     */
    private final MIEnergyStorage energyOutput = new MIEnergyStorage.NoInsert() {
        @Override
        public boolean canConnect(CableTier cableTier) {
            return true;
        }

        @Override
        public boolean canExtract() {

            return true;
        }

        @Override
        public long extract(long maxAmount, boolean simulate) {
            long extracted = Math.min(maxAmount, energyThisTick);
            if (!simulate) {
                energyThisTick -= extracted;
            }
            return extracted;
        }

        @Override
        public long getAmount() {
            return energyThisTick;
        }

        @Override
        public long getCapacity() {
            return Long.MAX_VALUE;
        }
    };

    public RollSolarPanelBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntites.SOLAR_PANEL_BE.get(), pos, blockState);
    }

    public MIEnergyStorage getEnergyOutput() {
        return energyOutput;
    }

    private long getBaseProduction() {
        if (getBlockState().getBlock() instanceof RollSolarPanelBlock panel) {
            return panel.getBaseProduction();
        }
        return 0L; // fallback if somehow attached to a non-panel block
    }


    public static double getDimensionMultiplier(Level level) {
        ResourceLocation dimensionId = level.dimension().location();


        return MyConfig.INSTANCE.solarPanelOptions.dimensionMultipliers.getOrDefault(dimensionId, 1.0F);
    }

    private boolean canProduceEnergy(Level level, BlockPos pos){
        // Dimension blacklist is enforced once in RollSolarPanelBlock#getTicker, so a
        // blacklisted panel never ticks. Here we only check the time-varying conditions.
        return level.canSeeSky(pos.above()) && level.isDay();
    }



    @Override
    public void tick() {
        if (level.isClientSide) {
            return;
        }

        // Refill this tick's budget. Cables pull from it via energyOutput; a machine directly
        // below is pushed to just below. Both draw from the same budget, so no double-spend.
        energyThisTick = canProduceEnergy(level, worldPosition)
                ? Math.round(getBaseProduction() * getDimensionMultiplier(level)) // round 1.6 -> 2, not 1
                : 0;

        if (energyThisTick <= 0) {
            return;
        }

        if (outputCache == null) {
            // Only supply energy to the block directly below.
            outputCache = BlockCapabilityCache.create(EnergyApi.SIDED, (ServerLevel) level,
                    worldPosition.below(), Direction.UP);
        }

        var target = outputCache.getCapability();
        // Only push into things that accept insertion (e.g. a machine). Cables don't receive; they
        // are left to pull via energyOutput, avoiding double-output.
        if (target != null && target.canReceive()) {
            energyThisTick -= target.receive(energyThisTick, false);
        }
    }
}
