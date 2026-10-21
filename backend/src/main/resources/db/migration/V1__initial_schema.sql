-- Initial schema for Water ATM. See ROADMAP.md §8 for the diagram.
-- Money is always stored as whole Kenyan shillings (integer), never floating point.

CREATE TABLE customer (
    id          UUID PRIMARY KEY,
    phone       VARCHAR(12)  NOT NULL UNIQUE,   -- 2547XXXXXXXX format (what M-Pesa expects)
    name        VARCHAR(100),
    lat         DOUBLE PRECISION,
    lng         DOUBLE PRECISION,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE partner_shop (
    id                 UUID PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    whatsapp_phone     VARCHAR(12)  NOT NULL UNIQUE,
    lat                DOUBLE PRECISION NOT NULL,
    lng                DOUBLE PRECISION NOT NULL,
    price_per_20l_kes  INTEGER      NOT NULL CHECK (price_per_20l_kes > 0),
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE subscription (
    id           UUID PRIMARY KEY,
    customer_id  UUID        NOT NULL REFERENCES customer (id),
    day_of_week  VARCHAR(9)  NOT NULL,          -- MONDAY ... SUNDAY
    time_of_day  TIME        NOT NULL,
    jerrycans    INTEGER     NOT NULL CHECK (jerrycans > 0),
    paused       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id               UUID PRIMARY KEY,
    customer_id      UUID        NOT NULL REFERENCES customer (id),
    shop_id          UUID        NOT NULL REFERENCES partner_shop (id),
    subscription_id  UUID        REFERENCES subscription (id),
    jerrycans        INTEGER     NOT NULL CHECK (jerrycans > 0),
    total_kes        INTEGER     NOT NULL CHECK (total_kes > 0),
    status           VARCHAR(20) NOT NULL,
    channel          VARCHAR(10) NOT NULL,      -- WHATSAPP or APP
    delivery_lat     DOUBLE PRECISION NOT NULL,
    delivery_lng     DOUBLE PRECISION NOT NULL,
    delivery_code    VARCHAR(4),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE payment (
    id                    UUID PRIMARY KEY,
    order_id              UUID        NOT NULL REFERENCES orders (id),
    checkout_request_id   VARCHAR(100) UNIQUE,  -- from the STK push request
    mpesa_receipt         VARCHAR(20)  UNIQUE,  -- unique = a duplicate callback can never pay twice
    amount_kes            INTEGER     NOT NULL CHECK (amount_kes > 0),
    status                VARCHAR(20) NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Points are a ledger: we only ever INSERT rows. Balance = SUM(change).
CREATE TABLE points_ledger (
    id           UUID PRIMARY KEY,
    customer_id  UUID         NOT NULL REFERENCES customer (id),
    order_id     UUID         REFERENCES orders (id),
    change       INTEGER      NOT NULL,
    reason       VARCHAR(50)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_points_customer ON points_ledger (customer_id);

-- Where each WhatsApp user is in the bot conversation (ROADMAP.md §6).
CREATE TABLE conversation (
    phone        VARCHAR(12) PRIMARY KEY,
    state        VARCHAR(30) NOT NULL,
    draft_order  JSONB,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
