-- =====================================================
-- V4: 仓库与 Git 操作表
-- 对应 docs/database-design.md 第 6 节
-- =====================================================

-- 1. project_repository 项目仓库表
CREATE TABLE `project_repository` (
    `id`              BIGINT          NOT NULL,
    `project_id`      BIGINT          NOT NULL,
    `provider`        VARCHAR(32)     NOT NULL,
    `repo_full_name`  VARCHAR(255)    NOT NULL,
    `repo_url`        VARCHAR(512)    NOT NULL,
    `clone_url`       VARCHAR(512)    NOT NULL,
    `default_branch`  VARCHAR(128)    NULL,
    `local_path`      VARCHAR(512)    NULL,
    `status`          VARCHAR(32)     NOT NULL DEFAULT 'BOUND',
    `last_sync_time`  DATETIME(3)     NULL,
    `create_time`     DATETIME(3)     NOT NULL,
    `update_time`     DATETIME(3)     NOT NULL,
    `create_by`       BIGINT          NOT NULL,
    `update_by`       BIGINT          NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_repository` (`project_id`),
    INDEX `idx_project_repository_provider` (`provider`, `repo_full_name`),
    INDEX `idx_project_repository_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. repository_branch 仓库分支表
CREATE TABLE `repository_branch` (
    `id`                BIGINT          NOT NULL,
    `project_id`        BIGINT          NOT NULL,
    `repository_id`     BIGINT          NOT NULL,
    `branch_name`       VARCHAR(128)    NOT NULL,
    `commit_hash`       VARCHAR(128)    NULL,
    `protected_branch`  TINYINT         NOT NULL DEFAULT 0,
    `last_sync_time`    DATETIME(3)     NULL,
    `create_time`       DATETIME(3)     NOT NULL,
    `update_time`       DATETIME(3)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_repository_branch` (`repository_id`, `branch_name`),
    INDEX `idx_repository_branch_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. git_operation_log Git 操作日志表
CREATE TABLE `git_operation_log` (
    `id`              BIGINT          NOT NULL,
    `project_id`      BIGINT          NOT NULL,
    `repository_id`   BIGINT          NULL,
    `task_id`         BIGINT          NULL,
    `user_id`         BIGINT          NOT NULL,
    `operation_type`  VARCHAR(32)     NOT NULL,
    `branch`          VARCHAR(128)    NULL,
    `commit_hash`     VARCHAR(128)    NULL,
    `pr_url`          VARCHAR(512)    NULL,
    `status`          VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    `message`         TEXT            NULL,
    `error_message`   TEXT            NULL,
    `start_time`      DATETIME(3)     NULL,
    `end_time`        DATETIME(3)     NULL,
    `create_time`     DATETIME(3)     NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_git_log_project_time` (`project_id`, `create_time`),
    INDEX `idx_git_log_task` (`task_id`),
    INDEX `idx_git_log_user` (`user_id`),
    INDEX `idx_git_log_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
