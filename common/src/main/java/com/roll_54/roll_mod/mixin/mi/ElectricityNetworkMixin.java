package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.pipes.PipeStatsCollector;
import aztech.modern_industrialization.pipes.api.PipeNetwork;
import aztech.modern_industrialization.pipes.api.PipeNetworkData;
import aztech.modern_industrialization.pipes.electricity.ElectricityNetwork;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.roll_54.roll_mod.compat.mi.energy.DirectEnergyTransfer;
import com.roll_54.roll_mod.compat.mi.energy.ElectricityNetworkEndpoints;
import com.roll_54.roll_mod.compat.mi.energy.PipeNetworkTickingGeneration;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Replaces MI's energy network tick with {@link DirectEnergyTransfer}: only cable blocks touching a machine are
 * visited, and cables store no energy. The original tick is never called (the DimThreads fork's patch of it is then
 * unused but harmless). All state is per network, and a network only ticks on its own level's thread.
 */
@Mixin(value = ElectricityNetwork.class, remap = false)
public abstract class ElectricityNetworkMixin extends PipeNetwork implements ElectricityNetworkEndpoints {
    @Shadow
    @Final
    CableTier tier;

    @Shadow
    @Final
    PipeStatsCollector stats;

    @Unique
    private final List<PipeNetwork.PosNode> roll_mod$endpoints = new ArrayList<>();
    @Unique
    private int roll_mod$seenGeneration = -1;
    @Unique
    private boolean roll_mod$endpointsDirty = true;

    private ElectricityNetworkMixin(int id, PipeNetworkData data) {
        super(id, data);
    }

    @WrapMethod(method = "tick")
    private void roll_mod$directTick(ServerLevel world, Operation<Void> original) {
        Collection<PipeNetwork.PosNode> ticking = this.iterateTickingNodes();
        int generation = ((PipeNetworkTickingGeneration) this).roll_mod$tickingGeneration();
        if (this.roll_mod$endpointsDirty || generation != this.roll_mod$seenGeneration) {
            DirectEnergyTransfer.collectEndpoints(ticking, this.roll_mod$endpoints);
            this.roll_mod$seenGeneration = generation;
            this.roll_mod$endpointsDirty = false;
        }
        DirectEnergyTransfer.tick(world, this.tier, this.stats, this.roll_mod$endpoints);
    }

    @Override
    public void roll_mod$markEndpointsDirty() {
        this.roll_mod$endpointsDirty = true;
    }
}
