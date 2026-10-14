CREATE TABLE IF NOT EXISTS reservation_incidents (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT,
    locality VARCHAR(255),
    quantity INTEGER,
    reason VARCHAR(50) NOT NULL,
    detail VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
