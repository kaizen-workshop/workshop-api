ALTER TABLE workshop.workshop
    ADD COLUMN championship BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE workshop.payment (
    id UUID PRIMARY KEY,
    registration_id UUID NOT NULL UNIQUE REFERENCES workshop.registration(id),
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    status VARCHAR(20) NOT NULL,
    method VARCHAR(30) NOT NULL,
    external_reference VARCHAR(100) NOT NULL UNIQUE,
    idempotency_key UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (status IN ('PENDING', 'PAID', 'DECLINED', 'CANCELLED', 'REFUNDED', 'EXEMPT'))
);

CREATE TABLE workshop.payment_event (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES workshop.payment(id),
    previous_status VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    external_reference VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (status IN ('PENDING', 'PAID', 'DECLINED', 'CANCELLED', 'REFUNDED', 'EXEMPT'))
);

CREATE INDEX payment_event_payment_created_at ON workshop.payment_event (payment_id, created_at);
