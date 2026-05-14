-- =====================================================
-- V1: 用户认证与权限基础表
-- 对应 docs/database-design.md 第 4 节
-- =====================================================

-- 1. user 用户表
CREATE TABLE `user` (
    `id`            BIGINT          NOT NULL,
    `username`      VARCHAR(64)     NOT NULL,
    `email`         VARCHAR(128)    NOT NULL,
    `password`      VARCHAR(255)    NULL,
    `avatar`        VARCHAR(512)    NULL,
    `phone`         VARCHAR(32)     NULL,
    `status`        VARCHAR(32)     NOT NULL DEFAULT 'ENABLED',
    `github_id`     VARCHAR(64)     NULL,
    `github_login`  VARCHAR(128)    NULL,
    `token_usage`   BIGINT          NOT NULL DEFAULT 0,
    `last_login_time` DATETIME(3)   NULL,
    `create_time`   DATETIME(3)     NOT NULL,
    `update_time`   DATETIME(3)     NOT NULL,
    `create_by`     BIGINT          NULL,
    `update_by`     BIGINT          NULL,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    `version`       INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_email` (`email`),
    UNIQUE INDEX `uk_user_username` (`username`),
    INDEX `idx_user_github_id` (`github_id`),
    INDEX `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. role 角色表
CREATE TABLE `role` (
    `id`            BIGINT          NOT NULL,
    `code`          VARCHAR(64)     NOT NULL,
    `name`          VARCHAR(64)     NOT NULL,
    `description`   VARCHAR(255)    NULL,
    `status`        VARCHAR(32)     NOT NULL DEFAULT 'ENABLED',
    `create_time`   DATETIME(3)     NOT NULL,
    `update_time`   DATETIME(3)     NOT NULL,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. permission 权限表
CREATE TABLE `permission` (
    `id`            BIGINT          NOT NULL,
    `code`          VARCHAR(128)    NOT NULL,
    `name`          VARCHAR(128)    NOT NULL,
    `type`          VARCHAR(32)     NOT NULL,
    `resource`      VARCHAR(255)    NULL,
    `method`        VARCHAR(16)     NULL,
    `parent_id`     BIGINT          NULL,
    `sort_order`    INT             NOT NULL DEFAULT 0,
    `status`        VARCHAR(32)     NOT NULL DEFAULT 'ENABLED',
    `create_time`   DATETIME(3)     NOT NULL,
    `update_time`   DATETIME(3)     NOT NULL,
    `deleted`       TINYINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_permission_code` (`code`),
    INDEX `idx_permission_parent` (`parent_id`),
    INDEX `idx_permission_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. user_role 用户角色关系表
CREATE TABLE `user_role` (
    `id`            BIGINT          NOT NULL,
    `user_id`       BIGINT          NOT NULL,
    `role_id`       BIGINT          NOT NULL,
    `create_time`   DATETIME(3)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_user_role_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5. role_permission 角色权限关系表
CREATE TABLE `role_permission` (
    `id`             BIGINT         NOT NULL,
    `role_id`        BIGINT         NOT NULL,
    `permission_id`  BIGINT         NOT NULL,
    `create_time`    DATETIME(3)    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_role_permission` (`role_id`, `permission_id`),
    INDEX `idx_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6. github_account GitHub 账号表
CREATE TABLE `github_account` (
    `id`                BIGINT          NOT NULL,
    `user_id`           BIGINT          NOT NULL,
    `github_id`         VARCHAR(64)     NOT NULL,
    `login`             VARCHAR(128)    NOT NULL,
    `avatar_url`        VARCHAR(512)    NULL,
    `access_token_enc`  TEXT            NOT NULL,
    `scope`             VARCHAR(512)    NULL,
    `status`            VARCHAR(32)     NOT NULL DEFAULT 'BOUND',
    `bind_time`         DATETIME(3)     NOT NULL,
    `create_time`       DATETIME(3)     NOT NULL,
    `update_time`       DATETIME(3)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_github_account_user` (`user_id`),
    UNIQUE INDEX `uk_github_account_github_id` (`github_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =====================================================
-- 初始数据
-- =====================================================

-- 初始化角色
INSERT INTO `role` (`id`, `code`, `name`, `description`, `status`, `create_time`, `update_time`) VALUES
(1, 'ADMIN', '平台管理员', 'Platform Administrator', 'ENABLED', NOW(3), NOW(3)),
(2, 'USER',  '普通用户',   'Normal User',              'ENABLED', NOW(3), NOW(3));

-- 初始化权限
INSERT INTO `permission` (`id`, `code`, `name`, `type`, `status`, `sort_order`, `create_time`, `update_time`) VALUES
(1, 'project:create',    '创建项目',     'ACTION', 'ENABLED', 1, NOW(3), NOW(3)),
(2, 'project:view',      '查看项目',     'ACTION', 'ENABLED', 2, NOW(3), NOW(3)),
(3, 'admin:user:manage', '管理用户',     'ACTION', 'ENABLED', 3, NOW(3), NOW(3)),
(4, 'admin:agent:manage','管理 Agent',   'ACTION', 'ENABLED', 4, NOW(3), NOW(3));

-- ADMIN 角色拥有所有权限
INSERT INTO `role_permission` (`id`, `role_id`, `permission_id`, `create_time`) VALUES
(1, 1, 1, NOW(3)),
(2, 1, 2, NOW(3)),
(3, 1, 3, NOW(3)),
(4, 1, 4, NOW(3));

-- USER 角色拥有项目查看和创建权限
INSERT INTO `role_permission` (`id`, `role_id`, `permission_id`, `create_time`) VALUES
(5, 2, 1, NOW(3)),
(6, 2, 2, NOW(3));
