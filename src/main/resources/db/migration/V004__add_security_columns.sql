-- V004__add_security_columns.sql
-- Adds the fields needed to answer "who did this and from where" once JWT
-- auth landed - for fraud investigation and incident response, not read by
-- normal application logic. None of this migration's tables/columns are
-- populated by the current codebase yet; they're provisioned ahead of that
-- work so the schema doesn't need another migration when it lands.
-- Created: MES 4 Week 14 (Security)

-- created_by/updated_by/jwt_subject are deliberately separate from user_id:
-- user_id is who the payment is for, these columns are who was authenticated
-- when the write happened, which can differ (e.g. an admin acting on a
-- customer's behalf).
ALTER TABLE payments
ADD COLUMN created_by VARCHAR(100),
  -- User ID who created payment
ADD COLUMN updated_by VARCHAR(100),
  -- User ID who last updated
ADD COLUMN api_key_id VARCHAR(50),
  -- Which API key was used
ADD COLUMN jwt_subject VARCHAR(100),
  -- JWT subject claim
ADD COLUMN ip_address INET,
  -- Client IP address
ADD COLUMN user_agent TEXT;
-- Client user agent

-- Every rejected auth attempt, so repeated failures from one IP or user can
-- be spotted and rate-limited or blocked before they succeed.
CREATE TABLE auth_failures (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(100),
  api_key_id VARCHAR(50),
  failure_type VARCHAR(50),
  -- INVALID_TOKEN, RATE_LIMITED, etc
  ip_address INET,
  attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  details JSONB
);

-- Stores only the hash of the key, never the key itself, so a database leak
-- doesn't hand over usable credentials.
CREATE TABLE api_keys (
  id BIGSERIAL PRIMARY KEY,
  api_key_id VARCHAR(50) NOT NULL UNIQUE,
  user_id VARCHAR(100) NOT NULL,
  key_hash VARCHAR(255) NOT NULL,
  -- Hashed API key
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP,
  revoked_at TIMESTAMP,
  last_used_at TIMESTAMP,
  CONSTRAINT valid_key_hash CHECK (char_length(key_hash) >= 64)
);

-- Create indexes for security
CREATE INDEX idx_auth_failures_user ON auth_failures(user_id);
CREATE INDEX idx_auth_failures_timestamp ON auth_failures(attempted_at DESC);
CREATE INDEX idx_payments_created_by ON payments(created_by);
CREATE INDEX idx_api_keys_user ON api_keys(user_id);
CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);
-- Add comment
COMMENT ON TABLE auth_failures IS 'Track failed authentication attempts for security monitoring';
COMMENT ON TABLE api_keys IS 'API key management for programmatic access';

-- Audit trail entries need the same request context as the payment they
-- describe, otherwise a status change can't be tied back to who triggered it.
ALTER TABLE payment_audit_log
ADD COLUMN ip_address INET,
  ADD COLUMN user_agent TEXT;
