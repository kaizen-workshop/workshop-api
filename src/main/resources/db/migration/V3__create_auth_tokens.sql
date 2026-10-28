CREATE TABLE workshop.refresh_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES workshop.app_user(id),
    token_hash VARCHAR(100) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE workshop.password_reset_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES workshop.app_user(id),
    token_hash VARCHAR(100) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
