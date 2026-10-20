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
                Dimensions in which /rtp is refused (namespaced ids, e.g. "minecraft:the_end").
                A player running /rtp in one of these is moved to the overworld first and the
                random teleport happens there instead. Blacklisting the overworld too refuses
                /rtp outright rather than sending anyone in circles.
                """)
        public ArrayList<String> blacklistedDimensions = new ArrayList<String>(
                List.of("minecraft:the_end")
        );

        @Comment("Closest a random teleport may land to the world spawn, in blocks.")
        public ValidatedInt minRadius = new ValidatedInt(500, 30_000_000, 0);

        @Comment("""
                Furthest a random teleport may land from the world spawn, in blocks.
                Capped by the world border: a border smaller than this shrinks both radii to fit,
                so there is no need to keep this number in step with /worldborder set.
                """)
        public ValidatedInt maxRadius = new ValidatedInt(25_000, 30_000_000, 1);

        @Comment("How many candidate positions are tried before /rtp gives up.")
        public ValidatedInt maxTries = new ValidatedInt(100, 1000, 1);

        @Comment("""
                Candidates examined per server tick, per searching player. These checks cost no
                chunk — at most one chunk is generated per player per tick regardless — so raising
                this only makes the search settle on a promising spot sooner.
                """)
        public ValidatedInt triesPerTick = new ValidatedInt(2, 20, 1);

        @Comment("""
                Wait between random teleports, in TICKS (20 = one second).
                Overridden per rank by the LuckPerms meta 'rollmod.rtp.cooldown', and waived
                entirely by the permission 'rollmod.rtp.bypasscooldown'.
                """)
        public ValidatedInt cooldownTicks = new ValidatedInt(12_000, 1_728_000, 0);
    }

    @Comment("Settings for the /fly command")
    public FlySettings fly = new FlySettings();

    public static class FlySettings extends ConfigSection {
        @Comment("""
                Dimensions where /fly is refused (namespaced ids, e.g. "minecraft:the_nether").
                A player who enters one while flying is put back on their feet and told why.
                Access to the command itself is the LuckPerms permission 'rollmod.fly.use'.
                """)
        public ArrayList<String> blacklistedDimensions = new ArrayList<String>();
    }

    @Comment("Settings for the /heal command")
    public HealSettings heal = new HealSettings();

    public static class HealSettings extends ConfigSection {
        @Comment("""
                Wait between heals, in TICKS (20 = one second).
                Overridden per rank by the LuckPerms meta 'rollmod.heal.cooldown', and waived
                entirely by the permission 'rollmod.heal.bypasscooldown'.
                """)
        public ValidatedInt cooldownTicks = new ValidatedInt(12_000, 1_728_000, 0);
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


    public ChunkGuardSettings chunkGuard = new ChunkGuardSettings();

    public static class ChunkGuardSettings extends ConfigSection {
        // Plain fields, not Validated*: :server reads these and can't see fzzy_config's types.
        @Comment("""
            Freeze a chunk (no entity, block entity, scheduled, random ticks or natural spawning) when something
            ticking inside it throws, instead of crashing the server. Stays frozen until '/rollmod admin chunkfreeze unfreeze'
            or a restart.
            """)
        public boolean freezeOnCrash = true;

        @Comment("Non-player entities in one chunk at which the chunk gets frozen. 0 = disabled.")
        @ValidatedInt.Restrict(min = 0, max = 100_000)
        public int entityLimit = 200;

        @Comment("An overload-frozen chunk resumes ticking once its non-player entity count drops below this.")
        @ValidatedInt.Restrict(min = 0, max = 100_000)
        public int resumeBelow = 150;

        @Comment("How often (in ticks) entity counts per chunk are rescanned.")
        @ValidatedInt.Restrict(min = 1, max = 1200)
        public int scanIntervalTicks = 20;
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


    @Comment("How the new modular drills, modular sabers, their modules and the Module Installation Table look.")
    public NewItems newItems = new NewItems();

    public static class NewItems extends ConfigSection {

        @Comment("""
                Show the new modular items as they really are: their textures, names and tooltips.
                False = every one of them shows the 'no texture yet' placeholder, the name
                'Unknown item' and no tooltip, for every player. Server-side and synced to clients;
                changes apply live, without a restart.
                """)
        public ValidatedBoolean showNewItems = new ValidatedBoolean(true);
    }

    @Comment("Anvil tweaks: over-max enchantment transfer and removing the 40-level 'Too Expensive!' cap.")
    public AnvilSettings anvil = new AnvilSettings();

    public static class AnvilSettings extends ConfigSection {

        @Comment("""
                Let an over-max enchantment on an anvil input carry into the result, e.g. a
                Sharpness VII book applied to a sword gives Sharpness VII. Combining never raises a
                level past max(enchantment max, highest input level): Sharpness V + Sharpness V is
                still Sharpness V, so books (and items) cannot be overchanted by combining.
                False = vanilla behavior (every combined level is clamped to the enchantment max).
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


    @Comment("Create Cybernetics tweaks.")
    public CreateCyberneticsSettings createCybernetics = new CreateCyberneticsSettings();

    public static class CreateCyberneticsSettings extends ConfigSection {

        @Comment("""
                Whether the Ripper counts as a hostile mob. It is a Monster by class, so vanilla treats
                it as one: it blocks sleeping nearby, despawns on Peaceful, is attacked by iron and
                snow golems and full conduits, wakes foxes and plays on the Hostile sound slider.
                False = none of that applies; it still fights back when hurt.
                True = vanilla behavior. Changes apply live, without a restart.
                """)
        public ValidatedBoolean ripperHostile = new ValidatedBoolean(false);
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

    public ChatChanges chat_changes = new ChatChanges();

    public static class ChatChanges extends ConfigSection {
        @Comment("Hide the advancement announcements in chat (challenges, see PlayerAdvancementsMixin).")
        public boolean hideAdvancementMessages = false;
    }

    @Comment("The moderation tab's death log: what a player carried when they died.")
    public DeathLogSettings deathlog = new DeathLogSettings();

    public static class DeathLogSettings extends ConfigSection {
        @Comment("Record player deaths at all. Off stops new records; old ones stay readable.")
        public ValidatedBoolean enabled = new ValidatedBoolean(true);
        @Comment("Deaths kept per player, newest first. Older ones are deleted as new ones arrive.")
        public ValidatedInt maxPerPlayer = new ValidatedInt(30, 500, 1);
    }

    @Comment("Player hub tabs that can be switched off.")
    public HubSettings hub = new HubSettings();

    public static class HubSettings extends ConfigSection {
        @Comment("Show the Skins tab in the player hub. Off while the tab is unfinished. Synced to clients; applies live.")
        public ValidatedBoolean skinsTab = new ValidatedBoolean(false);
    }

    @Comment("How long letters the server sends by itself stay in the player's list before they expire. Unaccepted attachments are lost with them.")
    public LetterSettings letters = new LetterSettings();

    public static class LetterSettings extends ConfigSection {
        @Comment("Hours a Minestar shop delivery letter waits to be accepted. 0 = never expires.")
        public ValidatedInt shopLifetimeHours = new ValidatedInt(24, 24 * 365, 0);
        @Comment("Hours a death-log letter (items of a death returned or retrieved by staff that did not fit the inventory) waits to be accepted. 0 = never expires.")
        public ValidatedInt deathLogLifetimeHours = new ValidatedInt(72, 24 * 365, 0);

        /** {@link #shopLifetimeHours} in millis, {@code 0} for never — what {@code LetterService} takes. */
        public long shopLifetime() {
            return hours(shopLifetimeHours.get());
        }

        public long deathLogLifetime() {
            return hours(deathLogLifetimeHours.get());
        }

        private static long hours(int hours) {
            return Math.max(0, hours) * 60L * 60L * 1000L;
        }
    }
}
