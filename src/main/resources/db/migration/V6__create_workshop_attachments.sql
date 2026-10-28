CREATE TABLE workshop.workshop_attachment (
    id UUID PRIMARY KEY,
    workshop_id UUID NOT NULL REFERENCES workshop.workshop(id),
    type VARCHAR(20) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    extension VARCHAR(10) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    checksum_sha256 VARCHAR(64) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (type IN ('IMAGE', 'ATTACHMENT')),
    CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp')),
    CHECK (extension IN ('jpg', 'jpeg', 'png', 'webp')),
    CHECK (checksum_sha256 ~ '^[0-9a-f]{64}$')
);

CREATE UNIQUE INDEX workshop_attachment_one_image
    ON workshop.workshop_attachment (workshop_id)
    WHERE type = 'IMAGE';

CREATE INDEX workshop_attachment_workshop_type
    ON workshop.workshop_attachment (workshop_id, type, created_at);
