-- seed-wave-test-users.sql
-- Creates the 100 accounts scenarios/wave-test.js logs in as, one per VU
-- (wave_test_user_1 .. wave_test_user_100). There's no registration endpoint,
-- and login checks real credentials, so these have to exist before the test
-- runs. Unlike seed-load-test-user.sql (a single shared account), wave-test.js
-- logs in individually per user, so each VU needs its own row.
--
-- Password (all 100 accounts): LoadTest123!
-- Hash generated with the app's own BCryptPasswordEncoder (strength 10) -
-- this is a throwaway local fixture, not a real credential. Reusing one hash
-- across all 100 rows is fine here: they're disposable test accounts sharing
-- one known password on purpose, not distinct secrets.
--
-- Re-run this after every app restart with app.database.reset-on-startup
-- enabled (the default), since Flyway's clean step wipes the users table.
--
-- Usage: psql -h localhost -U postgres -d payment_db -f seed-wave-test-users.sql

INSERT INTO users (user_id, username, email, password_hash, is_active)
SELECT
  'wave_test_user_' || i,
  'wave_test_user_' || i,
  'wave_test_user_' || i || '@example.com',
  '$2a$10$CFuiTEx8q13zNmU3Z04iL.Efao/J3FoYlGplurCUfrBaTWVSJC0lO',
  TRUE
FROM generate_series(1, 100) AS i
ON CONFLICT (username) DO NOTHING;

-- user_roles has no unique constraint (Hibernate's default @ElementCollection
-- mapping), so re-running this needs an explicit existence check instead of
-- ON CONFLICT to stay idempotent.
INSERT INTO user_roles (owner_id, role)
SELECT u.id, 'USER' FROM users u
WHERE u.username LIKE 'wave_test_user_%'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.owner_id = u.id AND ur.role = 'USER'
  );
