ALTER TABLE reservations ADD COLUMN IF NOT EXISTS user_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_reservations_user_id ON reservations (user_id);
