-- Removes data created by the E2E audit run today and restores demo users changed by it.
BEGIN;
SET search_path TO workshop;

CREATE TEMP TABLE e2e_users AS SELECT id FROM app_user WHERE username LIKE 'e2e.arweg.%';
CREATE TEMP TABLE e2e_ws AS SELECT id FROM workshop
  WHERE title ~ '^(E2E |free com preço|pix preço zero|datas invertidas|preço negativo)' AND created_at >= CURRENT_DATE;
CREATE TEMP TABLE e2e_posts AS SELECT id FROM post
  WHERE (title ~ '^(Post E2E|Agendado |Diag )' OR title = 'Diag feed' OR workshop_id IN (SELECT id FROM e2e_ws))
    AND created_at >= CURRENT_DATE;
CREATE TEMP TABLE e2e_regs AS SELECT id FROM registration WHERE workshop_id IN (SELECT id FROM e2e_ws);
CREATE TEMP TABLE e2e_pays AS SELECT id FROM payment WHERE registration_id IN (SELECT id FROM e2e_regs);
CREATE TEMP TABLE e2e_groups AS SELECT id FROM workshop_group WHERE workshop_id IN (SELECT id FROM e2e_ws);

DELETE FROM payment_event WHERE payment_id IN (SELECT id FROM e2e_pays);
DELETE FROM payment WHERE id IN (SELECT id FROM e2e_pays);
DELETE FROM registration WHERE id IN (SELECT id FROM e2e_regs);
DELETE FROM chat_message WHERE group_id IN (SELECT id FROM e2e_groups);
DELETE FROM workshop_group WHERE id IN (SELECT id FROM e2e_groups);
DELETE FROM workshop_attachment WHERE workshop_id IN (SELECT id FROM e2e_ws);
DELETE FROM evaluation WHERE workshop_id IN (SELECT id FROM e2e_ws);
DELETE FROM post WHERE id IN (SELECT id FROM e2e_posts);  -- likes/comments cascade
DELETE FROM workshop WHERE id IN (SELECT id FROM e2e_ws);

DELETE FROM notification WHERE created_at >= CURRENT_DATE AND (
  title = 'Aviso E2E'
  OR EXISTS (SELECT 1 FROM e2e_ws w WHERE notification.data_json LIKE '%' || w.id::text || '%'));
DELETE FROM notification_device WHERE created_at >= CURRENT_DATE AND id IN (
  SELECT id FROM notification_device WHERE user_id IN (SELECT id FROM app_user WHERE username LIKE 'demo.%'));
DELETE FROM administrative_audit WHERE occurred_at >= CURRENT_DATE AND (
  entity_id IN (SELECT id FROM e2e_ws) OR entity_id IN (SELECT id FROM e2e_posts)
  OR entity_id IN (SELECT id FROM e2e_users) OR user_id IN (SELECT id FROM e2e_users)
  OR entity_id IN (SELECT id FROM theme WHERE name LIKE 'Tema E2E %')
  OR entity_id IN (SELECT id FROM category WHERE name LIKE 'Cat E2E %'));

DELETE FROM refresh_token WHERE user_id IN (SELECT id FROM e2e_users);
DELETE FROM password_reset_token WHERE user_id IN (SELECT id FROM e2e_users);
DELETE FROM user_theme WHERE user_id IN (SELECT id FROM e2e_users);
DELETE FROM notification WHERE user_id IN (SELECT id FROM e2e_users);
DELETE FROM app_user WHERE id IN (SELECT id FROM e2e_users);
DELETE FROM theme WHERE name LIKE 'Tema E2E %';
DELETE FROM category WHERE name LIKE 'Cat E2E %';

-- Restore demo.ana profile and interests to the seed values.
UPDATE app_user SET phone = '+55 47 99999-2001' WHERE username = 'demo.ana';
DELETE FROM user_theme WHERE user_id = '30000000-0000-0000-0000-000000000002';
INSERT INTO user_theme (user_id, theme_id) VALUES
  ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002'),
  ('30000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000005');

SELECT (SELECT count(*) FROM workshop WHERE title LIKE 'E2E%') AS ws_left,
       (SELECT count(*) FROM post WHERE title LIKE 'Post E2E%' OR title LIKE 'Diag%') AS posts_left,
       (SELECT count(*) FROM app_user WHERE username LIKE 'e2e.%') AS users_left,
       (SELECT count(*) FROM workshop) AS ws_total, (SELECT count(*) FROM post) AS posts_total;
COMMIT;
