-- V003__add_performance_indexes.sql
-- Indexes added after profiling the production query patterns rather than
-- guessed upfront; each one below maps to a specific slow query we saw.
--
-- Plain CREATE INDEX, not CONCURRENTLY: this table has zero rows and zero
-- traffic at the point this migration runs (it's created earlier in this
-- same Flyway session), so there's no live table to avoid locking. It also
-- sidesteps a real deadlock - CONCURRENTLY has to wait for every other open
-- transaction to finish, including Flyway's own advisory-lock transaction,
-- which never finishes until the whole migration run does.
-- Created: MES 4 Week 15 (Performance Optimization)

-- Composite index for common query pattern: user + status
CREATE INDEX idx_payments_user_status ON payments(user_id, status);
-- Index for time-based queries (recent payments)
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);
-- Index for merchant-based queries
CREATE INDEX idx_payments_merchant ON payments(merchant);
-- Index for correlation tracking
CREATE INDEX idx_payments_correlation_id ON payments(correlation_id);
-- Composite index for audit queries
CREATE INDEX idx_audit_log_payment_date ON payment_audit_log(payment_id, created_at DESC);
-- Index for event type queries
CREATE INDEX idx_payment_events_type_timestamp ON payment_events(event_type, event_timestamp DESC);
-- Index for status tracking in events
CREATE INDEX idx_payment_events_processing_status ON payment_events(processing_status, created_at);
-- Partial index for active/processing payments (smaller, faster)
CREATE INDEX idx_payments_active ON payments(user_id, status)
WHERE status IN ('PENDING', 'PROCESSING');
-- Partial index for recent failures (for debugging)
CREATE INDEX idx_payments_recent_failures ON payments(user_id, created_at DESC)
WHERE status = 'FAILED';
-- Add comment
COMMENT ON INDEX idx_payments_user_status IS 'Performance optimization: most common query pattern';
COMMENT ON INDEX idx_payments_active IS 'Partial index for active payments (smaller & faster)';
