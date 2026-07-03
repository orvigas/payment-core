-- V001__initial_schema.sql
-- Baseline schema for the payment core system: the payments table plus an
-- append-only audit log of every status transition, needed for dispute
-- investigations and compliance requests.
-- Created: MES 1 (Weeks 1-2)

-- Postgres enum keeps invalid statuses out of the column entirely, instead of
-- relying on application code to enforce the allowed values.
CREATE TYPE payment_status AS ENUM (
  'PENDING',
  'PROCESSING',
  'COMPLETED',
  'FAILED',
  'REFUNDED',
  'CANCELLED'
);

-- Two identifiers by design: the bigserial `id` is the internal PK used for
-- joins and physical storage order, while `payment_id` is the UUID exposed to
-- clients and the JPA entity so external callers can never infer volume or
-- sequence from the value they're given.
CREATE TABLE payments (
  id BIGSERIAL PRIMARY KEY,
  payment_id VARCHAR(50) NOT NULL UNIQUE,
  user_id VARCHAR(100) NOT NULL,
  amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
  currency VARCHAR(3) NOT NULL DEFAULT 'MXN',
  merchant VARCHAR(100) NOT NULL,
  description TEXT,
  status payment_status NOT NULL DEFAULT 'PENDING',
  -- Correlation tracking
  correlation_id VARCHAR(50),
  -- Lets a retried create-payment request return the original payment instead
  -- of charging the user twice.
  idempotency_key VARCHAR(100),
  -- Timestamps
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP,
  CONSTRAINT unique_idempotency_key UNIQUE (idempotency_key)
);

-- Separate from payments because it's append-only and grows without bound;
-- keeping it out of the hot table avoids bloating the payments row size and
-- its indexes with history nobody queries on the normal read path.
CREATE TABLE payment_audit_log (
  id BIGSERIAL PRIMARY KEY,
  payment_id VARCHAR(50) NOT NULL,
  previous_status payment_status,
  new_status payment_status NOT NULL,
  changed_by VARCHAR(100),
  change_reason TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (payment_id) REFERENCES payments(payment_id)
);

-- Baseline indexes for the lookups the API needs on day one: fetch by
-- public id, list a user's payments, filter by status.
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_payment_id ON payments(payment_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_audit_payment_id ON payment_audit_log(payment_id);

-- Reserved for a future numeric/human-readable payment reference; not used
-- by the application yet, kept here so the sequence number stays stable
-- once something does start consuming it.
CREATE SEQUENCE payment_seq START 1000000;

-- Add comment to table
COMMENT ON TABLE payments IS 'Core payments table - all payment transactions';
COMMENT ON TABLE payment_audit_log IS 'Audit log for payment status changes';
-- Grant permissions (if using separate app user)
-- GRANT SELECT, INSERT, UPDATE ON payments TO app_user;
-- GRANT SELECT, INSERT ON payment_audit_log TO app_user;
-- Initial data (optional seed)
-- (No seed data for production)
