package com.roll_54.roll_mod.economy.vendingblock.auction;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * The Auction House's action trail: {@code <game dir>/minestar/logs/auction_actions.txt}, one line
 * per action that went through — the same moments as {@code auction_history}, for an admin with
 * a text editor rather than a player with the GUI.
 *
 * <pre>
 * [2026-10-08 12:00:00.123] SOLD player=Steve(uuid) listing=uuid item=minecraft:diamond x3 price=900 other=Alex
 * [2026-10-08 12:00:00.124] CLAIMED player=Alex(uuid) listing=- item=minecraft:diamond x3 price=0 other=-
 * </pre>
 *
 * <p>Never read back by the code, so plain text. Lines are queued to one writer thread, which keeps
 * them in order and keeps disk off the server thread; a failed write is logged and swallowed, like
 * {@code CurrencyAudit} — losing a log line must not fail the trade.
 */
public final class AuctionLog {

  private static final String FOLDER = "logs";
  private static final String FILE = "auction_actions.txt";
  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
    Thread thread = new Thread(runnable, "roll_mod auction log");
    thread.setDaemon(true);
    return thread;
  });

  private AuctionLog() {}

  /**
   * Queues one line. Never throws.
   *
   * @param listing null for a claim, written as {@code -}
   * @param item    the stack acted on; {@code count} is what is written, not its own count
   */
  public static void write(AuctionAction action, UUID player, String name, @Nullable UUID listing,
                           @Nullable ItemStack item, int count, long price, @Nullable String otherName) {
    try {
      String itemId = item == null || item.isEmpty() ? "-"
          : BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
      String other = otherName == null || otherName.isBlank() ? "-" : clean(otherName);
      append(action + " player=" + (name == null || name.isBlank() ? "?" : clean(name)) + "(" + player + ")"
          + " listing=" + (listing == null ? "-" : listing)
          + " item=" + itemId + " x" + count
          + " price=" + price
          + " other=" + other);
    } catch (RuntimeException e) {
      RollMod.LOGGER.error("[Economy] could not format an auction log line for {}", action, e);
    }
  }

  /** Waits, briefly, for every queued line to reach the file. Called when the server stops. */
  public static void flush() {
    try {
      WRITER.submit(() -> {}).get(5, TimeUnit.SECONDS);
    } catch (Exception e) {
      RollMod.LOGGER.warn("[Economy] auction action log may be missing its last lines", e);
    }
  }

  /** One line per entry, whatever a name contains. */
  private static String clean(String text) {
    return text.replace('\n', ' ').replace('\r', ' ');
  }

  private static void append(String body) {
    String line = "[" + LocalDateTime.now().format(TIMESTAMP) + "] " + body + System.lineSeparator();
    try {
      WRITER.execute(() -> {
        try {
          Path file = MinestarFiles.folder(FOLDER).resolve(FILE);
          Files.writeString(file, line, StandardCharsets.UTF_8,
              StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
          RollMod.LOGGER.error("[Economy] could not write the auction action log: {}", line.strip(), e);
        }
      });
    } catch (RuntimeException e) {
      RollMod.LOGGER.error("[Economy] could not queue an auction log line: {}", line.strip(), e);
    }
  }
}
