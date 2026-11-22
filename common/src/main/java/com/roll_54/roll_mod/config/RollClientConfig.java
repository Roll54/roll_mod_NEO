package com.roll_54.roll_mod.config;

import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import net.minecraft.resources.ResourceLocation;

/**
 */
public class RollClientConfig extends Config {

    public static RollClientConfig INSTANCE;

    public RollClientConfig() {
    }

    public static void init() {
    }

    public TitleScreen titleScreen = new TitleScreen();

    public Hud hud = new Hud();

    public static class Hud extends ConfigSection {
        public boolean statusBars = true;
    }
}
