-- Every Auction House action a player can look back on (Auction House → History): what they
-- listed, cancelled, bought, sold, bid, got back, won and claimed.
--
-- One row per player per action: a sale writes BOUGHT for the buyer and SOLD for the seller. Rows
-- are only ever written after the action has actually gone through; the listings and claims
-- themselves still live in the world's AuctionData, and the money in currency_balances, so a
-- missing row never loses an item or a coin, only history. Not seasonal: the money wipe leaves it.
CREATE TABLE IF NOT EXISTS auction_history
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    uuid       CHAR(36)     NOT NULL,
    -- AuctionAction's name. Stored by name, so those names must never change.
    action     VARCHAR(32)  NOT NULL,
    -- The listing acted on; NULL for a collection claim, which belongs to no listing any more.
    listing_id CHAR(36)     NULL,
    -- The item's registry id (minecraft:diamond) and how many. The client resolves the id back to
    -- an icon and a name in its own language.
    item       VARCHAR(256) NULL,
    item_count INT          NOT NULL DEFAULT 0,
    -- The money involved: the price paid or asked, the bid placed, the escrow returned. 0 when none.
    price      BIGINT       NOT NULL DEFAULT 0,
    -- The other side, when there was one. The name is kept so the row reads without a profile lookup.
    other_uuid CHAR(36)     NULL,
    other_name VARCHAR(16)  NULL,
    -- Epoch millis, like every other time in the mod's tables.
    created_at BIGINT       NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_auction_history_player (uuid, created_at),
    INDEX idx_auction_history_time (created_at)
);
