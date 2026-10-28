ALTER TABLE workshop.registration
    ADD COLUMN idempotency_key UUID;

UPDATE workshop.registration
SET idempotency_key = id
WHERE idempotency_key IS NULL;

ALTER TABLE workshop.registration
    ALTER COLUMN idempotency_key SET NOT NULL;

CREATE UNIQUE INDEX registration_user_idempotency_key
    ON workshop.registration (user_id, idempotency_key);
