package com.roll_54.roll_mod.gtmogs.config;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;
import org.jetbrains.annotations.ApiStatus;

@Config(id = GTMOGS.CONFIG_ID)
public class ConfigHolder {

    public static ConfigHolder INSTANCE;
    private static final Object LOCK = new Object();

    @ApiStatus.Internal
    public static dev.toma.configuration.config.ConfigHolder<ConfigHolder> INTERNAL_INSTANCE;

    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null || INTERNAL_INSTANCE == null) {
                INTERNAL_INSTANCE = Configuration.registerConfig(ConfigHolder.class, ConfigFormats.YAML);
                INSTANCE = INTERNAL_INSTANCE.getConfigInstance();
            }
        }
    }

    @Configurable
    public WorldGenConfigs worldgen = new WorldGenConfigs();

    @Configurable
    @Configurable.Comment("Config options for Mod Compatibility")
    public CompatibilityConfigs compat = new CompatibilityConfigs();
    @Configurable
    public DeveloperConfigs dev = new DeveloperConfigs();

    public static class CompatibilityConfigs {

        @Configurable
        @Configurable.Comment("Config options regarding GTMOGS compatibility with minimap mods")
        public MinimapCompatConfig minimap = new MinimapCompatConfig();

        public static class MinimapCompatConfig {

            @Configurable
            @Configurable.Comment({
                    "Toggle specific map mod integration on/off (need to restart for this to take effect)" })
            public Toggle toggle = new Toggle();

            @Configurable
            @Configurable.Comment({ "The radius, in blocks, that clicking an ore block will search for veins in.",
                    "-1 to disable", "Default: 24" })
            @Configurable.Range(min = 1)
            public int oreBlockProspectRange = 24;

            @Configurable
            @Configurable.Comment("The map scale at which displayed ores will stop scaling.")
            @Configurable.DecimalRange(min = 0.1, max = 16)
            // todo: implement or purge
            public float oreScaleStop = 1;

            @Configurable
            @Configurable.Comment("The size, in pixels, of ore icons on the map")
            @Configurable.Range(min = 4)
            public int oreIconSize = 32;

            @Configurable
            @Configurable.Comment("The string prepending ore names in the ore vein tooltip")
            public String oreNamePrefix = "> ";

            @Configurable
            @Configurable.Comment({ "The color to draw a box around the ore icon with.",
                    "Accepts either an ARGB hex color prefixed with # or the string 'material' to use the ore's material color" })
            public String borderColor = "#00000000";

            @Configurable
            @Configurable.Comment({ "Which part of the screen to anchor buttons to", "Default: \"BOTTOM_LEFT\"" })
            public Anchor buttonAnchor = Anchor.BOTTOM_LEFT;

            @Configurable
            @Configurable.Comment({ "Which direction the buttons will go", "Default: \"VERTICAL\"" })
            public Direction direction = Direction.VERTICAL;

            @Configurable
            @Configurable.Comment({ "How horizontally far away from the anchor to place the buttons", "Default: 20" })
            public int xOffset = 20;

            @Configurable
            @Configurable.Comment({ "How vertically far away from the anchor to place the buttons", "Default: 0" })
            public int yOffset = 0;

            public static class Toggle {

                @Configurable
                @Configurable.Comment({ "FTB Chunks integration enabled" })
                public boolean ftbChunksIntegration = true;

                @Configurable
                @Configurable.Comment({ "Journey Map integration enabled" })
                public boolean journeyMapIntegration = true;

                @Configurable
                @Configurable.Comment({ "Xaero's map integration enabled" })
                public boolean xaerosMapIntegration = true;
            }

            public enum Anchor {

                TOP_LEFT,
                TOP_CENTER,
                TOP_RIGHT,
                RIGHT_CENTER,
                BOTTOM_RIGHT,
                BOTTOM_CENTER,
                BOTTOM_LEFT,
                LEFT_CENTER;

                public boolean isCentered() {
                    return this == TOP_CENTER || this == RIGHT_CENTER || this == BOTTOM_CENTER || this == LEFT_CENTER;
                }

                public Direction usualDirection() {
                    return switch (this) {
                        case TOP_CENTER, BOTTOM_CENTER -> Direction.HORIZONTAL;
                        case RIGHT_CENTER, LEFT_CENTER -> Direction.VERTICAL;
                        default -> null;
                    };
                }
            }

            public enum Direction {
                VERTICAL,
                HORIZONTAL
            }

            public int getBorderColor(int materialColor) {
                if (borderColor.equals("material")) {
                    return materialColor;
                }
                // please java may I have an unsigned int
                try {
                    long tmp = Long.decode(borderColor);
                    if (tmp > 0x7FFFFFFF) {
                        tmp -= 0x100000000L;
                    }
                    return (int) tmp;
                } catch (NumberFormatException e) {
                    return 0x00000000;
                }
            }
        }
    }

    public static class WorldGenConfigs {

        @Configurable
        public OreVeinConfigs oreVeins = new OreVeinConfigs();

        @Configurable
        @Configurable.Comment("Timing and stall diagnostics for ore generation")
        public DiagnosticsConfigs diagnostics = new DiagnosticsConfigs();

        public static class DiagnosticsConfigs {

            @Configurable
            @Configurable.Comment({
                    "Track how long ore generation takes, warn about slow chunks, and periodically log a summary?",
                    "Costs two timestamps per chunk phase, so it is safe to leave on.",
                    "Default: true"
            })
            public boolean enabled = true;
            @Configurable
            @Configurable.Range(min = 1, max = 60_000)
            @Configurable.Comment({
                    "Warn when a single chunk spends longer than this (in milliseconds) in one ore gen phase.",
                    "Default: 100"
            })
            public int slowChunkMillis = 100;
            @Configurable
            @Configurable.Range(min = 0, max = 86_400)
            @Configurable.Comment({
                    "How often (in seconds) to log the ore generation timing summary. 0 disables the summary.",
                    "Default: 300"
            })
            public int summaryIntervalSeconds = 300;
            @Configurable
            @Configurable.Comment({
                    "Include a JVM deadlock check (and the offending stack traces) in the summary?",
                    "Default: true"
            })
            public boolean detectDeadlocks = true;
            @Configurable
            @Configurable.Range(min = 0, max = 3_600)
            @Configurable.Comment({
                    "Dump thread stacks when the server thread stops ticking for this many seconds. 0 disables the watchdog.",
                    "Runs on its own thread, so it still reports when the server is frozen and commands/profilers cannot run.",
                    "Default: 10"
            })
            public int stallThresholdSeconds = 10;
        }

        public static class OreVeinConfigs {

            @Configurable
            @Configurable.Range(min = 1, max = 32)
            @Configurable.Comment({
                    "The grid size (in chunks) for ore vein generation",
                    "Default: 3"
            })
            public int oreVeinGridSize = 3;
            @Configurable
            @Configurable.Range(min = 0, max = 32 * 16)
            @Configurable.Comment({
                    "The maximum random offset (in blocks) from the grid for generating an ore vein.",
                    "Default: 12"
            })
            public int oreVeinRandomOffset = 12;
            @Configurable
            @Configurable.Comment({ "Prevents regular vanilla ores from being generated outside GregTech ore veins",
                    "Default: true" })
            public boolean removeVanillaOreGen = true;
            @Configurable
            @Configurable.Comment({ "Prevents vanilla's large ore veins from being generated", "Default: true" })
            public boolean removeVanillaLargeOreVeins = true;
            @Configurable
            @Configurable.Comment({
                    "Sets the maximum number of chunks that may be cached for ore vein generation.",
                    "Higher values may improve world generation performance, but at the cost of more RAM usage.",
                    "If you substantially increase the ore vein grid size, random vein offset, or have very large (custom) veins, you may need to increase this value as well.",
                    "Default: 512 (requires restarting the server / re-opening the world)"
            })
            public int oreGenerationChunkCacheSize = 512;
            @Configurable
            @Configurable.Comment({
                    "Generate surface indicators above ore veins?",
                    "When disabled, indicators are not generated, cached or placed at all; veins themselves are unaffected.",
                    "Only applies to newly generated chunks - indicators already placed in existing chunks are left alone.",
                    "Default: false"
            })
            public boolean generateOreIndicators = false;
            @Configurable
            @Configurable.Comment({
                    "Sets the maximum number of chunks for which ore indicators may be cached.",
                    "If you register any custom veins with very large indicator ranges (or modify existing ones that way), you may need to increase this value.",
                    "Has no effect when 'generateOreIndicators' is disabled.",
                    "Default: 2048 (requires restarting the server / re-opening the world)"
            })
            public int oreIndicatorChunkCacheSize = 2048;
        }
    }

    public static class DeveloperConfigs {

        @Configurable
        @Configurable.Comment({ "Debug general events? (will print recipe conficts etc. to server's debug.log)",
                "Default: false" })
        public boolean debug = false;
        @Configurable
        @Configurable.Comment({ "Debug ore vein placement? (will print placed veins to server's debug.log)",
                "Default: false (no placement printout in debug.log)" })
        public boolean debugWorldgen = false;
        @Configurable
        @Configurable.Comment({ "Generate ores in superflat worlds?", "Default: false" })
        public boolean doSuperflatOres = false;
        @Configurable
        @Configurable.Comment({ "Dump all registered GT models/blockstates/etc?", "Default: false" })
        public boolean dumpAssets = false;
    }
}
