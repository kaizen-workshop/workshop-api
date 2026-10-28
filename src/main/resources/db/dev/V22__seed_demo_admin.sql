-- Development/demo ADMIN account. Only ADMIN can confirm or decline simulated payments
-- (PATCH /payments/{id}/simulate/*), so without it a paid registration stays PENDING.
-- Same password as the other demo accounts: Workshop@2026!

INSERT INTO workshop.app_user (
    id, name, username, email, weg_registration, phone, profile_image,
    password_hash, role, status, must_change_password, last_login_at,
    created_at, updated_at, token_version
)
VALUES
    (
        '30000000-0000-0000-0000-000000000004', 'Daniel Admin', 'demo.admin',
        'admin.demo@workshop.local', 'ADM-3001', '+55 47 99999-3001', NULL,
        '$2a$10$ofg5AOaIvBDlG7c295POcu2MjYkFRQ1a/L6gad52HMfNZi6TVDdGe',
        'ADMIN', 'ACTIVE', FALSE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
    )
ON CONFLICT DO NOTHING;
