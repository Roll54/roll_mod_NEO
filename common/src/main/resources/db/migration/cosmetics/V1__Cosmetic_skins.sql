-- Cosmetic entitlements. Deliberately outside the world save: a world reset must not destroy
-- skins a player earned or paid for.
--
-- One row per grant rather than one blob per player. An unlock or a revoke is then a single-row
-- delta, so nothing ever read-modify-writes the whole set, and a skin id this build no longer
-- recognises can be left alone instead of being erased by a full-snapshot write.

CREATE TABLE IF NOT EXISTS cosmetic_unlocked_skins
(
    -- player_uuid, not uuid: UUID is a data type keyword in H2 2.x.
    player_uuid CHAR(36)     NOT NULL,
    skin_id     VARCHAR(128) NOT NULL,
    unlocked_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_uuid, skin_id)
);

-- At most one skin per slot, enforced by the primary key rather than by application code, so
-- "set active" is exactly one upsert.
CREATE TABLE IF NOT EXISTS cosmetic_active_skins
(
    player_uuid CHAR(36)     NOT NULL,
    -- SkinCategory.getSerializedName(), never the ordinal: reordering the enum must not silently
    -- reinterpret stored rows. SkinCategory.byName is nullable, so a row for a category that no
    -- longer exists is dropped on load instead of crashing.
    category    VARCHAR(32)  NOT NULL,
    skin_id     VARCHAR(128) NOT NULL,
    PRIMARY KEY (player_uuid, category)
);

CREATE INDEX IF NOT EXISTS idx_cosmetic_unlocked_skin ON cosmetic_unlocked_skins (skin_id);
