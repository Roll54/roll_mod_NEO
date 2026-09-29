package com.roll_54.roll_mod.items.modulardrill;

/**
 * An item that takes modules at the Module Installation Table: the modular drill and the modular
 * saber. Both keep their modules in the same {@code DRILL_MODULES} component, so the table, its
 * slot view and {@link DrillModuleHelper} work on either through this interface alone.
 */
public interface ModularTool {

    /** How many modules fit. */
    int moduleSlots();

    /** Which modules are allowed in — see {@link ModuleType#fits}. */
    ToolKind toolKind();

    /** The most complexity the installed modules may add up to — see {@link DrillModuleHelper#canInstall}. */
    int maxComplexity();
}
