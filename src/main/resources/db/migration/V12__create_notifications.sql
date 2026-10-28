CREATE TABLE workshop.notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES workshop.app_user(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    data_json TEXT NOT NULL,
    read_at TIMESTAMPTZ,
    scheduled_at TIMESTAMPTZ NOT NULL,
    delivered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX notification_user_created_at
    ON workshop.notification (user_id, created_at DESC, id DESC);
CREATE INDEX notification_scheduled_delivery
    ON workshop.notification (delivered_at, scheduled_at)
    WHERE delivered_at IS NULL;

CREATE TABLE workshop.notification_device (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES workshop.app_user(id),
    token VARCHAR(500) NOT NULL UNIQUE,
    platform VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (platform IN ('ANDROID', 'IOS'))
);

CREATE INDEX notification_device_user_active
    ON workshop.notification_device (user_id, active);
