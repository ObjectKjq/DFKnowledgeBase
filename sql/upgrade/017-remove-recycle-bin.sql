-- 017：移除知识库与文档回收站。旧版本升级到 016 后执行一次，执行前备份并停止应用。
-- 清理已经删除的业务对象，保留正常文档及其共享文件引用，再移除废弃字段。
-- 历史磁盘文件不由 SQL 删除；先保存下面查询输出，确认无正常文档引用后按路径清理。
CREATE TEMPORARY TABLE removed_knowledge_bases AS
SELECT id FROM knowledge_base WHERE delete_time IS NOT NULL OR deleted = b'1';
CREATE TEMPORARY TABLE removed_documents AS
SELECT d.id, d.file_id, d.file_path FROM knowledge_doc d
LEFT JOIN removed_knowledge_bases b ON b.id = d.knowledge_base_id
WHERE d.status = 6 OR d.deleted = b'1' OR d.delete_time IS NOT NULL OR b.id IS NOT NULL;
CREATE TEMPORARY TABLE removed_document_files AS
SELECT DISTINCT file_id FROM removed_documents WHERE file_id IS NOT NULL;

SELECT DISTINCT r.file_path AS legacy_file_to_remove FROM removed_documents r
WHERE r.file_path IS NOT NULL AND r.file_path <> ''
AND NOT EXISTS (
    SELECT 1 FROM knowledge_doc d WHERE d.file_path = r.file_path AND d.status <> 6
    AND d.deleted = b'0' AND d.delete_time IS NULL
    AND EXISTS (SELECT 1 FROM knowledge_base b WHERE b.id = d.knowledge_base_id
                AND b.deleted = b'0' AND b.delete_time IS NULL)
);

START TRANSACTION;
DELETE c FROM knowledge_doc_chunk c JOIN removed_documents d ON d.id = c.doc_id;
DELETE d FROM knowledge_doc d JOIN removed_documents r ON r.id = d.id;
DELETE m FROM knowledge_base_member m JOIN removed_knowledge_bases b ON b.id = m.knowledge_base_id;
DELETE b FROM knowledge_base b JOIN removed_knowledge_bases r ON r.id = b.id;
DELETE c FROM system_file_content c JOIN removed_document_files r ON r.file_id = c.file_id
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.file_id = c.file_id);
DELETE f FROM system_file f JOIN removed_document_files r ON r.file_id = f.id
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.file_id = f.id);
COMMIT;

ALTER TABLE knowledge_base DROP COLUMN delete_time;
ALTER TABLE knowledge_doc DROP COLUMN prev_status, DROP COLUMN delete_time;
ALTER TABLE knowledge_doc MODIFY COLUMN status tinyint NOT NULL
COMMENT '文档状态（1 排队 2 解析中 3 索引中 4 可用 5 失败）';

DROP TEMPORARY TABLE removed_document_files;
DROP TEMPORARY TABLE removed_documents;
DROP TEMPORARY TABLE removed_knowledge_bases;
