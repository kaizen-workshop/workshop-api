CREATE TABLE workshop.evaluation (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES workshop.app_user(id),
    workshop_id UUID NOT NULL REFERENCES workshop.workshop(id),
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    content_rating SMALLINT NOT NULL CHECK (content_rating BETWEEN 1 AND 5),
    instructor_rating SMALLINT NOT NULL CHECK (instructor_rating BETWEEN 1 AND 5),
    organization_rating SMALLINT NOT NULL CHECK (organization_rating BETWEEN 1 AND 5),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT evaluation_user_workshop_unique UNIQUE (user_id, workshop_id)
);

CREATE INDEX evaluation_workshop_created_at ON workshop.evaluation (workshop_id, created_at DESC);
