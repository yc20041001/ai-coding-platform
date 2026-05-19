-- Test database schema sync: V10 production hardening fields for model_request_log
-- Applied via spring.sql.init since flyway is disabled in test profile
-- continue-on-error: true handles the case where columns already exist

ALTER TABLE model_request_log
  ADD COLUMN fallback_used TINYINT NOT NULL DEFAULT 0;

ALTER TABLE model_request_log
  ADD COLUMN error_code VARCHAR(64) NULL;

ALTER TABLE model_request_log
  ADD COLUMN estimated_cost DECIMAL(12, 8) NULL;

-- Test database schema sync: V13 execution Agent version tracking
ALTER TABLE agent_execution
  ADD COLUMN agent_version_id BIGINT NULL;

CREATE INDEX idx_agent_execution_agent_version
  ON agent_execution (agent_version_id);
