CREATE TABLE workshop.workshop (
    id UUID PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    image VARCHAR(500),
    theme_id UUID NOT NULL REFERENCES workshop.theme(id),
    category_id UUID NOT NULL REFERENCES workshop.category(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location VARCHAR(250) NOT NULL,
    modality VARCHAR(30) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    registration_start TIMESTAMPTZ NOT NULL,
    registration_end TIMESTAMPTZ NOT NULL,
    maximum_participants INTEGER NOT NULL CHECK (maximum_participants > 0),
    payment_method VARCHAR(30) NOT NULL,
    additional_information TEXT,
    status VARCHAR(20) NOT NULL,
    scheduled_publish_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    created_by UUID NOT NULL REFERENCES workshop.app_user(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (end_date >= start_date),
    CHECK (price >= 0),
    CHECK (registration_end > registration_start)
);

CREATE INDEX workshop_idx_workshop_catalogue ON workshop.workshop (status, start_date);
CREATE INDEX workshop_idx_workshop_theme ON workshop.workshop (theme_id);
CREATE INDEX workshop_idx_workshop_category ON workshop.workshop (category_id);
