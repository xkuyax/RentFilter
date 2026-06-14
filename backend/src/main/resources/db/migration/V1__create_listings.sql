CREATE TABLE listings (
    id          BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255),
    source      VARCHAR(20)   NOT NULL,
    title       VARCHAR(512)  NOT NULL,
    description TEXT,
    price       NUMERIC(10, 2),
    rooms       REAL,
    area_sqm    REAL,
    address     VARCHAR(512)  NOT NULL,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    url         VARCHAR(1024) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
