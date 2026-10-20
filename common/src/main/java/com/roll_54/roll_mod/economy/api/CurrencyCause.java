package com.roll_54.roll_mod.economy.api;

import com.roll_54.roll_mod.economy.currency.ledger.LedgerReason;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why money is moving, and on whose say-so. Every call that changes a balance has to carry one, so
 * nothing can add, take or set money without the audit log ({@code minestar/logs/currency_logs.txt})
 * saying who did it — see {@code CurrencyAudit}.
 *
 * <p>The account being changed is the call's own argument; this is everything around it:
 *
 * @param initiator who started it — a player, an admin or the console, or a named system
 * @param sender    the other side of the movement, when there is one: the buyer paying a seller, the
 *                  bidder whose bid released somebody's escrow, the offer's sender
 * @param reason    what kind of movement it is, in the money history's own terms
 * @param detail    short free text naming the thing — a warp, a plot, a quest, "refund: listing gone"
 */
public record CurrencyCause(Initiator initiator, @Nullable Party sender, LedgerReason reason, String detail) {

  public enum Kind { PLAYER, CONSOLE, SYSTEM }

  /** @param id the player's uuid; null for the console and for systems */
  public record Initiator(Kind kind, @Nullable UUID id, String name) {}

  public record Party(UUID id, String name) {}

  public CurrencyCause {
    detail = detail == null ? "" : detail;
  }

  /** A player's own action: they bought, bid, paid, claimed, travelled. */
  public static CurrencyCause player(ServerPlayer player, LedgerReason reason) {
    return new CurrencyCause(
        new Initiator(Kind.PLAYER, player.getUUID(), player.getGameProfile().getName()),
        null, reason, "");
  }

  /** A command: the player who typed it, or the console (or a command block) by its name. */
  public static CurrencyCause command(CommandSourceStack source, LedgerReason reason) {
    ServerPlayer player = source.getPlayer();
    if (player != null) return player(player, reason);
    return new CurrencyCause(new Initiator(Kind.CONSOLE, null, source.getTextName()), null, reason, "");
  }

  /** Something the server does on its own — a playtime tick, an expiry sweep, a refund. */
  public static CurrencyCause system(String name, LedgerReason reason) {
    return new CurrencyCause(new Initiator(Kind.SYSTEM, null, name), null, reason, "");
  }

  public CurrencyCause withSender(@Nullable UUID id, @Nullable String name) {
    if (id == null) return this;
    return new CurrencyCause(initiator, new Party(id, name == null ? "" : name), reason, detail);
  }

  public CurrencyCause withSender(ServerPlayer player) {
    return withSender(player.getUUID(), player.getGameProfile().getName());
  }

  public CurrencyCause withReason(LedgerReason reason) {
    return new CurrencyCause(initiator, sender, reason, detail);
  }

  public CurrencyCause withDetail(String detail) {
    return new CurrencyCause(initiator, sender, reason, detail);
  }
}
