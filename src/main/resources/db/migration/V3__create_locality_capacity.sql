CREATE TABLE IF NOT EXISTS locality_capacity (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    locality VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL,
    CONSTRAINT uq_locality_capacity UNIQUE (event_id, locality)
);
