package com.roll_54.roll_mod.cosmetics.storage;

import static com.roll_54.roll_mod.RollMod.LOGGER;

import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import com.roll_54.roll_mod.cosmetics.PlayerItemSkins;
import com.roll_54.roll_mod.cosmetics.SkinCategory;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.jdbi.v3.core.Jdbi;

/**
 * The SQL behind cosmetic entitlements. Every method blocks and must therefore only ever run on the
 * cosmetics executor — {@link ItemSkinStore} is the only legitimate caller, and it serialises these
 * per player.
 *
 * <p>Writes are deltas, never snapshots. Two reasons, both load-bearing: a delta cannot clobber a
 * concurrent grant made elsewhere, and — more importantly — a snapshot write would delete rows for
 * skin ids this build does not recognise. {@link #loadBlocking} filters those out of memory but
 * deliberately leaves them in the table, so a mod update that restores a skin restores the unlock.
 */
final class ItemSkinRepository {

  private ItemSkinRepository() {}

  /** Matches VARCHAR(128) in the schema. A ResourceLocation may in theory be longer. */
  private static final int MAX_ID_LENGTH = 128;

  private static Jdbi jdbi() {
    return CosmeticsDatabase.getInstance().getJdbi();
  }

  static PlayerItemSkins loadBlocking(UUID uuid) {
    return jdbi().withHandle(handle -> {
      Set<ResourceLocation> unlocked = new LinkedHashSet<>();
      handle.createQuery("SELECT skin_id FROM cosmetic_unlocked_skins WHERE player_uuid = :uuid")
          .bind("uuid", uuid.toString())
          .mapTo(String.class)
          .forEach(raw -> {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            // Unknown to this build: keep the row, keep it out of memory.
            if (id != null && ItemSkinRegistry.get(id) != null) {
              unlocked.add(id);
            }
          });

      Map<SkinCategory, ResourceLocation> active = new EnumMap<>(SkinCategory.class);
      handle.createQuery(
              "SELECT category, skin_id FROM cosmetic_active_skins WHERE player_uuid = :uuid")
          .bind("uuid", uuid.toString())
          .map((rs, ctx) -> Map.entry(rs.getString("category"), rs.getString("skin_id")))
          .forEach(row -> {
            SkinCategory category = SkinCategory.byName(row.getKey());
            ResourceLocation id = ResourceLocation.tryParse(row.getValue());
            if (category != null && id != null && ItemSkinRegistry.get(id) != null) {
              active.put(category, id);
            }
          });

      return new PlayerItemSkins(unlocked, active);
    });
  }

  static void unlockBlocking(UUID uuid, ResourceLocation skinId) {
    if (tooLong(skinId)) {
      return;
    }
    // The no-op update is the portable INSERT IGNORE. Note the value is bound, never VALUES(col):
    // that function is deprecated in MySQL 8.0.20+ and unreliable in H2's MySQL mode.
    jdbi().useHandle(handle -> handle.createUpdate("""
                INSERT INTO cosmetic_unlocked_skins (player_uuid, skin_id)
                VALUES (:uuid, :skin)
                ON DUPLICATE KEY UPDATE skin_id = :skin
            """)
        .bind("uuid", uuid.toString())
        .bind("skin", skinId.toString())
        .execute());
  }

  /**
   * Revokes a skin and stops it rendering, in one transaction — mirroring
   * {@code PlayerItemSkins#withoutUnlocked}, where dropping the unlock also drops the active entry.
   * Split across two statements they could partially apply and leave a skin rendering that its owner
   * no longer owns.
   */
  static void revokeBlocking(UUID uuid, ResourceLocation skinId) {
    jdbi().useTransaction(handle -> {
      handle.createUpdate(
              "DELETE FROM cosmetic_unlocked_skins WHERE player_uuid = :uuid AND skin_id = :skin")
          .bind("uuid", uuid.toString())
          .bind("skin", skinId.toString())
          .execute();
      handle.createUpdate(
              "DELETE FROM cosmetic_active_skins WHERE player_uuid = :uuid AND skin_id = :skin")
          .bind("uuid", uuid.toString())
          .bind("skin", skinId.toString())
          .execute();
    });
  }

  static void setActiveBlocking(UUID uuid, SkinCategory category, ResourceLocation skinId) {
    if (tooLong(skinId)) {
      return;
    }
    jdbi().useHandle(handle -> handle.createUpdate("""
                INSERT INTO cosmetic_active_skins (player_uuid, category, skin_id)
                VALUES (:uuid, :category, :skin)
                ON DUPLICATE KEY UPDATE skin_id = :skin
            """)
        .bind("uuid", uuid.toString())
        .bind("category", category.getSerializedName())
        .bind("skin", skinId.toString())
        .execute());
  }

  static void clearActiveBlocking(UUID uuid, SkinCategory category) {
    jdbi().useHandle(handle -> handle.createUpdate(
            "DELETE FROM cosmetic_active_skins WHERE player_uuid = :uuid AND category = :category")
        .bind("uuid", uuid.toString())
        .bind("category", category.getSerializedName())
        .execute());
  }

  /**
   * Writes a whole snapshot. Only safe where the row set is known to be empty, which is exactly the
   * one-time import of a pre-database world attachment — see {@code ItemSkinStore}. Never call it to
   * "repair" state: it would delete rows for skins this build does not know about.
   */
  static void importBlocking(UUID uuid, PlayerItemSkins skins) {
    jdbi().useTransaction(handle -> {
      for (ResourceLocation skinId : skins.unlocked()) {
        if (tooLong(skinId)) {
          continue;
        }
        handle.createUpdate("""
                    INSERT INTO cosmetic_unlocked_skins (player_uuid, skin_id)
                    VALUES (:uuid, :skin)
                    ON DUPLICATE KEY UPDATE skin_id = :skin
                """)
            .bind("uuid", uuid.toString())
            .bind("skin", skinId.toString())
            .execute();
      }
      for (Map.Entry<SkinCategory, ResourceLocation> entry : skins.active().entrySet()) {
        if (tooLong(entry.getValue())) {
          continue;
        }
        handle.createUpdate("""
                    INSERT INTO cosmetic_active_skins (player_uuid, category, skin_id)
                    VALUES (:uuid, :category, :skin)
                    ON DUPLICATE KEY UPDATE skin_id = :skin
                """)
            .bind("uuid", uuid.toString())
            .bind("category", entry.getKey().getSerializedName())
            .bind("skin", entry.getValue().toString())
            .execute();
      }
    });
  }

  /** Log and skip rather than let the database truncate an id silently. */
  private static boolean tooLong(ResourceLocation skinId) {
    if (skinId.toString().length() > MAX_ID_LENGTH) {
      LOGGER.error("[Cosmetics] Skin id {} exceeds {} characters and cannot be stored",
              skinId, MAX_ID_LENGTH);
      return true;
    }
    return false;
  }
}
