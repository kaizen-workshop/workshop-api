-- Gives the demo ADMIN account the interests the other demo accounts have, so the first
-- login goes straight to the app instead of the theme-selection onboarding.

INSERT INTO workshop.user_theme (user_id, theme_id)
VALUES
    ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001'),
    ('30000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000002')
ON CONFLICT DO NOTHING;
