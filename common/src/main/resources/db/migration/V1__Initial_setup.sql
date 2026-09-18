CREATE TABLE IF NOT EXISTS currency_balances
(
    uuid       CHAR(36)    NOT NULL,
    type       VARCHAR(32) NOT NULL,
    balance    BIGINT      NOT NULL DEFAULT 0,
    updated_at TIMESTAMP            DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid, type)
);