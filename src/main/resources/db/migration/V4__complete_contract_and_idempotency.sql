ALTER TABLE reservations ADD COLUMN IF NOT EXISTS total NUMERIC(12,2);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS expira_en TIMESTAMP WITH TIME ZONE;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS idx_reservations_idempotency_key ON reservations (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_reservations_expira ON reservations (expira_en, status);
