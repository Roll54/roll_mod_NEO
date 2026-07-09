package com.roll_54.roll_mod.mixin.adstra;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import earth.terrarium.adastra.common.utils.radio.StationLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Mixin(StationLoader.class)
public abstract class StationLoaderMixin {

    // Ad Astra fetches its radio station list from https://adastra.terrarium.earth/stations.
    // We replace that web call with our own station list bundled inside the mod jar, so no
    // network request happens and StationLoader#init parses our {"stations":[...]} instead.
    @Unique
    private static final String STATIONS_RESOURCE = "/assets/roll_mod/sounds/endpoints_radio_moderntech.json";

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/teamresourceful/resourcefullib/common/utils/WebUtils;getJson(Ljava/lang/String;)Lcom/google/gson/JsonObject;"
            )
    )
    private static JsonObject roll_mod$loadBundledStations(String url) {
        try (InputStream in = StationLoaderMixin.class.getResourceAsStream(STATIONS_RESOURCE)) {
            if (in == null) return null;
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        }
    }
}