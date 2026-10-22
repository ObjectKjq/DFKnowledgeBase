-- 已有数据库的增量脚本，由部署者执行；全新数据库使用 sql/schema.sql。
-- 文件本体改存数据库（ADR-0009）：system_file 存元数据、system_file_content 存字节；
-- knowledge_doc 新增 file_id / file_url 承接两步式上传（先上传文件拿链接、再保存文档）。
-- 存量磁盘文件（knowledge.doc.storage-path）由启动迁移器入库并改写 file_id / file_url、置空 file_path，
-- 迁移完成且双读逻辑移除后，再执行文末注释的清理语句。
-- system_file_content.content 使用 LONGBLOB，部署 MySQL 需 max_allowed_packet ≥ 64MB。

CREATE TABLE IF NOT EXISTS `system_file` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
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

CREATE TABLE IF NOT EXISTS `system_file_content` (
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

-- file_path 转为过渡字段：新文档不再写入，存量文档由启动迁移器改写后置空
ALTER TABLE `knowledge_doc`
    ADD COLUMN `file_id`  bigint       NULL DEFAULT NULL COMMENT '文件ID（关联 system_file 表，过渡期存量文档为空）' AFTER `file_size`,
    ADD COLUMN `file_url` varchar(255) NULL DEFAULT NULL COMMENT '文件下载链接（上传接口返回的 url，过渡期存量文档为空）' AFTER `file_id`,
    MODIFY COLUMN `file_path` varchar(500) NULL DEFAULT NULL COMMENT '过渡字段：存量磁盘文件路径，迁移入库后置空，随双读逻辑一并移除';

-- 迁移完成后的清理（手动执行）：
-- ALTER TABLE `knowledge_doc` DROP COLUMN `file_path`;
