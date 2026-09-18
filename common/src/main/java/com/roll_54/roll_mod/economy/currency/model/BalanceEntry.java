package com.roll_54.roll_mod.economy.currency.model;

import java.util.UUID;
import org.jdbi.v3.core.mapper.RowMapper;

public record BalanceEntry(UUID uuid, long balance) {

  public static final RowMapper<BalanceEntry> MAPPER =
      (rs, _) -> {
        String uuidStr = rs.getString("uuid");
        UUID uuid =
            (uuidStr != null && !uuidStr.isEmpty()) ? UUID.fromString(uuidStr) : UUID.randomUUID();
        return new BalanceEntry(uuid, rs.getLong("balance"));
      };
}
