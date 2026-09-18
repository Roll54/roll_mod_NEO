package com.roll_54.roll_mod.cosmetics.storage;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.cosmetics.ItemSkinService;
import com.roll_54.roll_mod.cosmetics.PlayerItemSkins;
import com.roll_54.roll_mod.cosmetics.SkinCategory;
import com.roll_54.roll_mod.data.RMMAttachment;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The authoritative in-memory copy of everyone's cosmetics, written through to the database.
 *
 * <p>The database is the permanent record, but nothing may block on it: {@code ItemSkinService} is
 * called from commands and packet handlers that need an answer immediately, and its readers run on
 * the render path. So a mutation updates this cache synchronously on the server thread, returns, and
 * queues the SQL.
 *
 * <p><b>Every database touch for one player — the load included — goes through that player's entry
 * in {@link #CHAIN}, a serialised chain of futures.</b> That is the core correctness device, not
 * merely an ordering nicety:
 *
 * <ul>
 *   <li>A rejoin cannot read stale rows, because the new login's load physically cannot start until
 *       the previous session's last write has committed.
 *   <li>An unlock followed by a revoke cannot land out of order. Submitting both to a plain thread
 *       pool — as {@code CurrencyRepository} does — would allow exactly that.
 * </ul>
 *
 * <p>Threading: {@link #CACHE} is written only from the server thread (mutations, and load
 * completions bounced back through {@code server.execute}). {@link #CHAIN} is touched from the
 * server thread and from database threads, which is what {@link ConcurrentHashMap} is for. Database
 * tasks close over immutable arguments and never read the cache.
 */
public final class ItemSkinStore {

  private ItemSkinStore() {}

  private enum State { LOADING, READY, FAILED }

  /**
   * Outcome of a mutation. {@code UNCHANGED} is kept distinct from {@code CHANGED} because callers
   * disagree about it: asking for a skin that is already active is a success, while unlocking one
   * you already own is not.
   */
  public enum Result { CHANGED, UNCHANGED, UNAVAILABLE }

  private record Entry(PlayerItemSkins skins, State state) {}

  private static final Map<UUID, Entry> CACHE = new ConcurrentHashMap<>();
  private static final Map<UUID, CompletableFuture<Void>> CHAIN = new ConcurrentHashMap<>();

  /** Set while the server is stopping, so nothing queues onto a pool that is about to close. */
  private static volatile boolean closing;

  public static PlayerItemSkins get(UUID uuid) {
    Entry entry = CACHE.get(uuid);
    return entry == null ? PlayerItemSkins.EMPTY : entry.skins();
  }

  /** Whether this player's cosmetics can be changed right now: loaded, storage up, not stopping. */
  public static boolean isReady(UUID uuid) {
    Entry entry = CACHE.get(uuid);
    return entry != null
            && entry.state() == State.READY
            && CosmeticsDatabase.getInstance().isAvailable()
            && !closing;
  }

  // ---------------------------------------------------------------- mutations (server thread only)

  public static Result unlock(UUID uuid, ResourceLocation skinId) {
    return mutate(uuid,
            before -> before.withUnlocked(skinId),
            () -> ItemSkinRepository.unlockBlocking(uuid, skinId));
  }

  public static Result revoke(UUID uuid, ResourceLocation skinId) {
    return mutate(uuid,
            before -> before.withoutUnlocked(skinId),
            () -> ItemSkinRepository.revokeBlocking(uuid, skinId));
  }

  public static Result setActive(UUID uuid, SkinCategory category, ResourceLocation skinId) {
    return mutate(uuid,
            before -> before.withActive(category, skinId),
            () -> ItemSkinRepository.setActiveBlocking(uuid, category, skinId));
  }

  public static Result clearActive(UUID uuid, SkinCategory category) {
    return mutate(uuid,
            before -> before.withoutActive(category),
            () -> ItemSkinRepository.clearActiveBlocking(uuid, category));
  }

  /**
   * Applies {@code change} to the cache and queues {@code write}. Returns false when the change was
   * a no-op or when this player's data is not in a state to be changed.
   *
   * <p>Refusing while {@code LOADING} is deliberate: the delta would be computed against
   * {@link PlayerItemSkins#EMPTY} and would write a row that contradicts what the load is about to
   * return. The window is milliseconds against a local file.
   */
  private static Result mutate(UUID uuid,
                               java.util.function.UnaryOperator<PlayerItemSkins> change,
                               Runnable write) {
    if (!isReady(uuid)) {
      return Result.UNAVAILABLE;
    }
    Entry entry = CACHE.get(uuid);
    PlayerItemSkins after = change.apply(entry.skins());
    if (after == entry.skins()) {
      // The with*/without* methods return the same instance when nothing changed.
      return Result.UNCHANGED;
    }
    CACHE.put(uuid, new Entry(after, State.READY));
    enqueue(uuid, write);
    return Result.CHANGED;
  }

  // ---------------------------------------------------------------------------------- lifecycle

  /**
   * Loads this player's cosmetics, then announces them.
   *
   * <p>Login only. Respawns and dimension changes go to {@code ItemSkinService.resync}: re-reading
   * SQL on every death would be pure waste.
   */
  public static void onJoin(ServerPlayer player) {
    UUID uuid = player.getUUID();
    MinecraftServer server = player.server;

    // Read the legacy attachment here, on the server thread, before dispatching.
    PlayerItemSkins attachment = player.getData(RMMAttachment.ITEM_SKINS);

    if (!CosmeticsDatabase.getInstance().isAvailable() || closing) {
      CACHE.put(uuid, new Entry(PlayerItemSkins.EMPTY, State.FAILED));
      return;
    }
    CACHE.put(uuid, new Entry(PlayerItemSkins.EMPTY, State.LOADING));

    enqueue(uuid, () -> {
      Loaded loaded;
      try {
        loaded = importLegacyAttachment(uuid, ItemSkinRepository.loadBlocking(uuid), attachment);
      } catch (Exception e) {
        LOGGER.error("[Cosmetics] Failed to load skins for {}", uuid, e);
        server.execute(() -> CACHE.put(uuid, new Entry(PlayerItemSkins.EMPTY, State.FAILED)));
        return;
      }
      Loaded result = loaded;
      server.execute(() -> {
        // Re-fetch: they may have disconnected or respawned while we were in SQL.
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online == null) {
          CACHE.remove(uuid);
          return;
        }
        CACHE.put(uuid, new Entry(result.skins(), State.READY));
        if (result.imported()) {
          // Cleared only once the import is committed, and only when it actually ran — clearing it
          // after a skipped import would destroy the very data the import declined to overwrite.
          online.setData(RMMAttachment.ITEM_SKINS, PlayerItemSkins.EMPTY);
        }
        ItemSkinService.resync(online);
      });
    });
  }

  /**
   * One-time migration of the pre-database world attachment.
   *
   * <p>The "already imported" flag is the attachment being empty — no extra column, no bookkeeping,
   * idempotent by construction. If the database is unreachable the attachment is left untouched and
   * the import simply happens next session instead.
   *
   * <p>A full snapshot write is safe here and nowhere else, because it only runs when the player has
   * no rows at all.
   */
  private static Loaded importLegacyAttachment(UUID uuid,
                                               PlayerItemSkins loaded,
                                               @Nullable PlayerItemSkins attachment) {
    if (attachment == null || (attachment.unlocked().isEmpty() && attachment.active().isEmpty())) {
      return new Loaded(loaded, false);
    }
    if (!loaded.unlocked().isEmpty() || !loaded.active().isEmpty()) {
      // The database already has this player; it wins, and the attachment is left alone rather than
      // cleared, so nothing is destroyed by an import that chose not to run.
      return new Loaded(loaded, false);
    }
    ItemSkinRepository.importBlocking(uuid, attachment);
    LOGGER.info("[Cosmetics] Imported {} unlocked skin(s) from the world attachment for {}",
            attachment.unlocked().size(), uuid);
    return new Loaded(attachment, true);
  }

  /** What a load produced, and whether the legacy attachment may now be cleared. */
  private record Loaded(PlayerItemSkins skins, boolean imported) {}

  /**
   * Drops the cache entry. Deliberately does not wait on the chain: any queued write keeps running
   * on the daemon executor, and a fast rejoin is safe because its load queues behind that write.
   */
  public static void onLeave(UUID uuid) {
    CACHE.remove(uuid);
  }

  /**
   * Waits for every queued write, then clears. Must run <b>before</b>
   * {@link CosmeticsDatabase#shutdown()}: chains submit their successors from completion callbacks,
   * so a submission after the executor closes throws {@link java.util.concurrent.RejectedExecutionException}.
   *
   * <p>Blocking the server thread is intentional here — the server is already stopping, and losing a
   * player's last unlock to save a few hundred milliseconds would be a poor trade.
   */
  public static void flush(Duration timeout) {
    closing = true;
    CompletableFuture<?>[] pending = CHAIN.values().toArray(CompletableFuture[]::new);
    if (pending.length > 0) {
      LOGGER.info("[Cosmetics] Flushing {} pending skin write(s)...", pending.length);
      try {
        CompletableFuture.allOf(pending).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        LOGGER.error("[Cosmetics] {} skin write(s) did not finish within {} and were LOST.",
                pending.length, timeout, e);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        LOGGER.error("[Cosmetics] Error while flushing skin writes", e);
      }
    }
    CACHE.clear();
    CHAIN.clear();
    closing = false; // so the next world in this session starts clean
  }

  /** Appends a database task to this player's serialised chain. Server thread only. */
  private static void enqueue(UUID uuid, Runnable task) {
    ExecutorService executor = CosmeticsDatabase.getInstance().getExecutor();
    if (executor == null) {
      return;
    }
    try {
      chain(uuid, task, executor);
    } catch (RejectedExecutionException e) {
      // The pool closed between the isReady check and here — a shutdown racing a last mutation.
      // Losing that write is bad; throwing it onto the server thread would be worse.
      LOGGER.error("[Cosmetics] Storage closed before a skin write for {} could be queued", uuid, e);
    }
  }

  private static void chain(UUID uuid, Runnable task, ExecutorService executor) {
    CHAIN.compute(uuid, (key, previous) -> {
      CompletableFuture<Void> base =
              previous == null ? CompletableFuture.completedFuture(null) : previous;
      // handleAsync, not thenRunAsync: one failed task must not poison the rest of the chain.
      CompletableFuture<Void> next = base.handleAsync((ignored, error) -> {
        try {
          task.run();
        } catch (Exception e) {
          LOGGER.error("[Cosmetics] Skin write failed for {}", uuid, e);
        }
        return null;
      }, executor);
      // Remove only if still the tail, so tidying a finished chain cannot drop a queued successor.
      next.whenComplete((ignored, error) -> CHAIN.remove(uuid, next));
      return next;
    });
  }
}
