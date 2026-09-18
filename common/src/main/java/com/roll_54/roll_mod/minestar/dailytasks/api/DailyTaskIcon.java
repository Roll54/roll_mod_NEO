package com.roll_54.roll_mod.minestar.dailytasks.api;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import javax.annotation.Nullable;

/**
 * A task's icon: either an item to render, or a PNG to blit.
 *
 * <p>Both variants render in the daily-tasks screen. The vanilla advancement toast can only take an
 * {@link ItemStack}, so a PNG icon falls back to {@link #toastStack()}'s stand-in there.
 */
public sealed interface DailyTaskIcon {

    /**
     * An item, drawn with its usual model and its own vanilla tooltip.
     *
     * @param label what to draw where the stack count goes, or {@code null} to let the stack draw
     *              its own count. A rolled payout captions itself with its range — see
     *              {@link #ofRange(ItemStack, int, int)} — because the count it will pay does not
     *              exist yet at the time the strip is drawn.
     */
    record Item(ItemStack stack, @Nullable String label) implements DailyTaskIcon {

        /** The plain item icon: whatever count the stack carries, drawn the vanilla way. */
        public Item(ItemStack stack) {
            this(stack, null);
        }

        @Override
        public IGuiTexture texture() {
            return label == null ? new ItemStackTexture(stack) : new ItemLabelTexture(stack, label);
        }

        @Override
        public ItemStack toastStack() {
            // Tasks hold their icon in a static field, so never hand out the shared instance.
            return stack.copy();
        }
    }

    /**
     * A texture, e.g. {@code roll_mod:textures/gui/daily_tasks/my_task.png}. Must be a full resource
     * path, the same form {@link SpriteTexture#of(ResourceLocation)} takes.
     */
    record Texture(ResourceLocation png, ItemStack toastFallback) implements DailyTaskIcon {
        @Override
        public IGuiTexture texture() {
            return SpriteTexture.of(png);
        }

        @Override
        public ItemStack toastStack() {
            return toastFallback.copy();
        }
    }

    /** How the icon draws in the daily-tasks screen. */
    IGuiTexture texture();

    /** What the vanilla advancement toast shows; a PNG icon substitutes a stand-in item. */
    ItemStack toastStack();

    /** {@code true} when this is an item icon, which the screen renders as a real item slot. */
    default boolean isItem() {
        return this instanceof Item;
    }

    static DailyTaskIcon of(ItemLike item) {
        return new Item(new ItemStack(item));
    }

    static DailyTaskIcon of(ItemStack stack) {
        return new Item(stack);
    }

    /**
     * An item captioned with the range it pays, e.g. {@code "1-4"}, instead of a count — for a
     * reward that rolls its amount. The two bounds are drawn exactly as given, so they should be
     * the same pair the roll uses; {@link RewardRange} keeps the two ends together so they cannot
     * drift apart.
     */
    static DailyTaskIcon ofRange(ItemLike item, int min, int max) {
        return ofRange(new ItemStack(item), min, max);
    }

    /** {@link #ofRange(ItemLike, int, int)} for a stack that carries components. */
    static DailyTaskIcon ofRange(ItemStack stack, int min, int max) {
        // Count 1: the caption stands in for the count, and a stack carrying its own would draw
        // both, one over the other.
        return new Item(stack.copyWithCount(1), rangeLabel(min, max));
    }

    /** {@code "1-4"}, or just {@code "4"} when the two ends are the same. */
    static String rangeLabel(int min, int max) {
        return min == max ? Integer.toString(min) : min + "-" + max;
    }

    /** A PNG icon, with {@link Items#PAPER} standing in on the advancement toast. */
    static DailyTaskIcon of(ResourceLocation png) {
        return new Texture(png, new ItemStack(Items.PAPER));
    }

    /** A PNG icon with an explicit item to show on the advancement toast. */
    static DailyTaskIcon of(ResourceLocation png, ItemLike toastFallback) {
        return new Texture(png, new ItemStack(toastFallback));
    }
}
