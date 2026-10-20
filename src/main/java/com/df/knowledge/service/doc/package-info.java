/**
 * 文档业务：文档生命周期管理（上传、重试、软删除/恢复）与入库管线的异步执行骨架（ADR-0007）
 *
 * 状态迁移唯一依据 PRD §8.2 文档生命周期；内容权限统一经 KnowledgeBasePermissionService 判定（PRD §8.1）
 */
package com.df.knowledge.service.doc;
