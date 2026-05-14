-- V7: Agent Orchestrator + Model Gateway Tables
-- agent_execution, model_request_log

CREATE TABLE `agent_execution` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `task_id` BIGINT NULL COMMENT '任务 ID',
    `chat_session_id` BIGINT NULL COMMENT '聊天会话 ID',
    `chat_message_id` BIGINT NULL COMMENT '聊天消息 ID',
    `agent_id` BIGINT NOT NULL COMMENT 'Agent ID',
    `execution_type` VARCHAR(32) NOT NULL COMMENT '执行类型: TASK/CHAT/REVIEW/MANUAL',
    `status` VARCHAR(32) NOT NULL COMMENT '状态: PENDING/RUNNING/COMPLETED/FAILED/CANCELED',
    `input_prompt` MEDIUMTEXT NULL COMMENT '输入 Prompt',
    `output_content` MEDIUMTEXT NULL COMMENT '输出内容',
    `error_message` TEXT NULL COMMENT '错误信息',
    `started_at` DATETIME(3) NULL COMMENT '开始时间',
    `finished_at` DATETIME(3) NULL COMMENT '结束时间',
    `token_usage` BIGINT NOT NULL DEFAULT 0 COMMENT 'Token 使用量',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间',
    `update_time` DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_agent_execution_project_time` (`project_id`, `create_time`),
    INDEX `idx_agent_execution_task` (`task_id`),
    INDEX `idx_agent_execution_chat` (`chat_session_id`, `chat_message_id`),
    INDEX `idx_agent_execution_agent` (`agent_id`),
    INDEX `idx_agent_execution_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 执行记录表';

CREATE TABLE `model_request_log` (
    `id` BIGINT NOT NULL COMMENT '主键 ID',
    `project_id` BIGINT NOT NULL COMMENT '项目 ID',
    `execution_id` BIGINT NULL COMMENT 'Agent 执行记录 ID',
    `provider` VARCHAR(32) NOT NULL COMMENT '模型供应商',
    `model_name` VARCHAR(128) NOT NULL COMMENT '模型名称',
    `request_type` VARCHAR(32) NOT NULL COMMENT '请求类型',
    `prompt_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'Prompt Token 数',
    `completion_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT 'Completion Token 数',
    `total_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '总 Token 数',
    `latency_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '耗时毫秒',
    `success` TINYINT NOT NULL DEFAULT 1 COMMENT '是否成功',
    `error_message` TEXT NULL COMMENT '错误信息',
    `create_time` DATETIME(3) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_model_request_project_time` (`project_id`, `create_time`),
    INDEX `idx_model_request_execution` (`execution_id`),
    INDEX `idx_model_request_provider` (`provider`),
    INDEX `idx_model_request_success` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型请求日志表';
