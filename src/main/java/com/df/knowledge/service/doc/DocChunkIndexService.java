package com.df.knowledge.service.doc;

import com.df.knowledge.dal.dataobject.doc.DocChunkDO;

import java.util.List;

/**
 * 分块索引端口，生产实现写入自管 Elasticsearch 索引
 */
public interface DocChunkIndexService {

    /**
     * 同步批量写入分块与向量，返回时已可检索，任意条目失败都抛出异常
     *
     * @param knowledgeBaseId 所属知识库编号
     * @param chunks 分块列表
     * @param vectors 与分块顺序一致的向量列表
     */
    void writeBatch(Long knowledgeBaseId, List<DocChunkDO> chunks, List<float[]> vectors);

    /**
     * 同步删除文档的全部索引，返回时检索不可见
     *
     * @param docId 文档编号
     */
    void deleteByDocId(Long docId);
}
