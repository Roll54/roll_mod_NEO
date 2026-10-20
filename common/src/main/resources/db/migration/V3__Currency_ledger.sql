-- Every movement of money a player can see in their history (Home tab → History), and what the
-- "top earners" leaderboard sums over.
--
-- One row per player per movement: a vending sale writes one row for the buyer (negative) and one
-- for the owner (positive). Rows are only ever written after the money has actually moved; the
-- balance itself still lives in currency_balances, so a missing row never loses money, only history.
-- Seasonal like the balances: the admin wipe empties it together with them.
CREATE TABLE IF NOT EXISTS currency_ledger
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    uuid       CHAR(36)     NOT NULL,
    type       VARCHAR(32)  NOT NULL,
    -- Positive for income, negative for spending. An AUCTION_FEE row records what the house kept
    -- and is informational: the sale row already holds the reduced payout.
    delta      BIGINT       NOT NULL,
    reason     VARCHAR(32)  NOT NULL,
    -- What changed hands, for sales and purchases: the item's display name and how many.
    item       VARCHAR(256) NULL,
    item_count INT          NOT NULL DEFAULT 0,
    -- The other side of a trade, when there was one. The name is kept so the row reads without a
    -- profile lookup.
    other_uuid CHAR(36)     NULL,
    other_name VARCHAR(16)  NULL,
    -- Epoch millis, like every other time in the mod's tables.
    created_at BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_currency_ledger_player (uuid, created_at),
    INDEX idx_currency_ledger_time (created_at)
);
