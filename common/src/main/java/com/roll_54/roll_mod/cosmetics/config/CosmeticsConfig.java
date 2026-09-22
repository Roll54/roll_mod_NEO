package com.roll_54.roll_mod.cosmetics.config;

import static com.roll_54.roll_mod.RollMod.MODID;

import kotlin.jvm.functions.Function0;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Where cosmetic entitlements are stored. Separate from {@code CurrencyConfig} because the two are
 * separate databases with separate lifecycles: a server may perfectly well run cosmetics with no
 * economy at all, and losing the economy DB must not take skins with it.
 *
 * <p>Lands in {@code <gameDir>/config/cosmetics.json}, matching how {@code CurrencyConfig} resolves
 * ({@code super(id, "", "")} means Fzzy Config writes flat into the config directory).
 */
public class CosmeticsConfig extends Config {

  public static CosmeticsConfig MAIN;

  public CosmeticsConfig() {
    super(ResourceLocation.fromNamespaceAndPath(MODID, "cosmetics"), "", "");
  }

  public static void init() {
    MAIN =
        ConfigApi.registerAndLoadConfig((Function0<? extends CosmeticsConfig>) CosmeticsConfig::new);
  }

  @Override
  public @NotNull FileType fileType() {
    return FileType.JSON;
  }

  public StorageSettings storage = new StorageSettings();

  public static class StorageSettings extends ConfigSection {

    /**
     * Empty — the default — means the local embedded file at {@code <gameDir>/minestar/cosmetics},
     * which needs no setup and works in singleplayer. Point this at a shared {@code jdbc:mysql://…}
     * to have several servers read one set of cosmetics.
     *
     * <p>Either way the data lives outside the world save, which is the whole point: cosmetics are
     * permanent entitlements and must survive a world reset.
     */
    public String jdbcUrl = "";

    public String dbUser = "";
    public String dbPassword = "";

    /**
     * When a configured remote {@link #jdbcUrl} cannot be reached, open the local file instead of
     * disabling cosmetics.
     *
     * <p><b>Turn this off whenever {@link #jdbcUrl} is set.</b> On a shared database a brief outage
     * would otherwise open an empty local file, show every player as having lost every skin, and
     * strand any grant made during the outage in a file nothing will ever read again. A visible
     * outage is much better than silent divergence. It defaults on because the default configuration
     * has no remote URL at all, where the flag can never fire.
     */
    public boolean fallbackToLocalFile = true;
  }
}
