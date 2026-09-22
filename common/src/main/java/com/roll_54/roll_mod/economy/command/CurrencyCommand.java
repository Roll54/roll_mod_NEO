package com.roll_54.roll_mod.economy.command;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.roll_54.roll_mod.economy.api.CurrencyOfferService;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyOffer;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.PlayerLookup;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CurrencyCommand {

  private static final Component CURRENCY_NAME =
      Component.translatable("currency.rollcurrency.name");

  /** Online names, as a starting point. Anyone who has ever joined can be typed in full. */
  private static final SuggestionProvider<CommandSourceStack> PLAYERS =
      (ctx, builder) ->
          SharedSuggestionProvider.suggest(
              ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                  .map(player -> player.getGameProfile().getName()),
              builder);

  /** Who is currently offering the player money — the only names an answer can name. */
  private static final SuggestionProvider<CommandSourceStack> OFFER_SENDERS =
      (ctx, builder) -> {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return builder.buildFuture();
        return CurrencyOfferService.incoming(player.getUUID())
            .thenCompose(
                offers ->
                    SharedSuggestionProvider.suggest(
                        offers.stream().map(CurrencyOffer::senderName).distinct(), builder));
      };

  /**
   * Registers the currency tree under the mod's own root as {@code /rollmod money ...}.
   *
   * <p>There is no standalone {@code /rmc} any more, and no alias for it: this is the one root the
   * mod owns, and a currency command is no more deserving of a top-level word than a warp is.
   * Brigadier merges every {@code rollmod} literal registered here into the one node, which is how
   * the rest of the mod's commands are hung off it.
   *
   * <p>The lang keys stay {@code command.rmc.*}. They are internal ids, and renaming them would
   * touch every translation for no visible gain.
   *
   * <p>Players name each other with a plain word rather than an entity selector, following {@code
   * HomeCommands}: {@link PlayerLookup} resolves it whether or not they are logged in, which is what
   * lets an offer wait for someone and an admin pay somebody who is away.
   */
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("rollmod")
            .then(
                Commands.literal("money")

                    // ============================================
                    // NON-OP COMMANDS (use CurrencyType.MAIN only)
                    // ============================================
                    .then(
                        Commands.literal("get")
                            .executes(CurrencyCommand::checkSelf)
                            .then(
                                Commands.argument("target", EntityArgument.player())
                                    .executes(CurrencyCommand::checkOther)))

                    // ===== /rollmod money pay <player> <amount> =====
                    // An offer, not a transfer: the money is held out of the sender's balance and
                    // the recipient has to take it. See CurrencyOfferService.
                    .then(
                        Commands.literal("pay")
                            .then(
                                Commands.argument("player", StringArgumentType.word())
                                    .suggests(PLAYERS)
                                    .then(
                                        Commands.argument("amount", LongArgumentType.longArg(1))
                                            .executes(CurrencyCommand::payPlayer))))

                    // ===== /rollmod money offers =====
                    .then(Commands.literal("offers").executes(CurrencyCommand::listOffers))

                    // ===== /rollmod money accept|deny [player] =====
                    .then(answer("accept", true))
                    .then(answer("deny", false))

                    // ===== /rollmod money top [type] =====
                    .then(
                        Commands.literal("top")
                            .executes(ctx -> showTop(ctx, CurrencyType.MAIN))
                            .then(
                                Commands.argument("type", CurrencyTypeArgument.currencyType())
                                    .executes(
                                        ctx ->
                                            showTop(
                                                ctx,
                                                CurrencyTypeArgument.getCurrencyType(
                                                    ctx, "type")))))

                    // ============================================
                    // OP-ONLY ADMIN COMMANDS (with type parameter)
                    // ============================================
                    //
                    // These credit and debit outright. No offer, no acceptance, and the target need
                    // not be online — being able to force the balance is what makes it the admin
                    // command rather than a payment.
                    .then(
                        Commands.literal("admin")
                            .requires(source -> source.hasPermission(2)) // OP level 2
                            .then(adminOp("set", Mode.SET, 0))
                            .then(adminOp("add", Mode.ADD, 1))
                            .then(adminOp("take", Mode.TAKE, 1)))));
  }

  /** One of the two answers to an offer, which differ only in where the money ends up. */
  private static LiteralArgumentBuilder<CommandSourceStack> answer(String literal, boolean accept) {
    return Commands.literal(literal)
        .executes(ctx -> answerOffer(ctx, null, accept))
        .then(
            Commands.argument("player", StringArgumentType.word())
                .suggests(OFFER_SENDERS)
                .executes(
                    ctx ->
                        answerOffer(ctx, StringArgumentType.getString(ctx, "player"), accept)));
  }

  /** One admin operation. {@code minimum} is 0 for set, which may legitimately zero a balance. */
  private static LiteralArgumentBuilder<CommandSourceStack> adminOp(
      String literal, Mode mode, int minimum) {
    return Commands.literal(literal)
        .then(
            Commands.argument("player", StringArgumentType.word())
                .suggests(PLAYERS)
                .then(
                    Commands.argument("type", CurrencyTypeArgument.currencyType())
                        .then(
                            Commands.argument("amount", LongArgumentType.longArg(minimum))
                                .executes(ctx -> modifyBalance(ctx, mode)))));
  }

  private static int checkSelf(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    CurrencyService.get(player, CurrencyType.MAIN)
        .thenAcceptAsync(
            balance -> {
              ctx.getSource()
                  .sendSuccess(
                      () -> Component.translatable("command.rmc.get.self", balance, CURRENCY_NAME),
                      false);
            },
            ctx.getSource().getServer());

    return 1;
  }

  private static int checkOther(CommandContext<CommandSourceStack> ctx) {
    try {
      ServerPlayer target = EntityArgument.getPlayer(ctx, "target");

      CurrencyService.get(target, CurrencyType.MAIN)
          .thenAcceptAsync(
              balance -> {
                ctx.getSource()
                    .sendSuccess(
                        () ->
                            Component.translatable(
                                "command.rmc.get.other",
                                target.getName().getString(),
                                balance,
                                CURRENCY_NAME),
                        false);
              },
              ctx.getSource().getServer());

    } catch (Exception e) {
      return 0;
    }
    return 1;
  }

  /* --------------------------------------- offers ---------------------------------------- */

  /** Offers the named player money. They are looked up whether or not they are online. */
  private static int payPlayer(CommandContext<CommandSourceStack> ctx) {
    try {
      ServerPlayer sender = ctx.getSource().getPlayerOrException();
      String name = StringArgumentType.getString(ctx, "player");
      long amount = LongArgumentType.getLong(ctx, "amount");

      resolve(
          ctx,
          name,
          profile -> CurrencyOfferService.offer(sender, profile, CurrencyType.MAIN, amount));

    } catch (CommandSyntaxException e) {
      return 0;
    }
    return 1;
  }

  private static int answerOffer(
      CommandContext<CommandSourceStack> ctx, String senderName, boolean accept) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    if (accept) {
      CurrencyOfferService.accept(player, senderName);
    } else {
      CurrencyOfferService.deny(player, senderName);
    }
    return 1;
  }

  /** Both sides of what is outstanding: money waiting to be taken, and money waiting to be given. */
  private static int listOffers(CommandContext<CommandSourceStack> ctx) {
    ServerPlayer player = ctx.getSource().getPlayer();
    if (player == null) return 0;

    MinecraftServer server = ctx.getSource().getServer();
    UUID id = player.getUUID();

    CurrencyOfferService.incoming(id)
        .thenAcceptAsync(
            incoming ->
                CurrencyOfferService.outgoing(id)
                    .thenAcceptAsync(
                        outgoing -> {
                          if (incoming.isEmpty() && outgoing.isEmpty()) {
                            player.sendSystemMessage(
                                Component.translatable("command.rmc.offer.list.empty"));
                            return;
                          }
                          long now = System.currentTimeMillis();
                          print(player, "command.rmc.offer.list.incoming", incoming, now, true);
                          print(player, "command.rmc.offer.list.outgoing", outgoing, now, false);
                        },
                        server),
            server);
    return 1;
  }

  private static void print(
      ServerPlayer player, String header, List<CurrencyOffer> offers, long now, boolean incoming) {
    if (offers.isEmpty()) return;

    player.sendSystemMessage(Component.translatable(header).withStyle(ChatFormatting.GRAY));
    for (CurrencyOffer offer : offers) {
      player.sendSystemMessage(
          Component.translatable(
              "command.rmc.offer.list.line",
              offer.amount(),
              CURRENCY_NAME,
              incoming ? offer.senderName() : offer.recipientName(),
              Durations.format(offer.remainingMillis(now))));
    }
  }

  /* ----------------------------------------- top ----------------------------------------- */

  private static int showTop(CommandContext<CommandSourceStack> ctx, CurrencyType type) {
    MinecraftServer server = ctx.getSource().getServer();
    CurrencyService.getTop10(type)
        .thenAcceptAsync(
            entries -> {
              if (entries.isEmpty()) {
                ctx.getSource()
                    .sendSuccess(() -> Component.translatable("command.rmc.top.empty"), false);
                return;
              }

              ctx.getSource()
                  .sendSuccess(() -> Component.translatable("command.rmc.top.header"), false);

              AtomicInteger rank = new AtomicInteger(1);
              entries.forEach(
                  entry -> {
                    UUID playerUuid = entry.uuid();
                    String name;

                    if (server.getProfileCache() != null) {
                      name =
                          server
                              .getProfileCache()
                              .get(playerUuid)
                              .map(GameProfile::getName)
                              .orElse(playerUuid.toString().substring(0, 8) + "...");
                    } else {
                      name = playerUuid.toString().substring(0, 8) + "...";
                    }

                    ctx.getSource()
                        .sendSuccess(
                            () ->
                                Component.translatable(
                                    "command.rmc.top.line",
                                    rank.getAndIncrement(),
                                    name,
                                    entry.balance(),
                                    CURRENCY_NAME),
                            false);
                  });
            },
            server)
        .exceptionally(
            e -> {
              LOGGER.error("Error executing top command", e);
              return null;
            });

    return 1;
  }

  /* ---------------------------------------- admin ---------------------------------------- */

  private enum Mode {
    SET,
    ADD,
    TAKE
  }

  /**
   * Forces a balance, online or not.
   *
   * <p>Everything below the command has always been uuid-keyed, so the only thing an offline target
   * changes is that there is nobody to send the new balance to; they are told it on their next
   * login like everyone else.
   */
  private static int modifyBalance(CommandContext<CommandSourceStack> ctx, Mode mode) {
    String name = StringArgumentType.getString(ctx, "player");
    CurrencyType type = CurrencyTypeArgument.getCurrencyType(ctx, "type");
    long amount = LongArgumentType.getLong(ctx, "amount");
    MinecraftServer server = ctx.getSource().getServer();

    resolve(
        ctx,
        name,
        profile -> {
          switch (mode) {
            case SET ->
                CurrencyService.set(server, profile.id(), type, amount)
                    .thenAcceptAsync(
                        success -> sendFeedback(ctx, profile, "set", amount, type, success),
                        server);

            case ADD ->
                CurrencyService.deposit(server, profile.id(), type, amount)
                    .thenAcceptAsync(
                        success -> sendFeedback(ctx, profile, "add", amount, type, success),
                        server);

            case TAKE ->
                CurrencyService.withdraw(server, profile.id(), type, amount)
                    .thenAcceptAsync(
                        success -> sendFeedback(ctx, profile, "take", amount, type, success),
                        server);
          }
        });
    return 1;
  }

  private static void sendFeedback(
      CommandContext<CommandSourceStack> ctx,
      PlayerLookup.Profile target,
      String actionKey,
      long amount,
      CurrencyType type,
      boolean success) {
    if (success) {
      ctx.getSource()
          .sendSuccess(
              () ->
                  Component.translatable(
                      "command.rmc.admin." + actionKey + ".success",
                      target.name(),
                      amount,
                      CURRENCY_NAME,
                      type.id()),
              true);
    } else {
      ctx.getSource()
          .sendFailure(
              Component.translatable(
                  "command.rmc.admin.error.failed", actionKey, target.name()));
    }
  }

  /**
   * Turns a typed name into a player, or says there is no such player.
   *
   * <p>The callback runs on the server thread — {@link PlayerLookup#resolve} guarantees it — because
   * a name that is neither online nor cached is answered by Mojang on some other thread entirely.
   */
  private static void resolve(
      CommandContext<CommandSourceStack> ctx, String name, Consumer<PlayerLookup.Profile> then) {
    PlayerLookup.resolve(
        ctx.getSource().getServer(),
        name,
        profile -> {
          if (profile == null) {
            ctx.getSource()
                .sendFailure(Component.translatable("command.rmc.error.unknownPlayer", name));
            return;
          }
          then.accept(profile);
        });
  }
}
