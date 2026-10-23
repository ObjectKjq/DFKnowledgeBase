-- ----------------------------------------------------------------------------
-- 知识库平台建表脚本（用户功能 RBAC、知识库与成员）
-- 适用 MySQL 5.7+ / 8.0，InnoDB + utf8mb4
-- 逻辑删除约定：deleted = 0 未删除、1 已删除（登录日志除外，不删除）
-- ----------------------------------------------------------------------------

-- ----------------------------
-- 用户信息表
-- ----------------------------
DROP TABLE IF EXISTS `system_user`;
CREATE TABLE `system_user` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    varchar(30)  NOT NULL COMMENT '用户账号',
    `password`    varchar(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname`    varchar(30)  NOT NULL COMMENT '用户昵称',
    `avatar`      varchar(512) NULL DEFAULT '' COMMENT '头像地址',
    `email`       varchar(50)  NULL DEFAULT '' COMMENT '用户邮箱',
    `mobile`      varchar(30)  NULL DEFAULT '' COMMENT '手机号码',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '账号状态（0 开启 1 停用）',
    `login_ip`    varchar(50)  NULL DEFAULT '' COMMENT '最后登录 IP',
    `login_date`  datetime     NULL DEFAULT NULL COMMENT '最后登录时间',
    `remark`      varchar(500) NULL DEFAULT NULL COMMENT '备注',
    `creator`     varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_username` (`username`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户信息表';

-- ----------------------------
-- 角色信息表
-- ----------------------------
DROP TABLE IF EXISTS `system_role`;
CREATE TABLE `system_role` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `name`        varchar(30)  NOT NULL COMMENT '角色名称',
    `code`        varchar(100) NOT NULL COMMENT '角色权限标识',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '角色状态（0 开启 1 停用）',
    `type`        tinyint      NOT NULL COMMENT '角色类型（1 内置 2 自定义）',
    `remark`      varchar(500) NULL DEFAULT NULL COMMENT '备注',
    `creator`     varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_code` (`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色信息表';

-- ----------------------------
-- 菜单权限表
-- ----------------------------
DROP TABLE IF EXISTS `system_menu`;
CREATE TABLE `system_menu` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `name`        varchar(50)  NOT NULL COMMENT '菜单名称',
    `permission`  varchar(100) NULL DEFAULT '' COMMENT '权限标识（目录类型无权限串，留空）',
    `type`        tinyint      NOT NULL COMMENT '菜单类型（1 目录 2 菜单 3 按钮）',
    `sort`        int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父菜单ID（根为 0）',
    `path`        varchar(200) NULL DEFAULT '' COMMENT '路由地址',
    `icon`        varchar(100) NULL DEFAULT '' COMMENT '菜单图标',
    `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '菜单状态（0 开启 1 停用）',
    `visible`     bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否可见',
    `creator`     varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '菜单权限表';

-- ----------------------------
-- 用户和角色关联表
-- ----------------------------
DROP TABLE IF EXISTS `system_user_role`;
CREATE TABLE `system_user_role` (
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    `user_id`     bigint      NOT NULL COMMENT '用户ID（关联 system_user 表）',
    `role_id`     bigint      NOT NULL COMMENT '角色ID（关联 system_role 表）',
    `creator`     varchar(64) NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64) NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE,
    KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户和角色关联表';

-- ----------------------------
-- 角色和菜单关联表
-- ----------------------------
DROP TABLE IF EXISTS `system_role_menu`;
CREATE TABLE `system_role_menu` (
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    `role_id`     bigint      NOT NULL COMMENT '角色ID（关联 system_role 表）',
    `menu_id`     bigint      NOT NULL COMMENT '菜单ID（关联 system_menu 表）',
    `creator`     varchar(64) NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64) NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_role_id` (`role_id`) USING BTREE,
    KEY `idx_menu_id` (`menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '角色和菜单关联表';

-- ----------------------------
-- 登录日志表（只增不改不删，故无 updater / update_time / deleted）
-- ----------------------------
DROP TABLE IF EXISTS `system_login_log`;
CREATE TABLE `system_login_log` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '日志编号',
    `user_id`     bigint       NULL DEFAULT NULL COMMENT '用户ID（关联 system_user 表；账号不存在时为空）',
    `username`    varchar(30)  NOT NULL COMMENT '用户账号（快照，不随用户删除）',
    `login_ip`    varchar(50)  NULL DEFAULT '' COMMENT '登录 IP',
    `user_agent`  varchar(512) NULL DEFAULT '' COMMENT '浏览器 UA',
    `result`      tinyint      NOT NULL COMMENT '登录结果（0 成功 10 账号或密码错 20 用户被禁用）',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '登录日志表';

-- ----------------------------
-- 文件表（通用文件元数据，文件本体存 system_file_content，ADR-0009）
-- ----------------------------
DROP TABLE IF EXISTS `system_file`;
CREATE TABLE `system_file` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    `uploader_id` bigint NULL DEFAULT NULL COMMENT '上传者用户编号',
    `name`        varchar(255) NOT NULL COMMENT '上传时的原始文件名',
    `type`        varchar(128) NOT NULL COMMENT '文件 MIME 类型（Content-Type）',
    `size`        bigint       NOT NULL COMMENT '文件大小（字节）',
    `creator`     varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件表';

-- ----------------------------
-- 文件内容表（与文件表 1:1，content 用 LONGBLOB，部署 MySQL 需 max_allowed_packet ≥ 64MB）
-- ----------------------------
DROP TABLE IF EXISTS `system_file_content`;
CREATE TABLE `system_file_content` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '文件内容ID',
    `file_id`     bigint       NOT NULL COMMENT '文件ID（关联 system_file 表）',
    `content`     longblob     NOT NULL COMMENT '文件内容（二进制）',
    `creator`     varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_file_id` (`file_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件内容表';

-- ----------------------------
-- 知识库表
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
    `delete_time` datetime NULL DEFAULT NULL COMMENT '回收站删除时间',
    `name`         varchar(50)  NOT NULL COMMENT '知识库名称',
    `type`         tinyint      NOT NULL COMMENT '知识库类型（1 个人 2 共享）',
    `owner_id`     bigint       NOT NULL COMMENT '所有者用户ID（关联 system_user 表）',
    `default_flag` bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否默认知识库（用户创建时自动生成，不可删除）',
    `description`  varchar(200) NULL DEFAULT '' COMMENT '知识库说明',
    `creator`      varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`      varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_owner_id` (`owner_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库表';

-- ----------------------------
-- 知识库成员表
-- 「知识库 + 用户」的唯一性由服务层校验保证：本表随公共字段逻辑删除，
-- 若建联合唯一约束，成员被移除后将无法再次邀请同一用户
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_base_member`;
CREATE TABLE `knowledge_base_member` (
    `id`                bigint      NOT NULL AUTO_INCREMENT COMMENT '自增编号',
    `knowledge_base_id` bigint      NOT NULL COMMENT '知识库ID（关联 knowledge_base 表）',
    `user_id`           bigint      NOT NULL COMMENT '成员用户ID（关联 system_user 表）',
    `member_role`       tinyint     NOT NULL COMMENT '成员角色（1 编辑者 2 查看者）',
    `creator`           varchar(64) NULL DEFAULT '' COMMENT '创建者',
    `create_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`           varchar(64) NULL DEFAULT '' COMMENT '更新者',
    `update_time`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_knowledge_base_id` (`knowledge_base_id`) USING BTREE,
    KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库成员表';

-- ----------------------------
-- 文档表
-- 状态机（PRD §8.2）：1 排队 2 解析中 3 索引中 4 可用 5 失败 6 已删除（软删除）
-- 软删除 30 天保留期：delete_time 为起算点，prev_status 记录删除前状态供恢复
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_doc`;
CREATE TABLE `knowledge_doc` (
    `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `knowledge_base_id` bigint       NOT NULL COMMENT '知识库ID（关联 knowledge_base 表）',
    `title`             varchar(200) NOT NULL COMMENT '文档标题（默认取上传文件名）',
    `file_name`         varchar(255) NOT NULL COMMENT '上传时的原始文件名',
    `format`            tinyint      NOT NULL COMMENT '文档格式（1 PDF 2 Word 3 PPT 4 Excel 5 TXT 6 Markdown）',
    `file_size`         bigint       NOT NULL COMMENT '文件大小（字节）',
    `file_id`           bigint       NULL DEFAULT NULL COMMENT '文件ID（关联 system_file 表，过渡期存量文档为空）',
    `file_url`          varchar(255) NULL DEFAULT NULL COMMENT '文件下载链接（上传接口返回的 url，过渡期存量文档为空）',
    `file_path`         varchar(500) NULL DEFAULT NULL COMMENT '过渡字段：存量磁盘文件路径，迁移入库后置空，随双读逻辑一并移除',
    `status`            tinyint      NOT NULL COMMENT '文档状态（1 排队 2 解析中 3 索引中 4 可用 5 失败 6 已删除）',
    `ingest_submit_time` datetime(6) NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '本次入库提交时间',
    `fail_reason`       varchar(500) NULL DEFAULT NULL COMMENT '失败原因（保留首次失败原因）',
    `prev_status`       tinyint      NULL DEFAULT NULL COMMENT '删除前状态（软删除恢复到删除前状态用）',
    `delete_time`       datetime     NULL DEFAULT NULL COMMENT '删除时间（软删除 30 天保留期起算点）',
    `creator`           varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`           varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_knowledge_base_id` (`knowledge_base_id`) USING BTREE,
    KEY `idx_ingest_schedule` (`status`, `ingest_submit_time`, `id`) USING BTREE,
    KEY `idx_status` (`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档表';

-- ----------------------------
-- 文档分块表（重试时在事务中物理替换，不保留旧分块）
-- 偏移为抽取文本的 UTF-16 字符区间 [start_offset, end_offset)，分页文本直接拼接
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_doc_chunk`;
CREATE TABLE `knowledge_doc_chunk` (
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '分块ID',
    `doc_id`       bigint      NOT NULL COMMENT '文档ID（关联 knowledge_doc 表）',
    `chunk_index`  int         NOT NULL COMMENT '块序号（从0开始）',
    `text`         text        NOT NULL COMMENT '分块文本',
    `start_offset` int         NOT NULL COMMENT '文档抽取文本的起始字符偏移（含）',
    `end_offset`   int         NOT NULL COMMENT '文档抽取文本的结束字符偏移（不含）',
    `sheet_name`   varchar(255) NULL DEFAULT NULL COMMENT 'Excel工作表名称',
    `ocr`          bit(1) NOT NULL DEFAULT b'0' COMMENT '是否来自扫描页OCR',
    `page_number`  int         NULL DEFAULT NULL COMMENT 'PDF/PPT页码（从1开始），其他格式为空',
    `creator`      varchar(64) NULL DEFAULT '' COMMENT '创建者',
    `create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`      varchar(64) NULL DEFAULT '' COMMENT '更新者',
    `update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_doc_chunk_index` (`doc_id`, `chunk_index`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档分块表';

-- ----------------------------
-- 会话表
-- retrieval_scope 固化会话发起时的知识库编号范围，空数组表示全部有权知识库
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_conversation`;
CREATE TABLE `knowledge_conversation` (
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`         bigint       NOT NULL COMMENT '所属用户ID（关联 system_user 表）',
    `title`           varchar(100) NOT NULL COMMENT '会话标题',
    `retrieval_scope` json         NOT NULL COMMENT '发起会话时的知识库ID范围，空数组表示全部有权知识库',
    `state`           json         NULL COMMENT '当前会话检索选择与澄清状态',
    `creator`         varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_update_time` (`user_id`, `update_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会话表';

-- ----------------------------
-- 会话消息表
-- content 使用 JSON 保留用户消息、结果卡片、答案与引用等不同消息结构
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_message`;
CREATE TABLE `knowledge_message` (
    `id`              bigint      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` bigint      NOT NULL COMMENT '会话ID（关联 knowledge_conversation 表）',
    `type`            varchar(32) NOT NULL COMMENT '消息类型（user/results/answer/guide/empty/note）',
    `content`         json        NOT NULL COMMENT '结构化消息内容',
    `creator`         varchar(64) NULL DEFAULT '' COMMENT '创建者',
    `create_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         varchar(64) NULL DEFAULT '' COMMENT '更新者',
    `update_time`     datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_conversation_time` (`conversation_id`, `create_time`, `id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会话消息表';

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
