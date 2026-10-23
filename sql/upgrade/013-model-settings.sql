-- P0 命名模型与实例设置；对既有数据只做增量迁移。
CREATE TABLE IF NOT EXISTS system_model_configuration (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 kind VARCHAR(10) NOT NULL,
 base_url VARCHAR(1000) NOT NULL,
 api_key VARCHAR(2000) NOT NULL DEFAULT '',
 model VARCHAR(200) NOT NULL,
 timeout_seconds INT NOT NULL DEFAULT 60,
 test_status VARCHAR(20) NOT NULL DEFAULT 'UNTESTED',
 last_test_time DATETIME NULL,
 test_error VARCHAR(500) NULL,
 rerank_slot TINYINT GENERATED ALWAYS AS (CASE WHEN kind = 'RERANK' THEN 1 ELSE NULL END) STORED,
 UNIQUE KEY uk_rerank_slot (rerank_slot),
 UNIQUE KEY uk_model_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='命名模型接入配置';
CREATE TABLE IF NOT EXISTS system_instance_settings (
 id BIGINT NOT NULL PRIMARY KEY,
 mcp_enabled BIT NOT NULL DEFAULT 1,
 mcp_rate_limit_per_minute INT NOT NULL DEFAULT 60,
 ingest_global_concurrency INT NOT NULL DEFAULT 2,
 ingest_knowledge_base_concurrency INT NOT NULL DEFAULT 1,
 legacy_model_migrated BIT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单实例运行设置';
INSERT IGNORE INTO system_instance_settings(id,mcp_enabled,mcp_rate_limit_per_minute,ingest_global_concurrency,ingest_knowledge_base_concurrency) VALUES (1,1,60,2,1);
