CREATE TABLE IF NOT EXISTS code_index_file (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    repository_id BIGINT NULL,
    branch VARCHAR(128) NULL,
    file_path VARCHAR(500) NOT NULL,
    language VARCHAR(64) NULL,
    file_size BIGINT DEFAULT 0,
    line_count INT DEFAULT 0,
    content_hash VARCHAR(128) NULL,
    indexed_at DATETIME NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'INDEXED',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    UNIQUE KEY uk_code_index_file(project_id, branch, file_path),
    INDEX idx_code_index_project_branch(project_id, branch),
    INDEX idx_code_index_language(language),
    INDEX idx_code_index_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码索引文件表';

CREATE TABLE IF NOT EXISTS code_index_symbol (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    symbol_name VARCHAR(256) NOT NULL,
    symbol_type VARCHAR(64) NOT NULL,
    language VARCHAR(64) NULL,
    file_path VARCHAR(500) NOT NULL,
    start_line INT NULL,
    end_line INT NULL,
    snippet TEXT NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_code_symbol_project_name(project_id, symbol_name),
    INDEX idx_code_symbol_file(file_id),
    INDEX idx_code_symbol_type(symbol_type),
    INDEX idx_code_symbol_path(file_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码索引符号表';

CREATE TABLE IF NOT EXISTS code_index_chunk (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    chunk_index INT NOT NULL,
    start_line INT NOT NULL,
    end_line INT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    token_count INT DEFAULT 0,
    content_hash VARCHAR(128) NULL,
    create_time DATETIME NOT NULL,
    UNIQUE KEY uk_code_chunk_file_index(file_id, chunk_index),
    INDEX idx_code_chunk_project(project_id),
    INDEX idx_code_chunk_file(file_id),
    INDEX idx_code_chunk_path(file_path),
    INDEX idx_code_chunk_hash(content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码索引切片表';
