package com.roll_54.roll_mod.config;

import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;

import java.util.LinkedHashMap;
import static com.roll_54.roll_mod.RollMod.MODID;

public class MyConfig extends Config {

    public MyConfig() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "roll_mod"), "", "");

    }

    @Comment("BUKVI for nothing")
    public boolean enabled = true;

    @Comment("BUKVI for nothing")
    public ValidatedDouble multiplier =
            new ValidatedDouble(1.0, 5.0, 0.1);

    public Options options = new Options();

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
