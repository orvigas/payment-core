-- seed-load-test-user.sql
-- Creates the account payment-load-test.js logs in as. There's no
-- registration endpoint, and login now checks real credentials against the
-- users table, so this has to be inserted directly before running the test.
--
-- Password: LoadTest123!
-- Hash generated with the app's own BCryptPasswordEncoder (strength 10) -
-- this is a throwaway local fixture, not a real credential.
--
-- Re-run this after every app restart with app.database.reset-on-startup
-- enabled (the default), since Flyway's clean step wipes the users table.
--
-- Usage: psql -h localhost -U postgres -d payment_db -f seed-load-test-user.sql

INSERT INTO users (user_id, username, email, password_hash, is_active)
VALUES (
  'load_test_user',
  'load_test_user',
  'load_test_user@example.com',
  '$2a$10$CFuiTEx8q13zNmU3Z04iL.Efao/J3FoYlGplurCUfrBaTWVSJC0lO',
  TRUE
)
ON CONFLICT (username) DO NOTHING;

-- user_roles has no unique constraint (Hibernate's default @ElementCollection
-- mapping), so re-running this needs an explicit existence check instead of
-- ON CONFLICT to stay idempotent.
INSERT INTO user_roles (owner_id, role)
SELECT u.id, 'USER' FROM users u
WHERE u.username = 'load_test_user'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.owner_id = u.id AND ur.role = 'USER'
  );
