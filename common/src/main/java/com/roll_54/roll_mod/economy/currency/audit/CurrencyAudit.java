package com.roll_54.roll_mod.economy.currency.audit;

import com.mojang.authlib.GameProfile;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.api.CurrencyCause;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * The money audit trail: {@code <game dir>/minestar/logs/currency_logs.txt}, one line when a balance
 * change is asked for and one when it is answered.
 *
 * <pre>
 * [2026-10-04 12:00:00.123] #1042 REQ DEPOSIT main +250 target=Steve(uuid) initiator=PLAYER Steve(uuid) sender=Alex(uuid) reason=AUCTION_SALE caller=com.….AuctionManager.creditSeller:476 detail="…"
 * [2026-10-04 12:00:00.131] #1042 RES OK
 * </pre>
 *
 * <p>Written from inside the repositories, at the one place every balance change has to pass — so
 * nothing changes money without a line here, whichever API it came through. The request line is
 * written before the database is touched, so an attempt is on record even when it fails or the
 * server dies before it lands; the result line carries the same number.
 *
 * <p>{@code caller} is the first stack frame outside the money API itself: the code that actually
 * asked. It is read on the calling thread, at the call, because by the time the result arrives the
 * stack is the database executor's.
 *
 * <p>Never read back by the code, so plain text. Lines are queued to one writer thread, which keeps
 * them in order and keeps disk off the server thread; a failed write is logged and swallowed, like
 * {@code ModerationLog} — losing an audit line must not fail the payment.
 */
public final class CurrencyAudit {

  /** What was asked of the money store. */
  public enum Op { DEPOSIT, WITHDRAW, SET, TRANSFER, WIPE, HOLD, CLAIM, CLAIM_EXPIRED,
    /** A money request was answered by paying it: the payer's coins go to the requester. */
    REQUEST_PAY }

  private static final String FOLDER = "logs";
  private static final String FILE = "currency_logs.txt";
  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  /**
   * Frames that are the money API itself, never the caller. Matched as class-name prefixes, so
   * their nested and anonymous classes are skipped with them.
   */
  private static final List<String> API_CLASSES = List.of(
      "com.roll_54.roll_mod.economy.api.CurrencyService",
      "com.roll_54.roll_mod.economy.api.CurrencyOfferService",
      "com.roll_54.roll_mod.economy.api.MoneyRequestService",
      "com.roll_54.roll_mod.economy.api.CurrencyCause",
      "com.roll_54.roll_mod.economy.currency.repository.CurrencyRepository",
      "com.roll_54.roll_mod.economy.currency.repository.CurrencyOfferRepository",
      "com.roll_54.roll_mod.economy.currency.repository.MoneyRequestRepository",
      "com.roll_54.roll_mod.economy.currency.audit.CurrencyAudit");

  private static final AtomicLong SEQUENCE = new AtomicLong();

  private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "roll_mod currency audit");
    thread.setDaemon(true);
    return thread;
  });

  private CurrencyAudit() {}

  /**
   * Records that a change was asked for, and answers with the number its result line will carry.
   *
   * @param type   null when not known until the database answers (an offer claim, an expiry sweep)
   * @param target the account changed; for a transfer, the one paying
   * @param to     a transfer's receiving account; null otherwise
   */
  public static long begin(Op op, @Nullable CurrencyType type, long amount, @Nullable UUID target,
                           @Nullable UUID to, CurrencyCause cause) {
    long id = SEQUENCE.incrementAndGet();
    StringBuilder line = new StringBuilder();
    line.append('#').append(id).append(" REQ ").append(op).append(' ').append(type == null ? "?" : type.id())
        .append(' ').append(signed(op, amount));
    if (op == Op.WIPE || op == Op.CLAIM_EXPIRED) {
      line.append(" target=ALL");
    } else if (op == Op.TRANSFER) {
      line.append(" from=").append(who(target)).append(" to=").append(who(to));
    } else {
      line.append(" target=").append(who(target));
    }
    CurrencyCause.Initiator initiator = cause.initiator();
    line.append(" initiator=").append(initiator.kind()).append(' ').append(clean(initiator.name()));
    if (initiator.id() != null) line.append('(').append(initiator.id()).append(')');
    if (cause.sender() != null) {
      line.append(" sender=").append(named(cause.sender().id(), cause.sender().name()));
    }
    line.append(" reason=").append(cause.reason());
    line.append(" caller=").append(caller());
    if (!cause.detail().isBlank()) line.append(" detail=\"").append(clean(cause.detail())).append('"');
    write(line.toString());
    return id;
  }

  /** Writes request {@code id}'s result line once {@code operation} answers, and passes it through. */
  public static <T> CompletableFuture<T> track(long id, CompletableFuture<T> operation,
                                               Function<? super T, String> outcome) {
    return operation.whenComplete((result, error) -> {
      String text;
      if (error != null) {
        text = "FAILED " + error;
      } else {
        try {
          text = outcome.apply(result);
        } catch (RuntimeException e) {
          text = "OK " + result;
        }
      }
      write("#" + id + " RES " + text);
    });
  }

  /** A further line under request {@code id} — one refunded offer of an expiry sweep, say. */
  public static void note(long id, String text) {
    write("#" + id + " ... " + clean(text));
  }

  /** {@code OK} for a write that went through, {@code REFUSED} for one that was turned down. */
  public static String ok(Boolean success) {
    return Boolean.TRUE.equals(success) ? "OK" : "REFUSED";
  }

  /** Waits, briefly, for every queued line to reach the file. Called when the server stops. */
  public static void flush() {
    try {
      WRITER.submit(() -> {}).get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      RollMod.LOGGER.warn("[Economy] currency audit log may be missing its last lines", e);
    }
  }

  /* ------------------------------------------- helpers ------------------------------------------- */

  private static String signed(Op op, long amount) {
    return switch (op) {
      case DEPOSIT -> "+" + amount;
      // Read from the offer row inside the transaction; the result line carries it.
      case CLAIM, REQUEST_PAY -> "?";
      case WITHDRAW, HOLD -> "-" + amount;
      case SET -> "=" + amount;
      case WIPE, CLAIM_EXPIRED -> "*";
      case TRANSFER -> String.valueOf(amount);
    };
  }

  /** The first frame that is not the money API, the JDK or a lambda shim: who really asked. */
  private static String caller() {
    return StackWalker.getInstance().walk(frames -> frames
        .filter(frame -> !isPlumbing(frame.getClassName()))
        .findFirst()
        .map(frame -> frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber())
        .orElse("unknown"));
  }

  private static boolean isPlumbing(String className) {
    if (className.startsWith("java.") || className.startsWith("jdk.") || className.startsWith("sun.")
        || className.contains("$$Lambda")) {
      return true;
    }
    for (String api : API_CLASSES) {
      if (className.equals(api) || className.startsWith(api + "$")) return true;
    }
    return false;
  }

  /** {@code Name(uuid)}, the name looked up from the server when it knows one. */
  private static String who(@Nullable UUID id) {
    if (id == null) return "none";
    return named(id, nameOf(id));
  }

  private static String named(UUID id, @Nullable String name) {
    return (name == null || name.isBlank() ? "?" : clean(name)) + "(" + id + ")";
  }

  @Nullable
  private static String nameOf(UUID id) {
    try {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      if (server == null) return null;
      ServerPlayer online = server.getPlayerList().getPlayer(id);
      if (online != null) return online.getGameProfile().getName();
      if (server.getProfileCache() == null) return null;
      Optional<GameProfile> cached = server.getProfileCache().get(id);
      return cached.map(GameProfile::getName).orElse(null);
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** One line per entry, whatever a name or detail contains. */
  private static String clean(String text) {
    return text.replace('\n', ' ').replace('\r', ' ').replace('"', '\'');
  }

  private static void write(String body) {
    String line = "[" + LocalDateTime.now().format(TIMESTAMP) + "] " + body + System.lineSeparator();
    try {
      WRITER.execute(() -> {
        try {
          Path file = MinestarFiles.folder(FOLDER).resolve(FILE);
          Files.writeString(file, line, StandardCharsets.UTF_8,
              StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
          RollMod.LOGGER.error("[Economy] could not write the currency audit log: {}", line.strip(), e);
        }
      });
    } catch (RuntimeException e) {
      RollMod.LOGGER.error("[Economy] could not queue a currency audit line: {}", line.strip(), e);
    }
  }
}
