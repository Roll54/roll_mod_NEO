package com.roll_54.roll_mod.items.electricItems;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What a {@link DrillOrbItem} holds: merged item types with their counts. Counts go far past a
 * stack (and past the 99 the stack codec allows), so each entry keeps a single-item prototype and
 * the count apart. Immutable, as data components must be.
 */
public record DrillOrbContents(List<Entry> entries) {

    public static final DrillOrbContents EMPTY = new DrillOrbContents(List.of());

    public record Entry(ItemStack prototype, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(Entry::prototype),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(Entry::count)
        ).apply(i, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Entry::prototype,
                ByteBufCodecs.VAR_INT, Entry::count,
                Entry::new);

        public Entry {
            prototype = prototype.copyWithCount(1);
        }

        /** ItemStack has no equals of its own; components compare by value, so entries must too. */
        @Override
        public boolean equals(Object o) {
            return o instanceof Entry e && count == e.count && ItemStack.isSameItemSameComponents(prototype, e.prototype);
        }

        @Override
        public int hashCode() {
            return 31 * ItemStack.hashItemAndComponents(prototype) + count;
        }
    }

    public static final Codec<DrillOrbContents> CODEC =
            Entry.CODEC.listOf().xmap(DrillOrbContents::new, DrillOrbContents::entries);

    public static final StreamCodec<RegistryFriendlyByteBuf, DrillOrbContents> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(DrillOrbContents::new, DrillOrbContents::entries);

    public DrillOrbContents {
        entries = List.copyOf(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int totalCount() {
        int total = 0;
        for (Entry e : entries) total += e.count();
        return total;
    }

    /** Mutable working copy: stacks at their full merged counts, for filling or emptying an orb. */
    public List<ItemStack> toStacks() {
        List<ItemStack> stacks = new ArrayList<>(entries.size());
        for (Entry e : entries) stacks.add(e.prototype().copyWithCount(e.count()));
        return stacks;
    }

    /** Back from a working copy; empty stacks are dropped, same item types merged. */
    public static DrillOrbContents of(List<ItemStack> stacks) {
        List<ItemStack> merged = new ArrayList<>();
        outer:
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            for (ItemStack m : merged) {
                if (ItemStack.isSameItemSameComponents(m, stack)) {
                    m.grow(stack.getCount());
                    continue outer;
                }
            }
            merged.add(stack.copy());
        }
        List<Entry> entries = new ArrayList<>(merged.size());
        for (ItemStack m : merged) entries.add(new Entry(m, m.getCount()));
        return new DrillOrbContents(entries);
    }
}
