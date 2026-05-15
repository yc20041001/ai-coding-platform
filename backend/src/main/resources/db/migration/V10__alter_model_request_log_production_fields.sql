-- V10: Production hardening fields for model_request_log
-- Adds fallback tracking, error codes, and cost estimation support

ALTER TABLE model_request_log
  ADD COLUMN fallback_used TINYINT NOT NULL DEFAULT 0 COMMENT '是否使用了 fallback',
  ADD COLUMN error_code VARCHAR(64) NULL COMMENT '错误码',
  ADD COLUMN estimated_cost DECIMAL(12, 8) NULL COMMENT '预估成本(USD)';

-- Index for fallback analysis
CREATE INDEX idx_model_request_fallback ON model_request_log (fallback_used);
CREATE INDEX idx_model_request_error_code ON model_request_log (error_code);
