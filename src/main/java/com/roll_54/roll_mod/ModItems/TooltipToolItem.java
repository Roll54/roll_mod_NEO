/*
package com.roll_54.roll_mod.ModItems;

import com.roll_54.roll_mod.Roll_mod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

*/
/**
 * Tool item з кольоровою назвою/лором і Builder'ом.
 * Додає атрибути атаки/швидкості (MAINHAND) через DataComponents.
 * Підтримує опційний Tool-компонент (ванільні правила інструментів).
 *//*

public class TooltipToolItem extends Item {
    // lore
    private final int tooltipLines;
    private final boolean hasNameColor;
    private final int nameColorHex;
    private final boolean hasLoreColor;
    private final int loreColorHex;

    private TooltipToolItem(Properties props, int tooltipLines, Integer nameColorHex, Integer loreColorHex) {
        super(props);
        if (tooltipLines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
        this.tooltipLines = tooltipLines;

        this.hasNameColor = (nameColorHex != null);
        this.nameColorHex = this.hasNameColor ? nameColorHex : 0;

        this.hasLoreColor = (loreColorHex != null);
        this.loreColorHex = this.hasLoreColor ? loreColorHex : 0;
    }

    // Колір назви
    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        return hasNameColor ? base.copy().withStyle(s -> s.withColor(nameColorHex)) : base;
    }

    // Лор
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        // loreitem.roll_mod.<path>.tooltip_line<N>
        var key = stack.getItem().builtInRegistryHolder().key().location();
        String path = key.getPath();
        for (int i = 1; i <= tooltipLines; i++) {
            String locKey = "loreitem.roll_mod." + path + ".tooltip_line" + i;
            Component line = Component.translatable(locKey);
            if (hasLoreColor) line = line.copy().withStyle(s -> s.withColor(loreColorHex));
            tooltip.add(line);
        }
    }

    // -------------------
    //        Builder
    // -------------------
    public static final class Builder {
        private Item.Properties props = new Item.Properties();
        private Integer nameColorHex = null;
        private Integer loreColorHex = null;
        private int tooltipLines = 1;

        // атрибути зброї
        private Double attackDamage = null; // +damage (ADD_VALUE)
        private Double attackSpeed  = null; // +speed  (ADD_VALUE)

        // ідентифікатори модифікаторів (щоб не конфліктувати між предметами)
        private ResourceLocation dmgId = ResourceLocation.fromNamespaceAndPath(Roll_mod.MODID, "tool_attack_damage");
        private ResourceLocation spdId = ResourceLocation.fromNamespaceAndPath(Roll_mod.MODID, "tool_attack_speed");

        // опційний Tool-компонент (ванільні правила інструментів/майнінгу)
        private Consumer<Tool.Builder> toolConfigurator = null;

        public Builder props(Item.Properties props) {
            this.props = Objects.requireNonNull(props);
            return this;
        }

        public Builder nameColor(int hex) {
            this.nameColorHex = hex; return this;
        }

        public Builder loreColor(int hex) {
            this.loreColorHex = hex; return this;
        }

        public Builder tooltipLines(int lines) {
            if (lines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
            this.tooltipLines = lines; return this;
        }

        */
/** Скільки додати до базового урону (ваніл. ADD_VALUE) *//*

        public Builder attackDamage(double dmg) {
            this.attackDamage = dmg; return this;
        }

        */
/** Скільки додати/відняти від швидкості атаки (ваніл. ADD_VALUE, типово мечі мають від’ємне) *//*

        public Builder attackSpeed(double spd) {
            this.attackSpeed = spd; return this;
        }

        */
/** За потреби свій id для модифікатора урону (щоб не конфліктував між різними айтемами) *//*

        public Builder damageModifierId(ResourceLocation id) {
            this.dmgId = Objects.requireNonNull(id); return this;
        }

        */
/** За потреби свій id для модифікатора швидкості *//*

        public Builder speedModifierId(ResourceLocation id) {
            this.spdId = Objects.requireNonNull(id); return this;
        }

        */
/** Налаштувати Tool-компонент (майнінг/ефективності/рули), якщо потрібно *//*

        public Builder tool(Consumer<Tool.Builder> configurator) {
            this.toolConfigurator = Objects.requireNonNull(configurator);
            return this;
        }

        public TooltipToolItem build() {
            Item.Properties out = this.props;

            // 1) Атрибути (MAINHAND)
            ItemAttributeModifiers.Builder attr = ItemAttributeModifiers.builder();
            if (attackDamage != null) {
                attr.add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(dmgId, attackDamage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                );
            }
            if (attackSpeed != null) {
                attr.add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(spdId, attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                );
            }
            // додати, тільки якщо щось задали
            ItemAttributeModifiers builtAttr = attr.build();
            if (!builtAttr.modifiers().isEmpty()) {
                out = out.component(DataComponents.ATTRIBUTE_MODIFIERS, builtAttr);
            }

            // 2) Опційний Tool-компонент
            if (toolConfigurator != null) {
                Tool.Builder tb = Tool.builder();
                toolConfigurator.accept(tb);
                out = out.component(DataComponents.TOOL, tb.build());
            }

            return new TooltipToolItem(out, tooltipLines, nameColorHex, loreColorHex);
        }
    }
}
*/
