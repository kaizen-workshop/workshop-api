CREATE TABLE workshop.workshop_group (
    id UUID PRIMARY KEY,
    workshop_id UUID NOT NULL UNIQUE REFERENCES workshop.workshop(id),
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO workshop.workshop_group (id, workshop_id, active, created_at, updated_at)
SELECT gen_random_uuid(), id, status = 'PUBLISHED', created_at, updated_at
FROM workshop.workshop;

CREATE TABLE workshop.chat_message (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES workshop.workshop_group(id),
    author_id UUID NOT NULL REFERENCES workshop.app_user(id),
    content TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CHECK (char_length(content) BETWEEN 1 AND 4000)
);

CREATE INDEX chat_message_group_cursor
    ON workshop.chat_message (group_id, sent_at DESC, id DESC);
