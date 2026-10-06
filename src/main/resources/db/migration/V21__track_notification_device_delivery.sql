CREATE TABLE workshop.notification_device_delivery (
    notification_id UUID NOT NULL REFERENCES workshop.notification(id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES workshop.notification_device(id) ON DELETE CASCADE,
    delivered_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (notification_id, device_id)
);

CREATE INDEX notification_device_delivery_device
    ON workshop.notification_device_delivery (device_id);
