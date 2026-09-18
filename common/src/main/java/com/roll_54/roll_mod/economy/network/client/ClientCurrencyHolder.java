package com.roll_54.roll_mod.economy.network.client;

import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import java.util.EnumMap;
import java.util.Map;

public final class ClientCurrencyHolder {

  private static final Map<CurrencyType, Long> BALANCES = new EnumMap<>(CurrencyType.class);

  private ClientCurrencyHolder() {}

  public static void set(CurrencyType type, long amount) {
    BALANCES.put(type, amount);
  }

  public static long get(CurrencyType type) {
    return BALANCES.getOrDefault(type, 0L);
  }

  public static long getMainBalance() {
    return get(CurrencyType.MAIN);
  }
}
