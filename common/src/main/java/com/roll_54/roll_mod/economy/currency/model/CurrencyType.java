package com.roll_54.roll_mod.economy.currency.model;

public enum CurrencyType {
  MAIN("main"),
  EVENT("event"),
  EVENT2("event2"),
  BATTLEPASS("battlepass");

  private final String id;

  CurrencyType(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  /**
   * The type a stored {@code id} names, or empty when nothing answers to it.
   *
   * <p>Empty rather than a default: a row naming a currency this build no longer has is a row we
   * must not guess about, since guessing moves money into the wrong pocket.
   */
  public static java.util.Optional<CurrencyType> byId(String id) {
    for (CurrencyType type : values()) {
      if (type.id.equalsIgnoreCase(id)) return java.util.Optional.of(type);
    }
    return java.util.Optional.empty();
  }
}
