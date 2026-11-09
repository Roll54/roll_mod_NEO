package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.pipes.api.PipeNetworkNode;
import aztech.modern_industrialization.pipes.electricity.ElectricityNetworkNode;
import aztech.modern_industrialization.pipes.impl.PipeBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.roll_54.roll_mod.compat.mi.energy.ElectricityNetworkEndpoints;
import com.roll_54.roll_mod.compat.mi.energy.ElectricityNodeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Tells the cable's network when this cable block starts or stops touching a machine, so the network's endpoint list
 * ({@link com.roll_54.roll_mod.compat.mi.energy.DirectEnergyTransfer}) is recollected. Only real changes count:
 * {@code updateConnections} runs on every neighbour update.
 */
@Mixin(value = ElectricityNetworkNode.class, remap = false)
public abstract class ElectricityNetworkNodeMixin extends PipeNetworkNode implements ElectricityNodeAccess {
    @Shadow
    private List<Direction> connections;

    @Shadow
    long eu;

    @Unique
    private void roll_mod$endpointsChanged() {
        if (this.network instanceof ElectricityNetworkEndpoints endpoints) {
            endpoints.roll_mod$markEndpointsDirty();
        }
    }

    @WrapMethod(method = "addConnection")
    private void roll_mod$trackAdd(PipeBlockEntity pipe, Player player, Level world, BlockPos pos, Direction direction,
                                   Operation<Void> original) {
        int before = this.connections.size();
        original.call(pipe, player, world, pos, direction);
        if (this.connections.size() != before) this.roll_mod$endpointsChanged();
    }

    @WrapMethod(method = "removeConnection")
    private void roll_mod$trackRemove(Level world, BlockPos pos, Direction direction, Operation<Void> original) {
        int before = this.connections.size();
        original.call(world, pos, direction);
        if (this.connections.size() != before) this.roll_mod$endpointsChanged();
    }

    @WrapMethod(method = "updateConnections")
    private void roll_mod$trackUpdate(Level world, BlockPos pos, Operation<Void> original) {
        int before = this.connections.size();
        original.call(world, pos);
        if (this.connections.size() != before) this.roll_mod$endpointsChanged();
    }

    @Inject(method = "buildInitialConnections", at = @At("TAIL"))
    private void roll_mod$trackInitial(Level world, BlockPos pos, CallbackInfo ci) {
        this.roll_mod$endpointsChanged();
    }

    @Inject(method = "fromTag", at = @At("TAIL"))
    private void roll_mod$trackLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.roll_mod$endpointsChanged();
    }

    @Override
    public boolean roll_mod$hasConnections() {
        return !this.connections.isEmpty();
    }

    @Override
    public void roll_mod$clearStoredEu() {
        this.eu = 0;
    }
}
