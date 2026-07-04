-- V008__add_payments_user_fk.sql
-- payments.user_id predates the users table (V001 vs V007) and was never tied to it, so a
-- payment could be persisted with a user_id that doesn't correspond to any real account. Now
-- that payment ownership is always derived from the authenticated caller, every payment's
-- owner is guaranteed to be a real user, so the constraint can be enforced at the database level
-- as a second line of defense.

ALTER TABLE payments
  ADD CONSTRAINT fk_payments_user_id FOREIGN KEY (user_id) REFERENCES users(user_id);
