-- V005__add_rate_limiting_tables.sql
-- Schema for a database-backed rate limiter that would survive an app
-- restart and be shared across instances, unlike Resilience4j's current
-- in-memory limiter (see RateLimitingConfig) which resets per instance.
-- Not wired into the application yet - these tables exist for that future
-- move to a shared limiter.
-- Created: MES 4 Week 14 (Rate Limiting)

-- Current window's request count per user/endpoint. One row is
-- read-and-incremented on every request, so this stays small and narrow by
-- design - anything not needed to answer "is this call still under the
-- limit" belongs in rate_limit_violations instead.
CREATE TABLE rate_limit_tracking (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(100) NOT NULL,
  endpoint VARCHAR(255) NOT NULL,
  requests_in_period INT DEFAULT 0,
  limit_reset_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, endpoint)
);

-- Separate append-only log of exceeded limits, kept out of the hot
-- rate_limit_tracking table so abuse investigations don't compete with the
-- request path for locks on that table.
CREATE TABLE rate_limit_violations (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(100) NOT NULL,
  endpoint VARCHAR(255) NOT NULL,
  attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip_address INET,
  user_agent TEXT
);

-- Create indexes
CREATE INDEX idx_rate_limit_user ON rate_limit_tracking(user_id);
CREATE INDEX idx_rate_limit_violations_user ON rate_limit_violations(user_id);
CREATE INDEX idx_rate_limit_violations_timestamp ON rate_limit_violations(attempted_at DESC);
-- Add comment
COMMENT ON TABLE rate_limit_tracking IS 'Current rate limit state for each user/endpoint';
COMMENT ON TABLE rate_limit_violations IS 'Audit log of rate limit violations';
