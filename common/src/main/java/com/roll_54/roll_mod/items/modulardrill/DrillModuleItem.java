package com.roll_54.roll_mod.items.modulardrill;

import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A drill module: one item per (type, tier). The item is stateless — a drill stores installed
 * modules as item ids, and the installation table reconstructs the item from the id when a module
 * is taken back out, so nothing is lost in the round trip.
 *
 * <p>{@code value} means something different per type: speed multiplier for SPEED, fortune level
 * for FORTUNE, added blocks of reach for REACH, capacity multiplier for BATTERY, area radius for
 * AOE. On a saber: damage multiplier for DAMAGE, added attack speed for ATTACK_SPEED, radius for
 * SWEEP, level for LOOTING and IRRADIATION, XP multiplier for XP_MULTIPLIER, and a fraction for
 * VAMPIRISM (of damage healed) and BEHEADING (chance of a head). Unused otherwise.
 *
 * <p>{@code costFactor} multiplies the tool's energy cost while the module is installed — per
 * block in a drill, per hit in a saber (BURN's 0.9 is the discount the voided drops pay for). A
 * shared module may carry a separate {@code saberCostFactor}: Fortune is Fortune in a drill and
 * Looting in a saber, and each keeps its own balance. {@code complexity} is what it takes from the
 * tool's complexity budget.
 *
 * <p>No voltage requirement, deliberately: any module fits any drill with a free slot. Progression
 * is balanced through the crafting recipes, not through code locks.
 */
public class DrillModuleItem extends Item {

    /** Complexity a module costs per tier, when it names none of its own. */
    public static final int COMPLEXITY_PER_TIER = 3;

    public final ModuleType type;
    public final int tier;
    public final double value;
    /**
     * {@link #value} as a saber reads it. Differs only for a shared module whose effect changes with
     * the tool: Speed is a mining multiplier in a drill and added attack speed in a saber.
     */
    public final double saberValue;
    /** The energy cost factor in a drill (per block), and in a saber unless {@link #saberCostFactor} says otherwise. */
    public final double costFactor;
    /** The energy cost factor in a saber (per hit). Differs only for a shared module balanced per tool. */
    public final double saberCostFactor;
    /**
     * How much of the tool's complexity budget the module takes; negative gives some back (Speak).
     * See {@link DrillModuleHelper#canInstall}.
     */
    public final int complexity;

    /** Complexity {@code 3 × tier}, the same cost factor in every tool. */
    public DrillModuleItem(ModuleType type, int tier, double value, double costFactor, Properties properties) {
        this(type, tier, value, costFactor, costFactor, COMPLEXITY_PER_TIER * tier, properties);
    }

    public DrillModuleItem(ModuleType type, int tier, double value, double costFactor, int complexity,
                           Properties properties) {
        this(type, tier, value, costFactor, costFactor, complexity, properties);
    }

    public DrillModuleItem(ModuleType type, int tier, double value, double costFactor, double saberCostFactor,
                           int complexity, Properties properties) {
        this(type, tier, value, value, costFactor, saberCostFactor, complexity, properties);
    }

    public DrillModuleItem(ModuleType type, int tier, double value, double saberValue, double costFactor,
                           double saberCostFactor, int complexity, Properties properties) {
        super(properties.stacksTo(16));
        this.type = type;
        this.tier = tier;
        this.value = value;
        this.saberValue = saberValue;
        this.costFactor = costFactor;
        this.saberCostFactor = saberCostFactor;
        this.complexity = complexity;
    }

    /** The energy cost factor this module applies in this kind of tool. */
    public double costFactor(ToolKind tool) {
        return tool == ToolKind.SABER ? saberCostFactor : costFactor;
    }

    /** A placeholder name while the client hides new items — see {@link NewItemVisibility}. */
    @Override
    public Component getName(ItemStack stack) {
        return NewItemVisibility.name(super.getName(stack));
    }

    /**
     * The effect line always; with Shift, a Parameters block (tier, energy, complexity, fitting
     * tools, conflicts, the Trash Filter's fill) and then the Description lines.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        // Speed is the one module whose number differs per tool, so its line carries both.
        Object[] args = type == ModuleType.SPEED ? new Object[]{value, saberValue} : new Object[]{tooltipArg()};
        tooltip.add(Component.translatable(type.tooltipKey(), args).withStyle(ChatFormatting.AQUA));
        if (type.shared()) {
            tooltip.add(Component.translatable("tooltip.roll_mod.module.fits_both")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.roll_mod.general_press_shift"));
            return;
        }

        tooltip.add(Component.translatable("tooltip.roll_mod.module.parameters").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("tooltip.roll_mod.module.tier", tier).withStyle(ChatFormatting.GRAY));
        if (type != ModuleType.OVERCHARGE) {
            if (costFactor == saberCostFactor) {
                costLine(tooltip, costFactor, "");
            } else {
                // A shared module balanced per tool: one line for each.
                costLine(tooltip, costFactor, "_drill");
                costLine(tooltip, saberCostFactor, "_saber");
            }
            if (costFactor == 1.0 && saberCostFactor == 1.0) {
                tooltip.add(Component.translatable("tooltip.roll_mod.module.cost_none")
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        tooltip.add(Component.translatable("tooltip.roll_mod.module.complexity",
                        (complexity > 0 ? "+" : "") + complexity)
                .withStyle(complexity < 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.roll_mod.module.tools", toolNames())
                .withStyle(ChatFormatting.GRAY));
        Set<ModuleType> conflicts = DrillModuleHelper.conflictsOf(type);
        if (!conflicts.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.roll_mod.module.conflicts", typeNames(conflicts))
                    .withStyle(ChatFormatting.RED));
        }
        if (type == ModuleType.TRASH_FILTER) {
            int filled = DrillModuleHelper.filterItems(stack.get(ComponentsRegistry.TRASH_FILTER.get())).size();
            tooltip.add(Component.translatable("tooltip.roll_mod.module.filter",
                            filled, DrillModuleHelper.TRASH_FILTER_SLOTS)
                    .withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.translatable("tooltip.roll_mod.module.description").withStyle(ChatFormatting.WHITE));
        detailLines(tooltip);
        tooltip.add(Component.translatable("tooltip.roll_mod.module.install_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** "Drill", "Saber" or "Drill, Saber" — the tools this module's type fits. */
    private Component toolNames() {
        MutableComponent names = Component.empty();
        for (ToolKind tool : ToolKind.values()) {
            if (!type.fits(tool)) continue;
            if (!names.getSiblings().isEmpty()) names.append(", ");
            names.append(Component.translatable("tooltip.roll_mod.module.tool." + tool.name().toLowerCase(Locale.ROOT)));
        }
        return names;
    }

    /** The types' short names ({@code <tooltipKey>.name}), comma-separated. */
    private static Component typeNames(Set<ModuleType> types) {
        MutableComponent names = Component.empty();
        for (ModuleType other : types) {
            if (!names.getSiblings().isEmpty()) names.append(", ");
            names.append(Component.translatable(other.tooltipKey() + ".name"));
        }
        return names;
    }

    /**
     * The Shift description: {@code <tooltipKey>.desc.1}, {@code .desc.2}, … for as long as the lang
     * file has them, so a line is added or dropped in the lang files alone.
     */
    private void detailLines(List<Component> tooltip) {
        String prefix = type.tooltipKey() + ".desc.";
        Language language = Language.getInstance();
        for (int i = 1; language.has(prefix + i); i++) {
            tooltip.add(Component.translatable(prefix + i).withStyle(ChatFormatting.GRAY));
        }
    }

    /** "+n% energy per …" (or "-n%"), with {@code suffix} naming the tool when the two differ. */
    private static void costLine(List<Component> tooltip, double factor, String suffix) {
        if (factor == 1.0) return;
        String key = (factor < 1.0 ? "tooltip.roll_mod.module.cost_discount" : "tooltip.roll_mod.module.cost_factor")
                + suffix;
        long percent = Math.round(Math.abs(factor - 1.0) * 100);
        tooltip.add(Component.translatable(key, percent).withStyle(ChatFormatting.AQUA));
    }

    /** The number the effect line shows: an area edge for AOE, a whole number where one fits. */
    private Object tooltipArg() {
        return switch (type) {
            case AOE -> (int) value * 2 + 1;
            case FORTUNE, REACH -> (int) value;
            case BATTERY, DAMAGE -> Math.round((value - 1.0) * 100);
            case SPEED, ATTACK_SPEED, SWEEP, XP_MULTIPLIER -> value;
            case LOOTING, IRRADIATION -> (int) value;
            case VAMPIRISM, BEHEADING -> Math.round(value * 100);
            default -> "";
        };
    }
}
