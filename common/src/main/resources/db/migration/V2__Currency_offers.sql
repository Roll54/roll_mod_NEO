-- Money a player has offered to another and which has not been answered yet.
--
-- The amount is already gone from the sender's balance when the row is written: an offer holds the
-- money, so what the recipient is shown is money that is really there, and the sender cannot promise
-- the same coins twice. Every row therefore ends one of three ways -- accepted, denied or expired --
-- and each of the three moves the amount somewhere. A row that is simply deleted loses money.
CREATE TABLE IF NOT EXISTS currency_offers
(
    id             CHAR(36)    NOT NULL,
    sender         CHAR(36)    NOT NULL,
    -- Names as Mojang spells them, kept so an offer can be listed and answered by name without a
    -- profile lookup per row -- including for a sender who has not logged in since.
    sender_name    VARCHAR(16) NOT NULL,
    recipient      CHAR(36)    NOT NULL,
    recipient_name VARCHAR(16) NOT NULL,
    type           VARCHAR(32) NOT NULL,
    amount         BIGINT      NOT NULL,
    -- Epoch millis, matching every other deadline in the mod (TpaRequest, KitView).
    expires_at     BIGINT      NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_currency_offers_recipient (recipient),
    INDEX idx_currency_offers_sender (sender),
    INDEX idx_currency_offers_expires (expires_at)
);
