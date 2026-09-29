package com.roll_54.roll_mod.items.modulardrill;

/**
 * What a module does. A tool holds at most one module of each type; everything else about a
 * module — its tier, its magnitude, its cost factor — lives on the {@link DrillModuleItem}.
 *
 * <p>Each type names the tools it fits. Battery, Eco and the XP Reactor mean the same thing on a
 * drill and a saber, so one item serves both; everything else belongs to one tool.
 */
public enum ModuleType {
    // Mining speed in a drill, attack speed in a saber — one module family for both.
    SPEED(ToolKind.DRILL, ToolKind.SABER),
    SILK_TOUCH(ToolKind.DRILL),
    // Fortune in a drill, Looting in a saber — one module family for both.
    FORTUNE(ToolKind.DRILL, ToolKind.SABER),
    XP_REACTOR(ToolKind.DRILL, ToolKind.SABER),
    OVERCHARGE(ToolKind.DRILL),
    REACH(ToolKind.DRILL),
    BURN(ToolKind.DRILL),
    BATTERY(ToolKind.DRILL, ToolKind.SABER),
    AOE(ToolKind.DRILL),
    SMELT(ToolKind.DRILL),
    TRASH_FILTER(ToolKind.DRILL),
    ECO(ToolKind.DRILL, ToolKind.SABER),
    LAVA_SOLIDIFIER(ToolKind.DRILL),
    SPEAK(ToolKind.DRILL),

    // Saber modules.
    DAMAGE(ToolKind.SABER),
    // Deprecated: merged into SPEED. Kept so existing items and installed modules still load.
    ATTACK_SPEED(ToolKind.SABER),
    SWEEP(ToolKind.SABER),
    // Deprecated: merged into FORTUNE. Kept so existing items and installed modules still load.
    LOOTING(ToolKind.SABER),
    XP_MULTIPLIER(ToolKind.SABER),
    DROP_COLLECTOR(ToolKind.SABER),
    VAMPIRISM(ToolKind.SABER),
    BEHEADING(ToolKind.SABER),
    IRRADIATION(ToolKind.SABER),
    METEORITE(ToolKind.SABER);

    private final java.util.Set<ToolKind> tools;

    ModuleType(ToolKind first, ToolKind... rest) {
        this.tools = java.util.Collections.unmodifiableSet(java.util.EnumSet.of(first, rest));
    }

    /** Whether a module of this type may go into this kind of tool. */
    public boolean fits(ToolKind tool) {
        return tools.contains(tool);
    }

    /** Whether it fits more than one kind of tool, which its tooltip says. */
    public boolean shared() {
        return tools.size() > 1;
    }

    /** Lang key of this type's effect line, shown on the module item and in the tool's tooltip. */
    public String tooltipKey() {
        return "tooltip.roll_mod.module." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
