-- P0 检索会话状态：保留历史内容和创建时范围，恢复为默认混合模式。
ALTER TABLE knowledge_conversation ADD COLUMN state JSON NULL COMMENT '当前会话检索选择与澄清状态';
UPDATE knowledge_conversation SET state = JSON_OBJECT('mode','HYBRID','knowledgeBaseIds',retrieval_scope,
 'formats',JSON_ARRAY(),'selectedChunkIds',JSON_ARRAY(),'guideRound',0,'guideAnswers',JSON_ARRAY()) WHERE state IS NULL;
