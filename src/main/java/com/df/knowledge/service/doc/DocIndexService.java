package com.df.knowledge.service.doc;

/**
 * 文档索引 Service 接口
 *
 * 入库管线的索引阶段：向量化并写入分块索引（PRD FR-3），实现由入库管线后续交付补充
 */
public interface DocIndexService {

    /**
     * 建立文档分块的检索索引
     *
     * @param docId 文档编号
     */
    void index(Long docId);
}
