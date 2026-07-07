package com.roll_54.roll_mod.config;

import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedFloat;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import static com.roll_54.roll_mod.RollMod.MODID;

public class MyConfig extends Config {

    public static MyConfig INSTANCE;

    public MyConfig() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "roll_mod"), "", "");

    }

    public GitUpdater gitUpdater = new GitUpdater();

    public static class GitUpdater extends ConfigSection {
        @Comment("Nicknames allowed to run '/rollmod admin git_copy'. Case-insensitive. Console and command blocks can never run it.")
        public ArrayList<String> allowedPlayers = new ArrayList<String>(
                List.of("roll_54")
        );
        @Comment("GitHub repository owner.")
        public String repoOwner = "Roll54";
        @Comment("GitHub repository name.")
        public String repoName = "crafts-for-modern";
        @Comment("Branch to pull from.")
        public String branch = "main";
        @Comment("Folder inside the repository to copy into kubejs/server_scripts.")
        public String sourceFolder = "server_scripts";
        @Comment("Seconds to wait after the copy completes before running /reload.")
        public int reloadDelaySeconds = 300;
        @Comment("Seconds before the reload to show players the red warning title.")
        public int warnBeforeReloadSeconds = 60;
    }

    public Messages messages = new Messages();

    public static class Messages extends ConfigSection {
        @Comment("A list of translation keys to broadcast to all players every 30 minutes.")
        public ArrayList<String> broadcastMessages = new ArrayList<String>(
                List.of(
                        "message.roll_mod.broadcast.drink_stretch",
                        "message.roll_mod.broadcast.stand_breathe",
                        "message.roll_mod.broadcast.tea_break",
                        "message.roll_mod.broadcast.open_questbook",
                        "message.roll_mod.broadcast.stuck_questbook",
                        "message.roll_mod.broadcast.overwhelmed_questbook",
                        "message.roll_mod.broadcast.uranium_puree",
                        "message.roll_mod.broadcast.titanium_gum",
                        "message.roll_mod.broadcast.multiblock_check",
                        "message.roll_mod.broadcast.emc2",
                        "message.roll_mod.broadcast.first_join",
                        "message.roll_mod.broadcast.plutonium_candy",
                        "message.roll_mod.broadcast.golden_baton",
                        "message.roll_mod.broadcast.sulfur_jam",
                        "message.roll_mod.broadcast.sulfur_pie",
                        "message.roll_mod.broadcast.golden_potato_porridge",
                        "message.roll_mod.broadcast.creosote_plate"
                )
        );
        @Comment("Period of time that need to broadcast a message")
        public int TIME_TO_BROADCAST = 12000;
    }

    @Comment("Settings for the /rtp (random teleport) command")
    public RtpSettings rtp = new RtpSettings();

    public static class RtpSettings extends ConfigSection {
        @Comment("""
                Dimensions in which /rtp is allowed (namespaced ids, e.g. "minecraft:overworld").
                If a player runs /rtp from a dimension that is not in this list, they are first
                moved to the overworld and the random teleport is performed there instead.
                Leave the overworld out of the list to effectively disable /rtp everywhere.
                """)
        public ArrayList<String> allowedDimensions = new ArrayList<String>(
                List.of("minecraft:overworld")
        );
    }

    @Comment("BUKVI for nothing")
    public boolean enabled = true;

    @Comment("BUKVI for nothing")
    public ValidatedDouble multiplier =
            new ValidatedDouble(1.0, 5.0, 0.1);

    public Options options = new Options();


    //that how to make maps in config
    public static class Options extends ConfigSection {

        public ValidatedIdentifierMap<Double> itemWeights =
                new ValidatedIdentifierMap<Double>(
                        new LinkedHashMap<>(),
                        ValidatedIdentifier.ofTag(
                                ItemTags.SWORDS
                        ),
                        new ValidatedDouble(1.0, 1.0, 0.0)
                );
    }

    @Comment("Item that gives every next field")
    public String autogiveItem = "minecraft:stone";

    @Comment("Period of time that need to give a book, or whatever")
    public int TIME_TO_GIVE = 12000;

    public PvPSettings pvp = new PvPSettings();

    public static class PvPSettings extends ConfigSection {

        @Comment("""
            Enables ModernTech's custom armor formula.
            True = use custom diminishing-return formula up to 75%.
            False = vanilla armor behavior.
            """)
        public ValidatedBoolean enabled = new ValidatedBoolean(true);


        @Comment("""
            Armor value at which diminishing returns begin.
            Vanilla armor values: diamond ~20, netherite ~20, leather ~7.
            """)
        public ValidatedInt softCap = new ValidatedInt(10, 40, 0);


        @Comment("""
            Maximum possible damage reduction.
            0.75 = 75% reduction (default). Vanilla max is 80%.
            Cannot exceed 0.90 because of balance reasons.
            """)
        public ValidatedDouble maxReduction = new ValidatedDouble(0.75, 0.90, 0.10);


        @Comment("""
            Exponential curve steepness.
            Larger values = faster diminishing returns.
            Default = 0.05 (safe, balanced, similar to MMO behavior).
            """)
        public ValidatedDouble reductionCurve = new ValidatedDouble(0.05, 0.20, 0.005);
    }


    @Comment("Settings for Nether Storm behaviour")
    public StormSettings storm = new StormSettings();

    public static class StormSettings extends ConfigSection {

        @Comment("""
                Minimum delay before storm starts again (in Minecraft hours).
                1 hour = 72000 ticks.
                """)
        public double minStormDelayHours = 1;

        @Comment("""
                Maximum delay before storm starts again (in Minecraft hours).
                Storm delay will be randomly selected between min and max.
                """)
        public double maxStormDelayHours = 12;

        @Comment("""
                Minimum duration of the storm (in Minecraft hours).
                1 hour = 72000 ticks.
                """)
        public double minStormDurationHours = 1;

        @Comment("""
                Maximum duration of the storm (in Minecraft hours).
                Storm duration will be chosen randomly between min and max.
                """)
        public double maxStormDurationHours = 6;

        @Comment("straight forward explanation")
        public boolean BerriesExplodeDuringStorm = true;

        @Comment("HP multiplier")
        public ValidatedDouble MOB_HP_BUFF = new ValidatedDouble(2.5D, 200, 1);


    }


    @Comment("Anvil tweaks: over-enchanting past max level and removing the 40-level 'Too Expensive!' cap.")
    public AnvilSettings anvil = new AnvilSettings();

    public static class AnvilSettings extends ConfigSection {

        @Comment("""
                Allow the anvil to combine enchantments past their normal maximum level.
                e.g. Sharpness V + Sharpness V -> Sharpness VI, and so on.
                False = vanilla behavior (combined level is clamped to the enchantment max).
                """)
        public ValidatedBoolean overEnchant = new ValidatedBoolean(true);

        @Comment("""
                Remove the vanilla 'Too Expensive!' block at 40 levels so the anvil result is never
                greyed out for cost alone. Costs at or above the threshold are scaled exponentially
                instead (see expThreshold / expGrowth), so high-tier work stays possible but expensive.
                False = vanilla 40-level hard cap.
                """)
        public ValidatedBoolean removeLevelCap = new ValidatedBoolean(true);

        @Comment("""
                Level cost at which exponential scaling starts. Below this the cost is unchanged.
                Vanilla hard cap is 40.
                """)
        public ValidatedInt expThreshold = new ValidatedInt(40, 256, 1);

        @Comment("""
                Exponential growth factor applied per level above the threshold:
                    cost = threshold * expGrowth ^ (cost - threshold)
                1.0 = no extra scaling (cost stays flat past the threshold).
                1.1 = roughly +10% per level over the threshold (default).
                """)
        public ValidatedDouble expGrowth = new ValidatedDouble(1.1, 2.0, 1.0);
    }


    public SolarPanelOptions solarPanelOptions = new SolarPanelOptions();

    public static class SolarPanelOptions extends ConfigSection {

        public ValidatedIdentifierMap<Float> dimensionMultipliers =
                new ValidatedIdentifierMap<Float>(
                        new LinkedHashMap<>(),
                        ValidatedIdentifier.ofRegistryKey(
                                Registries.DIMENSION
                        ),
                        new ValidatedFloat(1.0F, 100.0F, 0.01F)
                );

        public ValidatedList<ResourceLocation> restrictedDimensions =
                new ValidatedList<ResourceLocation>(
                        new ArrayList<>(List.of(
                                ResourceLocation.fromNamespaceAndPath("minecraft", "the_nether")
                        )),
                        ValidatedIdentifier.ofRegistryKey(
                                Registries.DIMENSION
                        )
        );
    }



    @Override
    public FileType fileType() {
        return FileType.JSON5;
    }

    @Override
    public SaveType saveType() {
        return SaveType.SEPARATE;
    }

    @Override
    public int defaultPermLevel() {
        return 4;
    }
}
