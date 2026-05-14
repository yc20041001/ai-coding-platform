-- V6: Chat Module Tables
-- chat_session, chat_message, chat_message_reference

CREATE TABLE `chat_session` (
    `id` BIGINT NOT NULL COMMENT '会话 ID',
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '会话标题',
    `session_type` VARCHAR(32) NOT NULL COMMENT 'SINGLE / PROJECT / AGENT_GROUP',
    `creator_id` BIGINT NOT NULL COMMENT '创建人',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / ARCHIVED',
    `last_message_time` DATETIME(3) DEFAULT NULL COMMENT '最近消息时间',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间',
    `update_time` DATETIME(3) NOT NULL COMMENT '更新时间',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_chat_session_project_time` (`project_id`, `last_message_time`),
    INDEX `idx_chat_session_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会话表';

CREATE TABLE `chat_message` (
    `id` BIGINT NOT NULL COMMENT '消息 ID',
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `session_id` BIGINT NOT NULL COMMENT '会话 ID',
    `sender_id` BIGINT DEFAULT NULL COMMENT '发送人 ID',
    `sender_type` VARCHAR(32) NOT NULL COMMENT 'USER / AGENT / SYSTEM',
    `agent_id` BIGINT DEFAULT NULL COMMENT 'Agent ID',
    `task_id` BIGINT DEFAULT NULL COMMENT '关联任务 ID',
    `message_type` VARCHAR(32) NOT NULL COMMENT 'TEXT / MARKDOWN / CODE / TOOL_RESULT / ERROR',
    `content` MEDIUMTEXT NOT NULL COMMENT '消息内容',
    `status` VARCHAR(32) NOT NULL COMMENT 'STREAMING / COMPLETED / FAILED / CANCELED',
    `token_usage` BIGINT NOT NULL DEFAULT 0 COMMENT '本消息 Token 用量',
    `metadata` JSON DEFAULT NULL COMMENT '模型、耗时、上下文摘要等',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间',
    `update_time` DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_chat_message_session_time` (`session_id`, `create_time`),
    INDEX `idx_chat_message_project_time` (`project_id`, `create_time`),
    INDEX `idx_chat_message_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息表';

CREATE TABLE `chat_message_reference` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `message_id` BIGINT NOT NULL COMMENT '消息 ID',
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `reference_type` VARCHAR(32) NOT NULL COMMENT 'DOCUMENT / CODE / TASK / URL',
    `reference_id` BIGINT DEFAULT NULL COMMENT '引用对象 ID',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '标题',
    `url` VARCHAR(512) DEFAULT NULL COMMENT '链接',
    `file_path` VARCHAR(512) DEFAULT NULL COMMENT '文件路径',
    `start_line` INT DEFAULT NULL COMMENT '起始行',
    `end_line` INT DEFAULT NULL COMMENT '结束行',
    `score` DECIMAL(10, 6) DEFAULT NULL COMMENT '相似度分数',
    `snippet` TEXT DEFAULT NULL COMMENT '引用片段',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_message_reference_message` (`message_id`),
    INDEX `idx_message_reference_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息引用表';
