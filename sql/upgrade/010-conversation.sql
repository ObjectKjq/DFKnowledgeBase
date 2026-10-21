-- 已有数据库的增量脚本，由部署者执行；全新数据库使用 sql/schema.sql。
-- retrieval_scope 固化会话发起时的知识库编号范围，空数组表示全部有权知识库。
CREATE TABLE IF NOT EXISTS `knowledge_conversation` (
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`         bigint       NOT NULL COMMENT '所属用户ID（关联 system_user 表）',
    `title`           varchar(100) NOT NULL COMMENT '会话标题',
    `retrieval_scope` json         NOT NULL COMMENT '发起会话时的知识库ID范围，空数组表示全部有权知识库',
    `creator`         varchar(64)  NULL DEFAULT '' COMMENT '创建者',
    `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         varchar(64)  NULL DEFAULT '' COMMENT '更新者',
    `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_user_update_time` (`user_id`, `update_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会话表';

-- content 使用 JSON 保留用户消息、结果卡片、答案与引用等不同消息结构。
CREATE TABLE IF NOT EXISTS `knowledge_message` (
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
