package com.roll_54.roll_mod.minestar.dailytasks.api;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * A task's icon: either an item to render, or a PNG to blit.
 *
 * <p>Both variants render in the daily-tasks screen. The vanilla advancement toast can only take an
 * {@link ItemStack}, so a PNG icon falls back to {@link #toastStack()}'s stand-in there.
 */
public sealed interface DailyTaskIcon {

    /** An item, drawn with its usual model and its own vanilla tooltip. */
    record Item(ItemStack stack) implements DailyTaskIcon {
        @Override
        public IGuiTexture texture() {
            return new ItemStackTexture(stack);
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

    /** A PNG icon, with {@link Items#PAPER} standing in on the advancement toast. */
    static DailyTaskIcon of(ResourceLocation png) {
        return new Texture(png, new ItemStack(Items.PAPER));
    }

    /** A PNG icon with an explicit item to show on the advancement toast. */
    static DailyTaskIcon of(ResourceLocation png, ItemLike toastFallback) {
        return new Texture(png, new ItemStack(toastFallback));
    }
}
