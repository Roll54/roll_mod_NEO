package com.roll_54.roll_mod.config;

import kotlin.jvm.functions.Function0;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import net.minecraft.resources.ResourceLocation;

import static com.roll_54.roll_mod.RollMod.MODID;

/**
 * Settings that only concern this player's own client, so they are never synced from a server.
 * Shown in the mod list's Config screen next to the synced {@link MyConfig}. Registered on the
 * client only ({@code RollMod}).
 */
public class RollClientConfig extends Config {

    public static RollClientConfig INSTANCE;

    public RollClientConfig() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "roll_mod_client"), "", "");
    }

    public static void init() {
        INSTANCE = ConfigApi.registerAndLoadConfig(
                (Function0<? extends RollClientConfig>) RollClientConfig::new, RegisterType.CLIENT);
    }

    @Comment("The pack's title screen.")
    public TitleScreen titleScreen = new TitleScreen();

    @Comment("The status rows above the hotbar.")
    public Hud hud = new Hud();

    @Comment("What the prospector's pick has found, on Xaero's World Map and Minimap (when they are installed).")
    public ProspectingMap prospectingMap = new ProspectingMap();

    /** How prospected ores show on the map. */
    public enum ProspectMapMode {
        /** The raw ore's icon in each chunk where the pick found it. */
        ICONS,
        /** Every prospected ore block on the World Map, in the ore scanner's colours, at a chosen height. */
        ORE_LAYER
    }

    public static class ProspectingMap extends ConfigSection {
        @Comment("ICONS: the raw ore's icon in each chunk where your pick found that ore. ORE_LAYER: the whole veins your pick recorded, block by block in the ore scanner's colours, on the World Map at the height you pick there.")
        public ProspectMapMode mapMode = ProspectMapMode.ICONS;
        @Comment("Show the button on the World Map screen that shows or hides the ores (the keybind works either way).")
        public boolean minimapButton = true;
        @Comment("After mining this many blocks of an ore in a chunk, hovering its icon on the World Map tells the exact number of blocks in the vein there.")
        public int exactCountAfter = 3;
        @Comment("Show prospected ores on the World Map.")
        public boolean showOnWorldMap = true;
        @Comment("Show prospected ore icons on the Minimap.")
        public boolean showOnMinimap = true;
        @Comment("ORE_LAYER: how many blocks above and below the chosen height are shown.")
        public int oreLayerSpread = 4;
        @Comment("Size of the ore icons on the World Map, 1.0 = normal (Xaero's own icon scale applies on top).")
        public double worldMapIconScale = 1.0;
        @Comment("Size of the ore icons on the Minimap, 1.0 = normal (Xaero's own icon scale applies on top).")
        public double minimapIconScale = 1.0;
    }

    public static class Hud extends ConfigSection {
        @Comment("Show health, hunger, armor and air as flat bars with their numbers (17/20). Off: vanilla's hearts, drumsticks and armor icons.")
        public boolean statusBars = true;
    }

    public static class TitleScreen extends ConfigSection {
        @Comment("Show a small Singleplayer button (+) under the accessibility button. It opens the vanilla world list, where worlds can be created and played. Takes effect the next time the title screen opens.")
        public boolean singleplayerButton = false;
    }
}
