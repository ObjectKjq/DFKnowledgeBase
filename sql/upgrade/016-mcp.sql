-- MCP 凭证与调用审计；增量升级不修改现有业务数据。
CREATE TABLE IF NOT EXISTS mcp_credential (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL COMMENT '凭证创建者',
 name VARCHAR(80) NOT NULL,
 token_hash CHAR(64) NOT NULL COMMENT 'SHA-256 摘要，禁止返回',
 token_prefix VARCHAR(24) NOT NULL,
 tools VARCHAR(128) NOT NULL DEFAULT '',
 expires_at DATETIME(6) NOT NULL,
 revoked_at DATETIME(6) NULL,
 last_used_at DATETIME(6) NULL,
 create_time DATETIME(6) NOT NULL,
 rate_window_start BIGINT NOT NULL DEFAULT 0,
 rate_window_count INT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_mcp_token_hash (token_hash),
 KEY idx_mcp_credential_user (user_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人 MCP 凭证';
CREATE TABLE IF NOT EXISTS mcp_call_log (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 credential_id BIGINT NULL,
 user_id BIGINT NULL,
 call_time DATETIME(6) NOT NULL,
 tool_name VARCHAR(64) NOT NULL,
 knowledge_base_ids TEXT NOT NULL COMMENT '仅已授权编号 JSON',
 result_count INT NOT NULL DEFAULT 0,
 outcome VARCHAR(16) NOT NULL,
 reason_code VARCHAR(64) NOT NULL,
 KEY idx_mcp_log_user_time (user_id,call_time,id),
 KEY idx_mcp_log_credential_time (credential_id,call_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 脱敏调用记录';
