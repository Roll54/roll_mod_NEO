package com.roll_54.roll_mod.init;


import com.roll_54.roll_mod.config.MyConfig;
import kotlin.jvm.functions.Function0;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;

public class ModConfigs {

    public static MyConfig MAIN;

    public static void init() {
        MAIN = ConfigApi.registerAndLoadConfig((Function0<? extends MyConfig>) MyConfig::new);
    }
}