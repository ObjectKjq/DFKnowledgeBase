-- 012：文件业务授权与知识库回收站。升级前先备份；本脚本只执行一次。
-- 不删除存量业务数据。旧逻辑删除知识库按最后更新时间进入回收站。
ALTER TABLE system_file ADD COLUMN uploader_id bigint NULL DEFAULT NULL COMMENT '上传者用户编号' AFTER id;
ALTER TABLE knowledge_base ADD COLUMN delete_time datetime NULL DEFAULT NULL COMMENT '回收站删除时间' AFTER id;
CREATE INDEX idx_knowledge_base_delete_time ON knowledge_base(delete_time);
CREATE INDEX idx_knowledge_doc_file_id ON knowledge_doc(file_id);

-- creator 是旧上传时记录的可信审计身份。无可信身份的未关联文件保持不公开，不能分配给管理员。
UPDATE system_file SET uploader_id = CAST(creator AS UNSIGNED)
WHERE creator REGEXP '^[1-9][0-9]*$' AND uploader_id IS NULL;

-- 旧实现可能先删文件导致业务引用失效；仍存在可恢复引用时恢复元数据及字节的逻辑标记。
UPDATE system_file f JOIN knowledge_doc d ON d.file_id = f.id AND d.deleted = b'0'
SET f.deleted = b'0';
UPDATE system_file_content c JOIN system_file f ON f.id = c.file_id AND f.deleted = b'0'
SET c.deleted = b'0';

-- 保留能够恢复的知识库数据；旧实现已经删除的成员审计无法区分主动移除，不擅自复活历史成员。
UPDATE knowledge_base SET delete_time = COALESCE(update_time, create_time, NOW()), deleted = b'0'
WHERE deleted = b'1';

-- 清除旧实现留下且已无业务引用的逻辑删除文件，实际回收 LONGBLOB。
DELETE c FROM system_file_content c JOIN system_file f ON f.id = c.file_id
WHERE f.deleted = b'1' AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.file_id = f.id AND d.deleted = b'0');
DELETE f FROM system_file f WHERE f.deleted = b'1'
AND NOT EXISTS (SELECT 1 FROM knowledge_doc d WHERE d.file_id = f.id AND d.deleted = b'0');
