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
}
