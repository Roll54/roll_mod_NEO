package com.roll_54.roll_mod.economy.command;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CurrencyCommand {

  private static final Component CURRENCY_NAME =
      Component.translatable("currency.rollcurrency.name");

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

    // ============================================
    // NON-OP COMMANDS (use CurrencyType.MAIN only)
    // ============================================

    dispatcher.register(
        Commands.literal("rmc")

            // ===== /rmc get [player] =====
            // TODO: make second player have to accept
            .then(
                Commands.literal("get")
                    .executes(CurrencyCommand::checkSelf)
                    .then(
                        Commands.argument("target", EntityArgument.player())
                            .executes(CurrencyCommand::checkOther)))

            // ===== /rmc pay <target> <amount> =====
            .then(
                Commands.literal("pay")
                    .then(
                        Commands.argument("target", EntityArgument.player())
                            .then(
                                Commands.argument("amount", LongArgumentType.longArg(1))
                                    .executes(CurrencyCommand::payPlayer))))

            // ===== /rmc top [type] =====
            .then(
                Commands.literal("top")
                    .executes(ctx -> showTop(ctx, CurrencyType.MAIN))
                    .then(
                        Commands.argument("type", CurrencyTypeArgument.currencyType())
                            .executes(
                                ctx ->
                                    showTop(
                                        ctx, CurrencyTypeArgument.getCurrencyType(ctx, "type")))))

            // ============================================
            // OP-ONLY ADMIN COMMANDS (with type parameter)
            // ============================================

            // ===== /rmc admin ... =====
            .then(
                Commands.literal("admin")
                    .requires(s -> s.hasPermission(2)) // OP level 2

                    // set
                    .then(
                        Commands.literal("set")
                            .then(
                                Commands.argument("targets", EntityArgument.players())
                                    .then(
                                        Commands.argument(
                                                "type", CurrencyTypeArgument.currencyType())
                                            .then(
                                                Commands.argument(
                                                        "amount", LongArgumentType.longArg(0))
                                                    .executes(
                                                        ctx -> modifyBalance(ctx, Mode.SET))))))

                    // add
                    .then(
                        Commands.literal("add")
                            .then(
                                Commands.argument("targets", EntityArgument.players())
                                    .then(
                                        Commands.argument(
                                                "type", CurrencyTypeArgument.currencyType())
                                            .then(
                                                Commands.argument(
                                                        "amount", LongArgumentType.longArg(1))
                                                    .executes(
                                                        ctx -> modifyBalance(ctx, Mode.ADD))))))

                    // take
                    .then(
                        Commands.literal("take")
                            .then(
                                Commands.argument("targets", EntityArgument.players())
                                    .then(
                                        Commands.argument(
                                                "type", CurrencyTypeArgument.currencyType())
                                            .then(
                                                Commands.argument(
                                                        "amount", LongArgumentType.longArg(1))
                                                    .executes(
                                                        ctx -> modifyBalance(ctx, Mode.TAKE))))))));
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

  private static int payPlayer(CommandContext<CommandSourceStack> ctx) {
    try {
      ServerPlayer sender = ctx.getSource().getPlayerOrException();
      ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
      long amount = LongArgumentType.getLong(ctx, "amount");

      if (sender.equals(target)) {
        ctx.getSource().sendFailure(Component.translatable("command.rmc.pay.error.self"));
        return 0;
      }

      CurrencyService.transfer(
              sender, target, CurrencyType.MAIN, amount, ctx.getSource().getServer())
          .thenAcceptAsync(
              success -> {
                if (success) {
                  // Sender message
                  ctx.getSource()
                      .sendSuccess(
                          () ->
                              Component.translatable(
                                  "command.rmc.pay.success.sender",
                                  amount,
                                  CURRENCY_NAME,
                                  target.getName().getString()),
                          true);

                  // Receiver message
                  target.sendSystemMessage(
                      Component.translatable(
                          "command.rmc.pay.success.receiver",
                          amount,
                          CURRENCY_NAME,
                          sender.getName().getString()));
                } else {
                  ctx.getSource()
                      .sendFailure(
                          Component.translatable(
                              "command.rmc.pay.error.insufficient", amount, CURRENCY_NAME));
                }
              },
              ctx.getSource().getServer());

    } catch (Exception e) {
      return 0;
    }
    return 1;
  }

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

  private enum Mode {
    SET,
    ADD,
    TAKE
  }

  private static int modifyBalance(CommandContext<CommandSourceStack> ctx, Mode mode) {
    try {
      Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
      CurrencyType type = CurrencyTypeArgument.getCurrencyType(ctx, "type");
      long amount = LongArgumentType.getLong(ctx, "amount");

      for (ServerPlayer target : targets) {
        switch (mode) {
          case SET ->
              CurrencyService.set(target, type, amount)
                  .thenAcceptAsync(
                      success -> sendFeedback(ctx, target, "set", amount, type, success),
                      ctx.getSource().getServer());

          case ADD ->
              CurrencyService.deposit(target, type, amount, ctx.getSource().getServer())
                  .thenAcceptAsync(
                      success -> sendFeedback(ctx, target, "add", amount, type, success),
                      ctx.getSource().getServer());

          case TAKE ->
              CurrencyService.withdraw(target, type, amount)
                  .thenAcceptAsync(
                      success -> sendFeedback(ctx, target, "take", amount, type, success),
                      ctx.getSource().getServer());
        }
      }
    } catch (Exception e) {
      LOGGER.error("Error modifying balance", e);
      return 0;
    }
    return 1;
  }

  private static void sendFeedback(
      CommandContext<CommandSourceStack> ctx,
      ServerPlayer target,
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
                      target.getName().getString(),
                      amount,
                      CURRENCY_NAME,
                      type.id()),
              true);
    } else {
      ctx.getSource()
          .sendFailure(
              Component.literal(
                  "Failed to " + actionKey + " balance for " + target.getName().getString()));
    }
  }
}
