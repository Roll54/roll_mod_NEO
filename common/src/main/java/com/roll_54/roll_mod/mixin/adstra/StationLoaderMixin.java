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
    //
    // TODO: the resource below does not exist yet, so this redirect currently returns null and
    //  StationLoader#init bails on its own null check before ever touching STATIONS. The list stays
    //  empty, and because ServerboundSetStationPacket gates on StationLoader#hasStation(url) —
    //  an exact string match against that list — no station can be tuned at all. Either ship the
    //  file or drop the mixin; a registered redirect with nothing behind it is worse than no mixin.
    //
    // TODO: decide whether this mixin is wanted at all. This Ad Astra build already reads a local
    //  list without us: StationLoader#init calls readLocalStations() first, which takes the system
    //  property `adastra.stations` as a filesystem path and parses it, and only falls back to the
    //  web call we redirect here when that property is unset or the read throws. Setting
    //  -Dadastra.stations=<path> in the JVM args gets the same result with no bytecode surgery and
    //  lets an operator swap the list without a rebuild. This redirect is the fallback of a
    //  fallback.
    //
    // TODO: if we keep it and want the list served rather than bundled, return
    //  WebUtils.getJson("http://<host>/stations") from here instead of reading the jar. It is a
    //  plain java.net.http.HttpClient GET, so http/https work and file:// does not — HttpClient
    //  rejects the scheme and WebUtils swallows it into null. Note getJson passes checkStatus=false,
    //  so a 404 body is parsed as if it were the list and the failure is silent.
    //
    // TODO: whatever serves the streams the list points at must not send Content-Length.
    //  RadioHandler#streamRadio throws IOException outright when the response carries one, so a
    //  static file from nginx or `python -m http.server` will not play — it needs a chunked,
    //  endless stream (Icecast/Shoutcast shape). Audio is decoded by Mp3AudioStream via jlayer, so
    //  MP3 only. This runs client-side, so every player's machine has to reach the URL: localhost
    //  is single-player only, a server needs a LAN or public host.
    //
    // TODO: nothing here can push data anywhere. WebUtils exposes only get(), with no POST helper,
    //  and the radio path never uploads. If we ever want the game to report what someone tuned to,
    //  that is our own HttpClient call, and the place that knows is the ServerboundSetStationPacket
    //  handler, not this class.
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