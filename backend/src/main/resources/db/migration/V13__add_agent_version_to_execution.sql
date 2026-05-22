-- V13__add_agent_version_to_execution.sql
-- Track the exact Agent version used by each execution.

SET @agent_execution_version_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'agent_execution'
    AND COLUMN_NAME = 'agent_version_id'
);

SET @agent_execution_version_column_sql = IF(
  @agent_execution_version_column_exists = 0,
  'ALTER TABLE agent_execution ADD COLUMN agent_version_id BIGINT NULL COMMENT ''Agent version ID used for this execution'' AFTER agent_id',
  'SELECT 1'
);
PREPARE agent_execution_version_column_stmt FROM @agent_execution_version_column_sql;
EXECUTE agent_execution_version_column_stmt;
DEALLOCATE PREPARE agent_execution_version_column_stmt;

SET @agent_execution_version_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'agent_execution'
    AND INDEX_NAME = 'idx_agent_execution_agent_version'
);

SET @agent_execution_version_index_sql = IF(
  @agent_execution_version_index_exists = 0,
  'CREATE INDEX idx_agent_execution_agent_version ON agent_execution (agent_version_id)',
  'SELECT 1'
);
PREPARE agent_execution_version_index_stmt FROM @agent_execution_version_index_sql;
EXECUTE agent_execution_version_index_stmt;
DEALLOCATE PREPARE agent_execution_version_index_stmt;
