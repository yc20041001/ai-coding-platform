-- V11: GitHub OAuth + PR Review tables
-- Milestone 17: GitHub OAuth authorization, repository/PR caching, AI PR review jobs and findings

-- 1. github_oauth_state - OAuth state anti-CSRF
CREATE TABLE IF NOT EXISTS github_oauth_state (
    id BIGINT PRIMARY KEY,
    state VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    redirect_uri VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL,
    CONSTRAINT uk_github_oauth_state UNIQUE (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_github_oauth_user ON github_oauth_state (user_id);
CREATE INDEX idx_github_oauth_expires ON github_oauth_state (expires_at);

-- 2. github_repository_cache - GitHub repository cache
CREATE TABLE IF NOT EXISTS github_repository_cache (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    github_repo_id BIGINT NOT NULL,
    owner VARCHAR(128) NOT NULL,
    repo_name VARCHAR(128) NOT NULL,
    full_name VARCHAR(256) NOT NULL,
    private_repo TINYINT NOT NULL DEFAULT 0,
    default_branch VARCHAR(128) NULL,
    html_url VARCHAR(512) NULL,
    description TEXT NULL,
    language VARCHAR(64) NULL,
    github_updated_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_user_github_repo UNIQUE (user_id, github_repo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_github_repo_full_name ON github_repository_cache (full_name);
CREATE INDEX idx_github_repo_updated ON github_repository_cache (github_updated_at);

-- 3. github_pull_request_cache - GitHub PR cache
CREATE TABLE IF NOT EXISTS github_pull_request_cache (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NULL,
    repository_id BIGINT NULL,
    github_pr_id BIGINT NOT NULL,
    github_repo_id BIGINT NOT NULL,
    number INT NOT NULL,
    title VARCHAR(512) NOT NULL,
    state VARCHAR(32) NOT NULL,
    author_login VARCHAR(128) NULL,
    base_branch VARCHAR(128) NULL,
    head_branch VARCHAR(128) NULL,
    html_url VARCHAR(512) NULL,
    diff_url VARCHAR(512) NULL,
    patch_url VARCHAR(512) NULL,
    additions INT DEFAULT 0,
    deletions INT DEFAULT 0,
    changed_files INT DEFAULT 0,
    github_created_at DATETIME NULL,
    github_updated_at DATETIME NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    CONSTRAINT uk_github_pr UNIQUE (github_repo_id, number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_github_pr_project ON github_pull_request_cache (project_id);
CREATE INDEX idx_github_pr_repo ON github_pull_request_cache (repository_id);
CREATE INDEX idx_github_pr_state ON github_pull_request_cache (state);
CREATE INDEX idx_github_pr_updated ON github_pull_request_cache (github_updated_at);

-- 4. pr_review_job - PR Review job
CREATE TABLE IF NOT EXISTS pr_review_job (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    repository_id BIGINT NULL,
    pull_request_id BIGINT NOT NULL,
    agent_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    review_mode VARCHAR(32) NOT NULL DEFAULT 'FULL',
    summary MEDIUMTEXT NULL,
    risk_level VARCHAR(32) NULL,
    model_provider VARCHAR(64) NULL,
    model_name VARCHAR(128) NULL,
    token_usage BIGINT DEFAULT 0,
    error_message TEXT NULL,
    started_at DATETIME NULL,
    finished_at DATETIME NULL,
    creator_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_pr_review_project_time ON pr_review_job (project_id, create_time);
CREATE INDEX idx_pr_review_pr ON pr_review_job (pull_request_id);
CREATE INDEX idx_pr_review_status ON pr_review_job (status);
CREATE INDEX idx_pr_review_creator ON pr_review_job (creator_id);

-- 5. pr_review_finding - PR Review findings
CREATE TABLE IF NOT EXISTS pr_review_finding (
    id BIGINT PRIMARY KEY,
    review_job_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    severity VARCHAR(32) NOT NULL,
    category VARCHAR(64) NOT NULL,
    file_path VARCHAR(512) NULL,
    line_number INT NULL,
    title VARCHAR(512) NOT NULL,
    description MEDIUMTEXT NULL,
    suggestion MEDIUMTEXT NULL,
    code_snippet MEDIUMTEXT NULL,
    create_time DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_review_finding_job ON pr_review_finding (review_job_id);
CREATE INDEX idx_review_finding_project ON pr_review_finding (project_id);
CREATE INDEX idx_review_finding_severity ON pr_review_finding (severity);
CREATE INDEX idx_review_finding_file ON pr_review_finding (file_path);
