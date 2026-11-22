package com.roll_54.roll_mod.config;

import static com.roll_54.roll_mod.RollMod.MODID;

import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import net.minecraft.resources.ResourceLocation;

/**
 * This player's own client settings, {@code roll_mod:roll_mod_client}: never synced, registered on the client only
 * (RollMod calls {@link #init} behind a dist check). Editable from the mod list's Config screen; the hub's settings tab
 * writes {@link Hud#statusBars} and saves.
 */
public class RollClientConfig extends Config {

    /** Null on a dedicated server, and on the client until {@link #init} has run. */
    public static RollClientConfig INSTANCE;

    public RollClientConfig() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "roll_mod_client"));
    }

    public static void init() {
        INSTANCE = ConfigApi.registerAndLoadConfig(RollClientConfig::new, RegisterType.CLIENT);
    }

    @Comment("The mod's main menu.")
    public TitleScreen titleScreen = new TitleScreen();

    public static class TitleScreen extends ConfigSection {
        @Comment("Show a small Singleplayer button under the accessibility one, opening the vanilla world list.")
        public boolean singleplayerButton = false;
    }

    @Comment("The in-game HUD.")
    public Hud hud = new Hud();

    public static class Hud extends ConfigSection {
        @Comment("Health, hunger, armor and air above the hotbar as flat bars with numbers. Off: vanilla's icons.")
        public boolean statusBars = true;
    }
}
