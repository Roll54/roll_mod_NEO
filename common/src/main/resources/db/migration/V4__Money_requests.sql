-- Money a player has asked another to pay them, not answered yet.
--
-- Unlike currency_offers nothing is held: the payer's coins stay theirs until they agree, and are
-- taken only then -- so a request can simply be deleted (refused, cancelled, expired) without
-- moving money anywhere.
CREATE TABLE IF NOT EXISTS money_requests
(
    id             CHAR(36)    NOT NULL,
    requester      CHAR(36)    NOT NULL,
    -- Names as Mojang spells them, so a request can be listed and answered by name without a
    -- profile lookup per row.
    requester_name VARCHAR(16) NOT NULL,
    payer          CHAR(36)    NOT NULL,
    payer_name     VARCHAR(16) NOT NULL,
    type           VARCHAR(32) NOT NULL,
    amount         BIGINT      NOT NULL,
    -- Epoch millis, matching every other deadline in the mod.
    created_at     BIGINT      NOT NULL,
    expires_at     BIGINT      NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_money_requests_payer (payer),
    INDEX idx_money_requests_requester (requester),
    INDEX idx_money_requests_expires (expires_at)
);
