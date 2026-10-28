ALTER TABLE workshop.registration ADD COLUMN cancellation_idempotency_key UUID;
CREATE UNIQUE INDEX registration_cancellation_idempotency_idx
    ON workshop.registration (user_id, cancellation_idempotency_key)
    WHERE cancellation_idempotency_key IS NOT NULL;

ALTER TABLE workshop.notification ADD COLUMN updated_at TIMESTAMPTZ;
UPDATE workshop.notification SET updated_at = GREATEST(created_at, read_at, delivered_at);
ALTER TABLE workshop.notification ALTER COLUMN updated_at SET NOT NULL;
CREATE INDEX notification_user_updated_at
    ON workshop.notification (user_id, updated_at DESC, id DESC);
