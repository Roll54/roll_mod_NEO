package com.roll_54.roll_mod.items;

import aztech.modern_industrialization.MIText;
import aztech.modern_industrialization.MITooltips;
import aztech.modern_industrialization.items.SteamDrillItem;
import aztech.modern_industrialization.util.TextHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * A netherite-clad {@link SteamDrillItem}. Everything that makes the steam drill what it is — the
 * 3x3 area, the water and fuel upkeep, the silk touch toggle, the netherite-tier harvest level —
 * is inherited untouched. The netherite frame adds two things: the drill survives fire and lava,
 * and it mines {@value #SPEED_BONUS_PERCENT}% faster than the steam drill it is upgraded from.
 */
public class NetheriteSteamDrillItem extends SteamDrillItem {
    /** Mining speed this drill adds on top of the steam drill's own, as a percentage of it. */
    public static final int SPEED_BONUS_PERCENT = 200;

    private static final float SPEED_MULTIPLIER = 1f + SPEED_BONUS_PERCENT / 100f;

    /**
     * The steam drill's "Tool configuration" help block. MI attaches it to its own drill by item
     * identity, so an upgraded drill would otherwise lose it: the lines below are MI's own, taken
     * from {@link MITooltips#STEAM_DRILL}, so the two drills read the same under shift. Held in a
     * field only because the attachment registers itself when it is built.
     */
    @SuppressWarnings("unused")
    private static final MITooltips.TooltipAttachment HELP_TOOLTIP = MITooltips.TooltipAttachment.ofMultilines(
            (stack, item) -> item instanceof NetheriteSteamDrillItem ? List.of(
                    MITooltips.line(MIText.ToolConfiguration).build(),
                    MITooltips.line(MIText.SteamDrillWaterHelp).arg("use", MITooltips.KEYBIND_PARSER).build(),
                    MITooltips.line(MIText.SteamDrillFuelHelp).arg("use", MITooltips.KEYBIND_PARSER).build(),
                    MITooltips.line(MIText.SteamDrillProfit).arg("modern_industrialization.toggle_3x3", MITooltips.KEYBIND_PARSER).build(),
                    MITooltips.line(MIText.SteamDrillToggle).arg("sneak", MITooltips.KEYBIND_PARSER).arg("use", MITooltips.KEYBIND_PARSER)
                            .build())
                    : List.of());

    public NetheriteSteamDrillItem(Properties settings) {
        super(settings.fireResistant());
    }

    /**
     * The bonus applies to the mining speed only. MI returns two fixed values that are not speeds
     * and must come through unscaled: 1 for a block this is the wrong tool for, and 0 when the
     * drill is out of water or fuel. The guard below is the same condition MI uses to pick its
     * speed branch, so the multiplier lands on that branch and nothing else.
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        if (canUse(stack) && isCorrectToolForDrops(stack, state)) {
            speed *= SPEED_MULTIPLIER;
        }
        return speed;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.roll_mod.netherite_steam_drill.speed",
                Component.literal("+" + SPEED_BONUS_PERCENT + "%").withStyle(ChatFormatting.GOLD))
                .setStyle(TextHelper.GRAY_TEXT.withItalic(false)));
    }
}
