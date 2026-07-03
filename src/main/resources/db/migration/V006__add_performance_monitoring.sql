-- V006__add_performance_monitoring.sql
-- Long-term storage for performance data that Prometheus/Micrometer only
-- keep for their configured retention window; these tables are for
-- historical trend analysis (e.g. "did p95 latency regress after last
-- month's release") rather than live alerting. Not written to by the
-- current codebase - CustomMetrics reports to Micrometer only, not here.
-- Created: MES 4 Week 15-16 (Observability)

CREATE TABLE query_performance (
  id BIGSERIAL PRIMARY KEY,
  query_name VARCHAR(255) NOT NULL,
  query_hash VARCHAR(64) NOT NULL,
  execution_time_ms NUMERIC(10, 3) NOT NULL,
  rows_affected INT,
  executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(query_hash, executed_at)
);

CREATE TABLE endpoint_performance (
  id BIGSERIAL PRIMARY KEY,
  endpoint VARCHAR(255) NOT NULL,
  method VARCHAR(10) NOT NULL,
  -- GET, POST, etc
  status_code INT,
  response_time_ms NUMERIC(10, 3) NOT NULL,
  bytes_returned BIGINT,
  user_id VARCHAR(100),
  executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE system_metrics (
  id BIGSERIAL PRIMARY KEY,
  metric_name VARCHAR(100) NOT NULL,
  metric_value NUMERIC(15, 2) NOT NULL,
  unit VARCHAR(20),
  -- ms, bytes, requests, etc
  recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_query_perf_name ON query_performance(query_name);
CREATE INDEX idx_query_perf_time ON query_performance(executed_at DESC);
CREATE INDEX idx_endpoint_perf_endpoint ON endpoint_performance(endpoint);
CREATE INDEX idx_endpoint_perf_user ON endpoint_performance(user_id);
CREATE INDEX idx_endpoint_perf_time ON endpoint_performance(executed_at DESC);
CREATE INDEX idx_system_metrics_name ON system_metrics(metric_name);
CREATE INDEX idx_system_metrics_time ON system_metrics(recorded_at DESC);
-- Add comment
COMMENT ON TABLE query_performance IS 'Query performance tracking for optimization';
COMMENT ON TABLE endpoint_performance IS 'API endpoint response time tracking';
COMMENT ON TABLE system_metrics IS 'System-level metrics snapshots';
