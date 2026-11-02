ALTER TABLE reservations ALTER COLUMN event_id TYPE VARCHAR(64) USING event_id::text;
ALTER TABLE reservations RENAME COLUMN section_id TO locality_id;
ALTER TABLE reservations ALTER COLUMN locality_id TYPE VARCHAR(64);

DROP INDEX IF EXISTS idx_reservations_event_section;
CREATE INDEX IF NOT EXISTS idx_reservations_event_locality ON reservations (event_id, locality_id);
