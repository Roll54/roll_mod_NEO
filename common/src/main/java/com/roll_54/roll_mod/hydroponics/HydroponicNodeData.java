package com.roll_54.roll_mod.hydroponics;

import com.roll_54.roll_mod.blocks.entity.HydroponicGardenBedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-level registry of {@link HydroponicNode}s.
 *
 * <p>Shared state deliberately does not live on an elected "master" block entity: a 5x5 field spans
 * up to four chunks, and a master whose chunk unloaded would strand the rest of the group. Level
 * saved data has neither problem — it is always loaded, always saved, and a bed only has to
 * remember its node id.
 *
 * <p>Beds merge only with the four horizontal neighbours at the same Y, up to
 * {@link HydroponicNode#MAX_MEMBERS} members.
 */
public class HydroponicNodeData extends SavedData {

    public static final String NAME = "roll_mod_hydroponics";

    private static final Direction[] NEIGHBOURS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private final Map<Integer, HydroponicNode> nodes = new HashMap<>();
    private final Map<BlockPos, Integer> membership = new HashMap<>();
    private int nextId = 1;

    public static HydroponicNodeData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(HydroponicNodeData::new, HydroponicNodeData::load), NAME);
    }

    @Nullable
    public HydroponicNode node(int id) {
        return nodes.get(id);
    }

    @Nullable
    public HydroponicNode nodeAt(BlockPos pos) {
        Integer id = membership.get(pos.immutable());
        return id == null ? null : nodes.get(id);
    }

    // ---------------------------------------------------------------- placement

    /**
     * Attaches a newly placed (or newly loaded but unassigned) bed, merging with whatever is already
     * next to it. A bed that joins an existing group inherits that group's settings; a bed that
     * cannot join one — because merging would exceed the 25 cap — still copies the settings, so a
     * field that outgrew a node still looks and behaves consistently.
     */
    public HydroponicNode attach(ServerLevel level, BlockPos pos) {
        BlockPos at = pos.immutable();
        HydroponicNode existing = nodeAt(at);
        if (existing != null) {
            return existing;
        }

        List<HydroponicNode> neighbours = neighbourNodes(at);
        HydroponicNode result;

        if (neighbours.isEmpty()) {
            result = createNode();
        } else {
            HydroponicNode primary = largest(neighbours);
            int combined = neighbours.stream().mapToInt(HydroponicNode::size).sum() + 1;
            if (combined <= HydroponicNode.MAX_MEMBERS) {
                for (HydroponicNode other : neighbours) {
                    if (other != primary) {
                        mergeInto(level, at, primary, other);
                    }
                }
                result = primary;
            } else {
                HydroponicNode joinable = neighbours.stream()
                        .filter(node -> node.size() < HydroponicNode.MAX_MEMBERS)
                        .max(Comparator.comparingInt(HydroponicNode::size))
                        .orElse(null);
                if (joinable != null) {
                    result = joinable;
                } else {
                    result = createNode();
                    result.setSettings(primary.settings());
                }
            }
        }

        result.members().add(at);
        membership.put(at, result.id());
        result.clampBuffers();
        setDirty();
        syncMembers(level, result);
        return result;
    }

    /** Folds {@code other} entirely into {@code primary} and deletes it. */
    private void mergeInto(ServerLevel level, BlockPos origin, HydroponicNode primary, HydroponicNode other) {
        primary.absorbBuffers(other);
        for (BlockPos member : other.members()) {
            primary.members().add(member);
            membership.put(member, primary.id());
        }
        // The merged-away node's slots would otherwise vanish silently: hand each one to the
        // survivor's matching slot and drop whatever does not fit.
        for (int slot = 0; slot < other.inputs().getSlots(); slot++) {
            ItemStack stack = other.inputs().getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            other.inputs().setStackInSlot(slot, ItemStack.EMPTY);
            ItemStack leftover = primary.inputs().insertItem(slot, stack, false);
            if (!leftover.isEmpty()) {
                Containers.dropItemStack(level, origin.getX() + 0.5D, origin.getY() + 0.5D,
                        origin.getZ() + 0.5D, leftover);
            }
        }
        nodes.remove(other.id());
    }

    // ---------------------------------------------------------------- removal

    /**
     * Detaches a broken bed. Whatever is left is re-partitioned into connected components: the
     * largest keeps the node (and the input slots), the others get new nodes with a proportional
     * share of every buffer and a copy of the settings.
     */
    public void detach(ServerLevel level, BlockPos pos) {
        BlockPos at = pos.immutable();
        Integer id = membership.remove(at);
        if (id == null) {
            return;
        }
        HydroponicNode node = nodes.get(id);
        if (node == null) {
            setDirty();
            return;
        }
        node.members().remove(at);
        setDirty();

        if (node.members().isEmpty()) {
            dropInputs(level, at, node);
            nodes.remove(id);
            return;
        }

        List<Set<BlockPos>> components = split(node.members());
        if (components.size() <= 1) {
            node.clampBuffers();
            syncMembers(level, node);
            return;
        }

        // Biggest first; ties broken by the lowest position so the outcome is deterministic.
        components.sort((a, b) -> a.size() != b.size()
                ? Integer.compare(b.size(), a.size())
                : Long.compare(lowest(a).asLong(), lowest(b).asLong()));

        int total = node.members().size();
        int acidity = node.acidity();
        int water = node.water();
        long energy = node.energy();
        int fertilizer = node.fertilizer();
        String fertilizerFluid = node.fertilizerFluidId();

        int usedAcidity = 0;
        int usedWater = 0;
        long usedEnergy = 0L;
        int usedFertilizer = 0;

        List<HydroponicNode> results = new ArrayList<>(components.size());
        for (int i = 1; i < components.size(); i++) {
            Set<BlockPos> component = components.get(i);
            HydroponicNode fresh = createNode();
            fresh.setSettings(node.settings());
            fresh.members().addAll(component);
            for (BlockPos member : component) {
                membership.put(member, fresh.id());
            }
            int shareAcidity = (int) ((long) acidity * component.size() / total);
            int shareWater = (int) ((long) water * component.size() / total);
            long shareEnergy = energy * component.size() / total;
            int shareFertilizer = (int) ((long) fertilizer * component.size() / total);
            fresh.setBuffers(shareAcidity, shareWater, shareEnergy, shareFertilizer, fertilizerFluid);
            fresh.clampBuffers();
            usedAcidity += shareAcidity;
            usedWater += shareWater;
            usedEnergy += shareEnergy;
            usedFertilizer += shareFertilizer;
            results.add(fresh);
        }

        // The biggest component keeps the original node, the slots, and every rounding remainder.
        Set<BlockPos> keep = components.get(0);
        node.members().clear();
        node.members().addAll(keep);
        for (BlockPos member : keep) {
            membership.put(member, node.id());
        }
        node.setBuffers(acidity - usedAcidity, water - usedWater, energy - usedEnergy,
                fertilizer - usedFertilizer, fertilizerFluid);
        node.clampBuffers();
        results.add(node);

        for (HydroponicNode result : results) {
            syncMembers(level, result);
        }
    }

    private void dropInputs(ServerLevel level, BlockPos pos, HydroponicNode node) {
        for (int slot = 0; slot < node.inputs().getSlots(); slot++) {
            ItemStack stack = node.inputs().getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, stack);
                node.inputs().setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    private HydroponicNode createNode() {
        HydroponicNode node = new HydroponicNode(nextId++, this::setDirty);
        nodes.put(node.id(), node);
        setDirty();
        return node;
    }

    private List<HydroponicNode> neighbourNodes(BlockPos pos) {
        List<HydroponicNode> found = new ArrayList<>(4);
        for (Direction direction : NEIGHBOURS) {
            HydroponicNode node = nodeAt(pos.relative(direction));
            if (node != null && !found.contains(node)) {
                found.add(node);
            }
        }
        return found;
    }

    private static HydroponicNode largest(List<HydroponicNode> candidates) {
        HydroponicNode best = candidates.get(0);
        for (HydroponicNode candidate : candidates) {
            if (candidate.size() > best.size()
                    || (candidate.size() == best.size()
                    && lowest(candidate.members()).compareTo(lowest(best.members())) < 0)) {
                best = candidate;
            }
        }
        return best;
    }

    private static BlockPos lowest(Set<BlockPos> members) {
        BlockPos best = null;
        for (BlockPos pos : members) {
            if (best == null || pos.asLong() < best.asLong()) {
                best = pos;
            }
        }
        return best == null ? BlockPos.ZERO : best;
    }

    /** Flood-fills {@code members} over horizontal adjacency into connected components. */
    private static List<Set<BlockPos>> split(Set<BlockPos> members) {
        List<Set<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> remaining = new HashSet<>(members);
        Deque<BlockPos> queue = new ArrayDeque<>();
        while (!remaining.isEmpty()) {
            BlockPos seed = remaining.iterator().next();
            remaining.remove(seed);
            Set<BlockPos> component = new LinkedHashSet<>();
            component.add(seed);
            queue.add(seed);
            while (!queue.isEmpty()) {
                BlockPos current = queue.removeFirst();
                for (Direction direction : NEIGHBOURS) {
                    BlockPos next = current.relative(direction);
                    if (remaining.remove(next)) {
                        component.add(next);
                        queue.add(next);
                    }
                }
            }
            components.add(component);
        }
        return components;
    }

    /** Pushes the node's settings onto every loaded member so clients see the shared state. */
    public static void syncMembers(ServerLevel level, HydroponicNode node) {
        for (BlockPos member : node.members()) {
            if (!level.isLoaded(member)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(member);
            if (blockEntity instanceof HydroponicGardenBedBlockEntity bed) {
                bed.adoptNode(node);
            }
        }
    }

    // ---------------------------------------------------------------- persistence

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("next_id", nextId);
        ListTag list = new ListTag();
        for (HydroponicNode node : nodes.values()) {
            list.add(node.save(registries));
        }
        tag.put("nodes", list);
        return tag;
    }

    public static HydroponicNodeData load(CompoundTag tag, HolderLookup.Provider registries) {
        HydroponicNodeData data = new HydroponicNodeData();
        data.nextId = Math.max(1, tag.getInt("next_id"));
        ListTag list = tag.getList("nodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            HydroponicNode node = HydroponicNode.load(list.getCompound(i), registries, data::setDirty);
            data.nodes.put(node.id(), node);
            for (BlockPos member : node.members()) {
                data.membership.put(member, node.id());
            }
            data.nextId = Math.max(data.nextId, node.id() + 1);
        }
        return data;
    }
}
