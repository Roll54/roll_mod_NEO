package com.roll_54.roll_mod.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TooltipArmorItem extends ArmorItem {
    private final int tooltipLines;
    private final Integer nameColorHex;
    private final Integer loreColorHex;

    public TooltipArmorItem(Holder<ArmorMaterial> material, Type type, Properties props,
                            int tooltipLines, Integer nameColorHex, Integer loreColorHex) {
        super(material, type, props);
        this.tooltipLines = tooltipLines;
        this.nameColorHex = nameColorHex;
        this.loreColorHex = loreColorHex;
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent base = (MutableComponent) super.getName(stack);
        if (nameColorHex != null) {
            return base.withStyle(style -> style.withColor(nameColorHex));
        }
        return base;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation key = stack.getItem().builtInRegistryHolder().key().location();
        String path = key.getPath();

        for (int i = 1; i <= tooltipLines; i++) {
            String locKey = "loreitem.roll_mod." + path + ".tooltip_line" + i;
            MutableComponent line = Component.translatable(locKey);
            if (loreColorHex != null) {
                line = line.withStyle(style -> style.withColor(loreColorHex));
            }
            tooltip.add(line);
        }
    }

    // Builder
    public static class Builder {
        private final Holder<ArmorMaterial> material;
        private final ArmorItem.Type type;
        private final Properties props;
        private int tooltipLines = 0;
        private Integer nameColorHex = null;
        private Integer loreColorHex = null;

        public Builder(Holder<ArmorMaterial> material, ArmorItem.Type type, Properties props) {
            this.material = material;
            this.type = type;
            this.props = props;
        }

        public Builder tooltipLines(int lines) {
            this.tooltipLines = lines;
            return this;
        }

        public Builder nameColor(int hex) {
            this.nameColorHex = hex;
            return this;
        }

        public Builder loreColor(int hex) {
            this.loreColorHex = hex;
            return this;
        }

        public TooltipArmorItem build() {
            return new TooltipArmorItem(material, type, props, tooltipLines, nameColorHex, loreColorHex);
        }
    }
}
