package com.roll_54.roll_mod.cosmetics.storage;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.cosmetics.config.CosmeticsConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.neoforged.fml.loading.FMLPaths;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;

/**
 * The cosmetics database: its own pool, its own executor, its own lifecycle.
 *
 * <p>Deliberately <em>not</em> the economy's {@code DatabaseManager}. Cosmetics are permanent
 * entitlements that must outlive any world and must not depend on an economy being configured at
 * all, so they get their own connection with its own default — an embedded H2 file in the game
 * directory, which needs no setup and works in singleplayer.
 *
 * <p>Two differences from {@code DatabaseManager} are deliberate corrections rather than style:
 *
 * <ul>
 *   <li>{@link #shutdown()} nulls its fields. The economy's does not, and because its {@code init}
 *       guard is {@code if (dataSource != null) return}, leaving one singleplayer world and loading
 *       another leaves every query pointed at a closed pool. A dedicated server never sees it
 *       because the process dies; a developer testing two worlds in one session sees it every time.
 *   <li>{@link #isAvailable()} exists so callers can check before they act. The economy instead lets
 *       {@code getExecutor()} throw {@link IllegalStateException} from the server thread, which is
 *       why its "currency is disabled for this session" message is only true until someone tries to
 *       use it.
 * </ul>
 */
public final class CosmeticsDatabase {

  private static final CosmeticsDatabase INSTANCE = new CosmeticsDatabase();

  private HikariDataSource dataSource;
  private Jdbi jdbi;
  private ExecutorService executor;

  /** Read from the server thread before every mutation, written from the lifecycle events. */
  private volatile boolean available;

  /** See {@link #init}: the embedded file's owner, fixed at creation and not safely changeable. */
  private static final String LOCAL_USER = "";

  private static final String LOCAL_PASSWORD = "";

  private CosmeticsDatabase() {}

  public static CosmeticsDatabase getInstance() {
    return INSTANCE;
  }

  public boolean isAvailable() {
    return this.available;
  }

  /**
   * Opens the configured database, falling back to the local file when allowed. Never throws: a
   * cosmetics outage must not stop a world from loading.
   */
  public void init(CosmeticsConfig.StorageSettings config) {
    if (this.dataSource != null) {
      return;
    }
    String configured = config.jdbcUrl == null ? "" : config.jdbcUrl.trim();

    if (!configured.isEmpty()) {
      if (tryOpen(configured, config.dbUser, config.dbPassword)) {
        this.available = true;
        return;
      }
      if (!config.fallbackToLocalFile) {
        LOGGER.error("[Cosmetics] {} is unreachable and fallback is off; cosmetics are disabled.",
                configured);
        this.available = false;
        return;
      }
      LOGGER.error("[Cosmetics] {} is unreachable — falling back to the local file. Grants made "
              + "now will NOT reach the shared database.", configured);
    }

    // H2 fixes the database owner from the credentials of the very first connection, so these must
    // never change: an existing cosmetics.mv.db would become unopenable ("Wrong user name or
    // password"). Empty is what JdbcDataSource defaults to, and what every existing file was made
    // with.
    this.available = tryOpen(localFileUrl(), LOCAL_USER, LOCAL_PASSWORD);
  }

  /**
   * {@code <gameDir>/roll_mod/cosmetics.mv.db} — outside the world save, so resetting the world
   * keeps everyone's skins.
   *
   * <p>{@code DATABASE_TO_LOWER} and {@code CASE_INSENSITIVE_IDENTIFIERS} are what actually make
   * {@code MODE=MySQL} behave like MySQL, and both are <b>creation-time only</b>: if the file is
   * first created without them they cannot be added later without deleting it. {@code
   * DB_CLOSE_ON_EXIT=FALSE} keeps H2's own JVM shutdown hook from racing Hikari's close.
   */
  private static String localFileUrl() {
    Path dir = FMLPaths.GAMEDIR.get().resolve(RollMod.MODID);
    try {
      Files.createDirectories(dir);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot create the cosmetics directory " + dir, e);
    }
    return "jdbc:h2:file:" + dir.resolve("cosmetics").toAbsolutePath()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"
            + ";DB_CLOSE_ON_EXIT=FALSE";
  }

  private boolean tryOpen(String url, String user, String password) {
    boolean embedded = url.startsWith("jdbc:h2:");
    HikariConfig hc = new HikariConfig();

    if (embedded) {
      // Hand Hikari a driver instance rather than a bare URL. DriverManager finds drivers through
      // META-INF/services, and that is exactly the mechanism modlauncher makes unreliable for a
      // jar-in-jar'd library — naming the class directly cannot fail that way.
      JdbcDataSource h2 = new JdbcDataSource();
      h2.setURL(url);
      // Set explicitly rather than left to the default, so that what the caller passes is what is
      // actually used — silently ignoring these is how a later switch to setJdbcUrl would start
      // failing against files this code created.
      h2.setUser(user);
      h2.setPassword(password);
      hc.setDataSource(h2);
    } else {
      hc.setJdbcUrl(url);
      hc.setUsername(user);
      hc.setPassword(password);
    }

    // An embedded file gains nothing from ten writers, and writes are serialised per player anyway.
    hc.setMaximumPoolSize(embedded ? 4 : 10);
    hc.setMinimumIdle(1);
    hc.setConnectionTimeout(5000);
    hc.setPoolName("RollCosmeticsPool");

    HikariDataSource opened = null;
    try {
      opened = new HikariDataSource(hc);
      // Probe here, so an unreachable database is a startup log line rather than a surprise on the
      // first player's login.
      try (Connection ignored = opened.getConnection()) {
        // opened successfully
      }
      this.dataSource = opened;
      this.jdbi = Jdbi.create(opened);
      // Daemon threads: a stuck write must never hold the JVM open.
      this.executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "roll_mod-cosmetics-db");
        thread.setDaemon(true);
        return thread;
      });
      LOGGER.info("[Cosmetics] Skin storage open at {}", embedded ? "the local file" : url);
      return true;
    } catch (Exception e) {
      LOGGER.error("[Cosmetics] Cannot open {}", url, e);
      if (opened != null && !opened.isClosed()) {
        opened.close();
      }
      return false;
    }
  }

  public Jdbi getJdbi() {
    Jdbi current = this.jdbi;
    if (current == null) {
      throw new IllegalStateException("Cosmetics database is not open");
    }
    return current;
  }

  /** Null when the database never opened — callers must check {@link #isAvailable()} first. */
  public ExecutorService getExecutor() {
    return this.executor;
  }

  HikariDataSource getDataSource() {
    return this.dataSource;
  }

  /**
   * Closes everything and resets to the un-initialised state, so a second world in the same client
   * session opens a fresh pool instead of reusing a closed one.
   */
  public void shutdown() {
    this.available = false;

    if (this.executor != null) {
      this.executor.shutdown();
      try {
        if (!this.executor.awaitTermination(5, TimeUnit.SECONDS)) {
          this.executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        this.executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
    if (this.dataSource != null && !this.dataSource.isClosed()) {
      this.dataSource.close();
    }

    this.executor = null;
    this.dataSource = null;
    this.jdbi = null;
  }
}
