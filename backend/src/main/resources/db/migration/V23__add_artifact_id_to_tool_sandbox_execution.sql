-- V23: Add artifact_id to tool_sandbox_execution for Patch Proposal Artifact
ALTER TABLE tool_sandbox_execution
    ADD COLUMN artifact_id BIGINT NULL AFTER error_message,
    ADD INDEX idx_tool_sandbox_artifact (artifact_id);
