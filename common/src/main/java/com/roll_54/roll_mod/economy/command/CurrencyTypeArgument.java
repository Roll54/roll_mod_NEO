package com.roll_54.roll_mod.economy.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class CurrencyTypeArgument implements ArgumentType<CurrencyType> {

  private static final SimpleCommandExceptionType ERROR_INVALID =
      new SimpleCommandExceptionType(Component.literal("Невідомий тип валюти"));

  public static CurrencyTypeArgument currencyType() {
    return new CurrencyTypeArgument();
  }

  public static CurrencyType getCurrencyType(CommandContext<?> context, String name) {
    return context.getArgument(name, CurrencyType.class);
  }

  @Override
  public CurrencyType parse(StringReader reader) throws CommandSyntaxException {
    String name = reader.readUnquotedString();
    return Arrays.stream(CurrencyType.values())
        .filter(t -> t.id().equalsIgnoreCase(name) || t.name().equalsIgnoreCase(name))
        .findFirst()
        .orElseThrow(ERROR_INVALID::create);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context, SuggestionsBuilder builder) {
    return SharedSuggestionProvider.suggest(
        Arrays.stream(CurrencyType.values()).map(CurrencyType::id), builder);
  }
}
