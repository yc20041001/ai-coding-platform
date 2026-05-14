-- =====================================================
-- V3: 项目与项目成员表
-- 对应 docs/database-design.md 第 5 节
-- =====================================================

-- 1. project 项目表
CREATE TABLE `project` (
    `id`            BIGINT          NOT NULL,
    `name`          VARCHAR(128)    NOT NULL,
    `description`   TEXT            NULL,
    `icon`          VARCHAR(512)    NULL,
    `owner_id`      BIGINT          NOT NULL,
    `repo_url`      VARCHAR(512)    NULL,
    `tech_stack`    VARCHAR(512)    NULL,
    `status`        VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    `create_time`   DATETIME(3)     NOT NULL,
    `update_time`   DATETIME(3)     NOT NULL,
    `create_by`     BIGINT          NOT NULL,
    `update_by`     BIGINT          NULL,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `version`       INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_project_owner` (`owner_id`),
    INDEX `idx_project_status` (`status`),
    INDEX `idx_project_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. project_member 项目成员表
CREATE TABLE `project_member` (
    `id`            BIGINT          NOT NULL,
    `project_id`    BIGINT          NOT NULL,
    `user_id`       BIGINT          NOT NULL,
    `role`          VARCHAR(32)     NOT NULL,
    `status`        VARCHAR(32)     NOT NULL DEFAULT 'ACTIVE',
    `joined_time`   DATETIME(3)     NOT NULL,
    `create_time`   DATETIME(3)     NOT NULL,
    `update_time`   DATETIME(3)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_member` (`project_id`, `user_id`),
    INDEX `idx_project_member_user` (`user_id`),
    INDEX `idx_project_member_role` (`project_id`, `role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. project_invitation 项目邀请表
CREATE TABLE `project_invitation` (
    `id`                BIGINT          NOT NULL,
    `project_id`        BIGINT          NOT NULL,
    `email`             VARCHAR(128)    NOT NULL,
    `invitee_user_id`   BIGINT          NULL,
    `inviter_id`        BIGINT          NOT NULL,
    `role`              VARCHAR(32)     NOT NULL,
    `token`             VARCHAR(128)    NOT NULL,
    `status`            VARCHAR(32)     NOT NULL DEFAULT 'PENDING',
    `expire_time`       DATETIME(3)     NOT NULL,
    `create_time`       DATETIME(3)     NOT NULL,
    `update_time`       DATETIME(3)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_invitation_token` (`token`),
    INDEX `idx_project_invitation_project` (`project_id`, `status`),
    INDEX `idx_project_invitation_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. project_config 项目配置表
CREATE TABLE `project_config` (
    `id`            BIGINT          NOT NULL,
    `project_id`    BIGINT          NOT NULL,
    `config_key`    VARCHAR(128)    NOT NULL,
    `config_value`  JSON            NOT NULL,
    `description`   VARCHAR(255)    NULL,
    `create_time`   DATETIME(3)     NOT NULL,
    `update_time`   DATETIME(3)     NOT NULL,
    `create_by`     BIGINT          NULL,
    `update_by`     BIGINT          NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_config_key` (`project_id`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
