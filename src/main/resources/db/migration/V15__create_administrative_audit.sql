CREATE TABLE workshop.administrative_audit (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES workshop.app_user(id),
    action VARCHAR(80) NOT NULL,
    entity VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    previous_value TEXT,
    new_value TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    ip VARCHAR(45)
);

CREATE INDEX administrative_audit_occurred_at_idx ON workshop.administrative_audit (occurred_at DESC, id DESC);
CREATE INDEX administrative_audit_entity_idx ON workshop.administrative_audit (entity, entity_id, occurred_at DESC);
CREATE INDEX administrative_audit_user_idx ON workshop.administrative_audit (user_id, occurred_at DESC);
