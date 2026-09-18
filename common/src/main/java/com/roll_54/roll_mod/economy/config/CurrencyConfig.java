package com.roll_54.roll_mod.economy.config;

import static com.roll_54.roll_mod.RollMod.MODID;

import kotlin.jvm.functions.Function0;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CurrencyConfig extends Config {

  public static CurrencyConfig MAIN;

  public CurrencyConfig() {
    super(ResourceLocation.fromNamespaceAndPath(MODID, "currency"), "", "");
  }

  public static void init() {
    MAIN =
        ConfigApi.registerAndLoadConfig((Function0<? extends CurrencyConfig>) CurrencyConfig::new);
  }

  @Override
  public @NotNull FileType fileType() {
    return FileType.JSON;
  }

  public SQLSettings sql = new SQLSettings();
  public ClientSettings client = new ClientSettings();
  public PlaytimeSettings playtime = new PlaytimeSettings();
  public PlotSettings plot = new PlotSettings();

  public static class SQLSettings extends ConfigSection {
    public String jdbcUrl = "jdbc:mysql://localhost:3306/mc_currency";
    public String dbUser = "mc_user";
    public String dbPassword = "test_password_for_db";
  }

  public static class PlotSettings extends ConfigSection {

    /** Log plot purchases, claims, refunds and failures to the server log. */
    public boolean logActions = false;
  }

  public static class PlaytimeSettings extends ConfigSection {

    /** Master switch for the automatic playtime rewards. */
    public boolean enabled = false;

    /** Currency type rewards are paid in (one of: main, event, event2, battlepass). */
    public String currencyType = "main";

    /** Persistent reward: accumulated playtime that survives logout/restart. */
    public int persistentIntervalMinutes = 15;

    public long persistentReward = 15;

    /** Session reward: continuous online time, reset to 0 on every login. */
    public int sessionIntervalMinutes = 60;

    public long sessionReward = 120;
  }

  public static class ClientSettings extends ConfigSection {

    /** Starting color for the gradient (left side) */
    public ValidatedColor gradientStartColor = new ValidatedColor(167, 94, 139);

    /** Ending color for the gradient (right side) */
    public ValidatedColor gradientEndColor = new ValidatedColor(22, 0, 110);

    /** Show a chat message whenever an automatic playtime reward is received. */
    public boolean showPlaytimeRewards = true;
  }
}
