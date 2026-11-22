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

    public static class Hud extends ConfigSection {
        @Comment("Show health, hunger, armor and air as flat bars with their numbers (17/20). Off: vanilla's hearts, drumsticks and armor icons.")
        public boolean statusBars = true;
    }

    public static class TitleScreen extends ConfigSection {
        @Comment("Show a small Singleplayer button (+) under the accessibility button. It opens the vanilla world list, where worlds can be created and played. Takes effect the next time the title screen opens.")
        public boolean singleplayerButton = false;
    }
}
