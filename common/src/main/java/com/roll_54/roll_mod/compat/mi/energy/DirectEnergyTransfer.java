package com.roll_54.roll_mod.compat.mi.energy;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import aztech.modern_industrialization.pipes.PipeStatsCollector;
import aztech.modern_industrialization.pipes.api.PipeNetwork;
import aztech.modern_industrialization.pipes.electricity.ElectricityNetworkNode;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * MI energy cables, endpoint-only and without storage.
 * <p>
 * MI ticked every cable block of a network every tick (to collect the machines next to it) and kept a buffer of up to
 * the tier's max transfer in each cable block: extract into the buffer, insert from the buffer. Here a network only
 * looks at the cable blocks that touch a machine (its endpoints) and moves energy straight from the machines that can
 * give it to the machines that can take it, in the same tick. Like item pipes, the cable blocks in between only make
 * the connection.
 * <p>
 * Kept from MI: at most the tier's max transfer per network per tick, and MI's fair split (shuffle, sort by the
 * simulated amount, divide the rest evenly). Differences: a machine touching the network through two cable blocks is
 * counted once (MI gave it two shares); nothing is pulled from generators when no consumer wants energy that tick.
 */
public final class DirectEnergyTransfer {
    private DirectEnergyTransfer() {}

    @FunctionalInterface
    private interface Operation {
        long transfer(MIEnergyStorage storage, long maxAmount, boolean simulate);
    }

    private static final Operation EXTRACT = MIEnergyStorage::extract;
    private static final Operation RECEIVE = MIEnergyStorage::receive;

    /** Rebuilds a network's endpoint list from its ticking nodes, and drops any EU MI stored in them. */
    public static void collectEndpoints(Collection<PipeNetwork.PosNode> tickingNodes, List<PipeNetwork.PosNode> endpoints) {
        endpoints.clear();
        for (PipeNetwork.PosNode entry : tickingNodes) {
            ElectricityNodeAccess node = (ElectricityNodeAccess) entry.getNode();
            node.roll_mod$clearStoredEu();
            if (node.roll_mod$hasConnections()) {
                endpoints.add(entry);
            }
        }
    }

    public static void tick(ServerLevel world, CableTier tier, PipeStatsCollector stats, List<PipeNetwork.PosNode> endpoints) {
        long extracted = 0;
        long inserted = 0;

        if (!endpoints.isEmpty()) {
            List<MIEnergyStorage> found = new ArrayList<>();
            for (PipeNetwork.PosNode entry : endpoints) {
                // MI's own per-node capability caches; adds only storages that accept this tier.
                ((ElectricityNetworkNode) entry.getNode()).appendAttributes(world, entry.getPos(), tier, found);
            }

            ReferenceOpenHashSet<MIEnergyStorage> seen = new ReferenceOpenHashSet<>(found.size());
            List<MIEnergyStorage> sources = new ArrayList<>();
            List<MIEnergyStorage> sinks = new ArrayList<>();
            for (MIEnergyStorage storage : found) {
                if (!seen.add(storage)) continue;
                if (storage.canExtract()) sources.add(storage);
                if (storage.canReceive()) sinks.add(storage);
            }

            if (!sources.isEmpty() && !sinks.isEmpty()) {
                long maxTransfer = tier.getMaxTransfer();
                long demand = 0;
                for (MIEnergyStorage sink : sinks) {
                    demand += sink.receive(maxTransfer, true);
                    if (demand >= maxTransfer) {
                        demand = maxTransfer;
                        break;
                    }
                }

                if (demand > 0) {
                    extracted = transferForTargets(EXTRACT, sources, demand);
                    if (extracted > 0) {
                        inserted = transferForTargets(RECEIVE, sinks, extracted);
                        if (inserted < extracted) {
                            // A consumer took less than it simulated; hand the rest back to where it came from.
                            transferForTargets(RECEIVE, sources, extracted - inserted);
                        }
                    }
                }
            }
        }

        stats.addValues(extracted, inserted);
    }

    /** MI's {@code ElectricityNetwork.transferForTargets}, unchanged. */
    private static long transferForTargets(Operation operation, List<MIEnergyStorage> targets, long maxAmount) {
        List<Target> sortable = new ArrayList<>(targets.size());
        for (MIEnergyStorage target : targets) {
            sortable.add(new Target(target));
        }
        Collections.shuffle(sortable);
        for (Target target : sortable) {
            target.simulationResult = operation.transfer(target.storage, maxAmount, true);
        }
        sortable.sort(Comparator.comparingLong(t -> t.simulationResult));

        long transferred = 0;
        for (int i = 0; i < sortable.size(); i++) {
            int remainingTargets = sortable.size() - i;
            long remainingAmount = maxAmount - transferred;
            transferred += operation.transfer(sortable.get(i).storage, remainingAmount / remainingTargets, false);
        }
        return transferred;
    }

    private static final class Target {
        final MIEnergyStorage storage;
        long simulationResult;

        Target(MIEnergyStorage storage) {
            this.storage = storage;
        }
    }
}
