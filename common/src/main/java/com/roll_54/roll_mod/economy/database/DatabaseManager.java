package com.roll_54.roll_mod.economy.database;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.economy.config.CurrencyConfig;
import com.roll_54.roll_mod.economy.currency.model.BalanceEntry;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

/**
 * The currency database: one pool and one executor, opened on {@code ServerStartingEvent} and closed
 * on {@code ServerStoppingEvent}.
 *
 * <p>Both of those fire once per <em>world</em>, not once per process, so everything this class owns
 * has to survive being closed and opened again inside one client JVM. {@link #shutdown()} therefore
 * resets the fields to their un-initialised state: it used to only close them, and because {@link
 * #init} returns early on {@code dataSource != null}, the second world of a session kept the first
 * world's terminated executor and rejected every query — including the balance read on login, which
 * surfaced as "Couldn't place player in world".
 */
public final class DatabaseManager {
  private static final DatabaseManager INSTANCE = new DatabaseManager();
  private HikariDataSource dataSource;
  private Jdbi jdbi;
  private volatile ExecutorService executor;

  private DatabaseManager() {}

  public static DatabaseManager getInstance() {
    return INSTANCE;
  }

  /**
   * Whether currency queries can run. False before the first world, after every world, and for a
   * whole session whose {@link #init} failed — a client with no SQL driver, for instance.
   */
  public boolean isAvailable() {
    return this.executor != null;
  }

  public void init(CurrencyConfig.SQLSettings config) {
    if (dataSource != null) {
      LOGGER.warn("Database already initialized");
      return;
    }

    LOGGER.info("Initializing Database Connection Pool...");

    HikariConfig hc = new HikariConfig();
    hc.setJdbcUrl(config.jdbcUrl);
    hc.setUsername(config.dbUser);
    hc.setPassword(config.dbPassword);

    hc.setMaximumPoolSize(10);
    hc.setMinimumIdle(2);
    hc.setIdleTimeout(300_000);
    hc.setConnectionTimeout(5000);
    hc.setPoolName("RollCurrencyPool");

    if (config.jdbcUrl.contains("mysql")) {
      hc.addDataSourceProperty("cachePrepStmts", "true");
      hc.addDataSourceProperty("prepStmtCacheSize", "250");
      hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      hc.addDataSourceProperty("useServerPrepStmts", "true");
    }

    // Built into locals and published only once everything succeeded: a half-open manager whose
    // dataSource is set but whose jdbi is not would block init() from ever retrying.
    HikariDataSource opened = null;
    try {
      opened = new HikariDataSource(hc);

      Jdbi handle = Jdbi.create(opened);
      handle.installPlugin(new SqlObjectPlugin());
      handle.registerRowMapper(BalanceEntry.class, BalanceEntry.MAPPER);

      this.dataSource = opened;
      this.jdbi = handle;
      // Written last: it is what isAvailable() reads, so nothing sees a usable executor before the
      // pool it submits work to exists.
      this.executor = Executors.newFixedThreadPool(10);

      LOGGER.info("Database initialized successfully.");

    } catch (Exception e) {
      this.dataSource = null;
      this.jdbi = null;
      this.executor = null;
      if (opened != null && !opened.isClosed()) {
        opened.close();
      }
      LOGGER.error("Failed to initialize database!", e);
      throw new RuntimeException("Critical database error", e);
    }
  }

  /** Closes everything and resets to un-initialised, so the next world opens a fresh pool. */
  public void shutdown() {
    LOGGER.info("Shutting down database services...");

    ExecutorService running = this.executor;
    // Cleared first, so anything still ticking sees the database as gone rather than submitting to
    // a pool that is about to reject it.
    this.executor = null;

    if (running != null) {
      running.shutdown();
      try {
        if (!running.awaitTermination(5, TimeUnit.SECONDS)) {
          running.shutdownNow();
        }
      } catch (InterruptedException e) {
        running.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }

    this.dataSource = null;
    this.jdbi = null;
    LOGGER.info("Database services stopped.");
  }

  public Jdbi getJdbi() {
    Jdbi current = this.jdbi;
    if (current == null)
      throw new IllegalStateException("Database not initialized! Call init() first.");
    return current;
  }

  /**
   * Null when no database is open — callers run on the server thread and must check rather than be
   * thrown at. See {@code CurrencyRepository}, which turns this into a default-valued future.
   */
  public ExecutorService getExecutor() {
    return this.executor;
  }

  HikariDataSource getDataSource() {
    return dataSource;
  }
}
