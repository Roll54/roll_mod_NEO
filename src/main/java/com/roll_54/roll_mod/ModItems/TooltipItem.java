package com.roll_54.roll_mod.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Objects;

/**
 * Item that appends N lore lines using keys:
 *   loreitem.roll_mod.<path>.tooltip_line<1..N>
 *
 * Supports HEX colors for the display name and for all lore lines via Builder.
 */
public class TooltipItem extends Item {
    private final int tooltipLines;

    private final boolean hasNameColor;
    private final int nameColorHex;

    private final boolean hasLoreColor;
    private final int loreColorHex;

    private TooltipItem(Properties props, int tooltipLines, Integer nameColorHex, Integer loreColorHex) {
        super(props);
        if (tooltipLines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
        this.tooltipLines = tooltipLines;

        this.hasNameColor = (nameColorHex != null);
        this.nameColorHex = this.hasNameColor ? nameColorHex : 0;

        this.hasLoreColor = (loreColorHex != null);
        this.loreColorHex = this.hasLoreColor ? loreColorHex : 0;
    }

    /** Color the item display name if configured. */
    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        if (hasNameColor) {
            // copy() to avoid mutating shared component instances
            return base.copy().withStyle(s -> s.withColor(nameColorHex));
        }
        return base;
    }

    /** Append N translatable lore lines; tint if configured. */
    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        // registry name: namespace:path
        ResourceLocation key = stack.getItem().builtInRegistryHolder().key().location();
        String path = key.getPath();

        for (int i = 1; i <= tooltipLines; i++) {
            String locKey = "loreitem.roll_mod." + path + ".tooltip_line" + i;
            MutableComponent line = Component.translatable(locKey);
            if (hasLoreColor) {
                line = line.withStyle(style -> style.withColor(loreColorHex));
            }
            tooltip.add(line);
        }
    }

    // -------------------
    //        Builder
    // -------------------
    public static final class Builder {
        private Item.Properties props = new Item.Properties();
        private Integer nameColorHex = null;  // null = default color
        private Integer loreColorHex = null;  // null = default color
        private int tooltipLines = 1;

        public Builder props(Item.Properties props) {
            this.props = Objects.requireNonNull(props);
            return this;
        }

        /** HEX like 0xRRGGBB (e.g., 0xFFAA00). */
        public Builder nameColor(int hex) {
            this.nameColorHex = hex;
            return this;
        }

        /** HEX like 0xRRGGBB (e.g., 0x00C8A0) for ALL lore lines. */
        public Builder loreColor(int hex) {
            this.loreColorHex = hex;
            return this;
        }

        /** Number of translatable lore lines: loreitem.roll_mod.<path>.tooltip_line1..N */
        public Builder tooltipLines(int lines) {
            if (lines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
            this.tooltipLines = lines;
            return this;
        }

        public TooltipItem build() {
            return new TooltipItem(props, tooltipLines, nameColorHex, loreColorHex);
        }
    }
}
