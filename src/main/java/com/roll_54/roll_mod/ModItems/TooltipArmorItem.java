package com.roll_54.roll_mod.ModItems;

import com.roll_54.roll_mod.Util.TooltipManager;
import com.roll_54.roll_mod.Util.TooltipOptions;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TooltipArmorItem extends ArmorItem {
    private final TooltipOptions opts;

    public TooltipArmorItem(Holder<ArmorMaterial> material, Type type, Properties props, TooltipOptions opts) {
        super(material, type, props);
        this.opts = (opts == null) ? TooltipOptions.NONE : opts;
    }

    @Override
    public Component getName(ItemStack stack) {
        return TooltipManager.colorName(super.getName(stack), opts);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        TooltipManager.addLore(stack, opts, tooltip, flag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return opts != null && opts.glow();
    }

    // ---------------- BUILDER ----------------
    public static class Builder {
        private final Holder<ArmorMaterial> material;
        private final ArmorItem.Type type;
        private final Properties props;
        private int tooltipLines = 0;
        private Integer nameColorHex = null;
        private Integer loreColorHex = null;
        private boolean textureGlow = false;

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

        /** Вмикає або вимикає ванільне світіння текстури */
        public Builder textureGlow(boolean glow) {
            this.textureGlow = glow;
            return this;
        }

        public TooltipArmorItem build() {
            return new TooltipArmorItem(material, type, props,
                    new TooltipOptions(tooltipLines, nameColorHex, loreColorHex, textureGlow));
        }
    }
}
