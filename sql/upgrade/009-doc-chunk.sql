-- 已有数据库的增量脚本，由部署者执行；全新数据库使用 sql/schema.sql。
-- 偏移为抽取文本的 UTF-16 字符区间 [start_offset, end_offset)，分页文本直接拼接。
CREATE TABLE IF NOT EXISTS `knowledge_doc_chunk` (
    `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '分块ID',
    `doc_id`       bigint      NOT NULL COMMENT '文档ID（关联 knowledge_doc 表）',
    `chunk_index`  int         NOT NULL COMMENT '块序号（从0开始）',
    `text`         text        NOT NULL COMMENT '分块文本',
    `start_offset` int         NOT NULL COMMENT '文档抽取文本的起始字符偏移（含）',
    `end_offset`   int         NOT NULL COMMENT '文档抽取文本的结束字符偏移（不含）',
    `page_number`  int         NULL DEFAULT NULL COMMENT 'PDF/PPT页码（从1开始），其他格式为空',
    `creator`      varchar(64) NULL DEFAULT '' COMMENT '创建者',
    `create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`      varchar(64) NULL DEFAULT '' COMMENT '更新者',
    `update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      bit(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_doc_chunk_index` (`doc_id`, `chunk_index`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档分块表';
