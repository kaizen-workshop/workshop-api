CREATE TABLE workshop.registration (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES workshop.app_user(id),
    workshop_id UUID NOT NULL REFERENCES workshop.workshop(id),
    status VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (status IN ('PENDING', 'CONFIRMED', 'WAITING_LIST', 'CANCELLED', 'REFUNDED')),
    CHECK (payment_status IN ('PENDING', 'PAID', 'DECLINED', 'CANCELLED', 'REFUNDED', 'EXEMPT')),
    CHECK ((status = 'CANCELLED' AND cancelled_at IS NOT NULL) OR (status <> 'CANCELLED' AND cancelled_at IS NULL))
);

CREATE UNIQUE INDEX registration_one_valid_per_user_workshop
    ON workshop.registration (user_id, workshop_id)
    WHERE status IN ('PENDING', 'CONFIRMED', 'WAITING_LIST');

CREATE INDEX registration_workshop_status_order
    ON workshop.registration (workshop_id, status, registered_at, id);
