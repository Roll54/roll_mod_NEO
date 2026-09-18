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

public final class DatabaseManager {
  private static final DatabaseManager INSTANCE = new DatabaseManager();
  private HikariDataSource dataSource;
  private Jdbi jdbi;
  private ExecutorService executor;

  private DatabaseManager() {}

  public static DatabaseManager getInstance() {
    return INSTANCE;
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

    try {
      this.dataSource = new HikariDataSource(hc);
      this.executor = Executors.newFixedThreadPool(10);

      this.jdbi = Jdbi.create(dataSource);
      this.jdbi.installPlugin(new SqlObjectPlugin());
      this.jdbi.registerRowMapper(BalanceEntry.class, BalanceEntry.MAPPER);

      LOGGER.info("Database initialized successfully.");

    } catch (Exception e) {
      LOGGER.error("Failed to initialize database!", e);
      throw new RuntimeException("Critical database error", e);
    }
  }

  public void shutdown() {
    LOGGER.info("Shutting down database services...");

    if (executor != null) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
      }
    }

    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
    LOGGER.info("Database services stopped.");
  }

  public Jdbi getJdbi() {
    if (jdbi == null)
      throw new IllegalStateException("Database not initialized! Call init() first.");
    return jdbi;
  }

  public ExecutorService getExecutor() {
    if (executor == null) throw new IllegalStateException("Executor not initialized!");
    return executor;
  }

  HikariDataSource getDataSource() {
    return dataSource;
  }
}
