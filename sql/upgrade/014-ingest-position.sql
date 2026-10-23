-- 已有数据库执行一次；部署前备份。保留原文档与分块，不重新编号。
ALTER TABLE `knowledge_doc`
    ADD COLUMN `ingest_submit_time` datetime(6) NULL COMMENT '本次入库提交时间' AFTER `status`,
    ADD INDEX `idx_ingest_schedule` (`status`, `ingest_submit_time`, `id`);
UPDATE `knowledge_doc` SET `ingest_submit_time` = `create_time` WHERE `ingest_submit_time` IS NULL;
ALTER TABLE `knowledge_doc` MODIFY COLUMN `ingest_submit_time` datetime(6) NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '本次入库提交时间';
ALTER TABLE `knowledge_doc_chunk`
    ADD COLUMN `sheet_name` varchar(255) NULL COMMENT 'Excel工作表名称' AFTER `page_number`,
    ADD COLUMN `ocr` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否来自扫描页OCR' AFTER `sheet_name`;
