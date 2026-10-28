CREATE TABLE workshop.app_user (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    username VARCHAR(60) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    weg_registration VARCHAR(50),
    phone VARCHAR(30),
    profile_image VARCHAR(500),
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    must_change_password BOOLEAN NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
