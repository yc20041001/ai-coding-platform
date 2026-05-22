ALTER TABLE tool_execution_job
    ADD COLUMN error_code VARCHAR(64) NULL AFTER last_error,
    ADD COLUMN failure_stage VARCHAR(64) NULL AFTER error_code,
    ADD COLUMN next_retry_at DATETIME NULL AFTER failure_stage,
    ADD COLUMN dead_lettered_at DATETIME NULL AFTER next_retry_at,
    ADD COLUMN dead_letter_reason TEXT NULL AFTER dead_lettered_at,
    ADD COLUMN source_job_id BIGINT NULL AFTER dead_letter_reason,
    ADD INDEX idx_tool_job_next_retry(next_retry_at),
    ADD INDEX idx_tool_job_dead_lettered(dead_lettered_at),
    ADD INDEX idx_tool_job_error_code(error_code);
