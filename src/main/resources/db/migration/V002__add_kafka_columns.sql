-- V002__add_kafka_columns.sql
-- Tracks the Kafka side of the payment lifecycle so we can answer "did this
-- event actually get published, and did a consumer process it" without
-- grepping broker logs.
-- Created: MES 2 (Weeks 5-8)

-- Lets us tell "payment created but event never published" apart from
-- "event published but processing never finished" when debugging a stuck
-- payment.
ALTER TABLE payments
ADD COLUMN kafka_topic_published VARCHAR(50),
  ADD COLUMN kafka_partition INT,
  ADD COLUMN kafka_offset BIGINT,
  ADD COLUMN event_published_at TIMESTAMP,
  ADD COLUMN processing_started_at TIMESTAMP,
  ADD COLUMN processing_completed_at TIMESTAMP;

-- One row per event actually emitted (initiated/charged/completed/failed),
-- independent of the payment's current status column, so we keep full
-- history even after the payment itself moves to a terminal state. Not yet
-- written to by PaymentProducer - provisioned ahead of that integration.
CREATE TABLE payment_events (
  id BIGSERIAL PRIMARY KEY,
  payment_id VARCHAR(50) NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  -- INITIATED, CHARGED, COMPLETED, FAILED
  event_timestamp TIMESTAMP NOT NULL,
  kafka_topic VARCHAR(50),
  kafka_partition INT,
  kafka_offset BIGINT,
  -- Raw event body kept as JSONB (not a fixed set of columns) so new event
  -- fields don't require a migration every time the event schema evolves.
  event_payload JSONB,
  processing_status VARCHAR(20),
  -- PENDING, PROCESSED, FAILED
  error_message TEXT,
  retry_count INT DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (payment_id) REFERENCES payments(payment_id)
);

-- Periodic snapshots of consumer lag per group/topic/partition, intended to
-- alert before a slow consumer causes payment notifications or analytics to
-- fall behind real time. No consumer writes here yet - table is reserved for
-- that monitoring job.
CREATE TABLE consumer_lag (
  id BIGSERIAL PRIMARY KEY,
  consumer_group VARCHAR(100) NOT NULL,
  topic VARCHAR(50) NOT NULL,
  partition INT NOT NULL,
  lag BIGINT NOT NULL,
  measured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(consumer_group, topic, partition)
);

-- Create indexes for event tracking
CREATE INDEX idx_payment_events_payment_id ON payment_events(payment_id);
CREATE INDEX idx_payment_events_type ON payment_events(event_type);
CREATE INDEX idx_payment_events_timestamp ON payment_events(event_timestamp DESC);
CREATE INDEX idx_consumer_lag_group ON consumer_lag(consumer_group);
-- Add comment
COMMENT ON TABLE payment_events IS 'Event tracking for Kafka-based async processing';
COMMENT ON TABLE consumer_lag IS 'Consumer lag monitoring for Kafka consumers';

-- Support the "payments stuck mid-processing" dashboard query: filter by
-- status and how long ago processing started.
CREATE INDEX idx_payments_event_published_at ON payments(event_published_at);
CREATE INDEX idx_payments_processing_status ON payments(status, processing_started_at);
