package com.roll_54.roll_mod.economy.database;

import com.roll_54.roll_mod.RollMod;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.resource.LoadableResource;

public class SchemaMigrator {

  private SchemaMigrator() {}

  private static final String MIGRATION_DIR = "db/migration";

  /**
   * Explicit list of migration scripts, in order.
   *
   * <p>Flyway's classpath scanner cannot enumerate directories inside NeoForge's modlauncher
   * classloader (it works in dev via the filesystem, but finds nothing in the packaged jar), so we
   * feed the scripts to Flyway by name instead. ADD EVERY NEW MIGRATION FILE HERE.
   */
  private static final List<String> MIGRATIONS =
      List.of("V1__Initial_setup.sql", "V2__Currency_offers.sql");

  public static CompletableFuture<Void> migrate() {
    ExecutorService executor = DatabaseManager.getInstance().getExecutor();
    if (executor == null) {
      // init() failed or never ran; the caller logs it and carries on without an economy.
      return CompletableFuture.failedFuture(
          new IllegalStateException("Cannot run migration: the database is not open"));
    }
    return CompletableFuture.runAsync(
        () -> {
          RollMod.LOGGER.info("[Migration] Starting database migration...");

          HikariDataSource hikariDataSource = DatabaseManager.getInstance().getDataSource();
          if (hikariDataSource == null) {
            throw new IllegalStateException("Cannot run migration: DataSource is null");
          }

          // Fix for NeoForge ClassLoading issues
          ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
          Thread.currentThread().setContextClassLoader(SchemaMigrator.class.getClassLoader());

          try {
            Flyway flyway =
                Flyway.configure()
                    .dataSource(hikariDataSource)
                    .locations("classpath:" + MIGRATION_DIR)
                    .resourceProvider(new ClasspathResourceProvider())
                    .baselineOnMigrate(true)
                    // Baseline at 0 so every migration (all using CREATE TABLE IF NOT EXISTS)
                    // re-applies idempotently even on a pre-existing schema with no history.
                    .baselineVersion("0")
                    .validateOnMigrate(true)
                    .load();

            flyway.migrate();
            RollMod.LOGGER.info("[Migration] Database is up to date.");

          } catch (FlywayException e) {
            RollMod.LOGGER.error("[Migration] Critical failure!", e);
            throw new RuntimeException("Migration failed", e);
          } finally {
            Thread.currentThread().setContextClassLoader(originalLoader);
          }
        },
        executor);
  }

  /**
   * Serves the migration scripts listed in {@link #MIGRATIONS} by loading them through the mod's
   * classloader, bypassing Flyway's (broken-in-modlauncher) directory scanning.
   */
  private static final class ClasspathResourceProvider implements ResourceProvider {

    @Override
    public LoadableResource getResource(String name) {
      for (String file : MIGRATIONS) {
        if (file.equals(name) || (MIGRATION_DIR + "/" + file).equals(name)) {
          return new ClasspathSqlResource(file);
        }
      }
      return null;
    }

    @Override
    public Collection<LoadableResource> getResources(String prefix, String[] suffixes) {
      List<LoadableResource> resources = new ArrayList<>();
      for (String file : MIGRATIONS) {
        if (!file.startsWith(prefix)) continue;
        for (String suffix : suffixes) {
          if (file.endsWith(suffix)) {
            resources.add(new ClasspathSqlResource(file));
            break;
          }
        }
      }
      return resources;
    }
  }

  private static final class ClasspathSqlResource extends LoadableResource {

    private final String filename;
    private final String path;

    ClasspathSqlResource(String filename) {
      this.filename = filename;
      this.path = MIGRATION_DIR + "/" + filename;
    }

    @Override
    public Reader read() {
      InputStream in = SchemaMigrator.class.getClassLoader().getResourceAsStream(path);
      if (in == null) {
        throw new FlywayException("Migration script not found on classpath: " + path);
      }
      return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    @Override
    public String getAbsolutePath() {
      return path;
    }

    @Override
    public String getAbsolutePathOnDisk() {
      return path;
    }

    @Override
    public String getFilename() {
      return filename;
    }

    @Override
    public String getRelativePath() {
      return path;
    }
  }
}
