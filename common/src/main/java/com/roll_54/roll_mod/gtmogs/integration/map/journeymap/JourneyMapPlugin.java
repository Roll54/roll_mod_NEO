package com.roll_54.roll_mod.gtmogs.integration.map.journeymap;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.RegistryEvent;
import journeymap.api.v2.common.event.ClientEventRegistry;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@journeymap.api.v2.client.JourneyMapPlugin(apiVersion = IClientAPI.API_VERSION)
public class JourneyMapPlugin implements IClientPlugin {

    @Getter
    private static boolean active = false;

    @Getter
    private static IClientAPI jmApi;

    @Getter
    private static JourneyMapOptions options;

    @Override
    public void initialize(@NotNull IClientAPI jmClientApi) {
        active = true;
        jmApi = jmClientApi;
        JourneyMapEventListener.init();
        ClientEventRegistry.OPTIONS_REGISTRY_EVENT.subscribe(GTMOGS.MOD_ID, JourneyMapPlugin::onOptionsRegistry);
    }

    @Override
    public String getModId() {
        return GTMOGS.MOD_ID;
    }

    protected static void onOptionsRegistry(RegistryEvent.OptionsRegistryEvent event) {
        options = new JourneyMapOptions();
    }
}
