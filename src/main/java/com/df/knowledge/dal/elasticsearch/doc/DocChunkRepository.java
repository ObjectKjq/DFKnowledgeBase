package com.df.knowledge.dal.elasticsearch.doc;

import com.df.knowledge.dal.dataobject.doc.DocChunkDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * 文档分块的 Elasticsearch Repository，使用字符串分块编号作为主键
 */
public interface DocChunkRepository extends ElasticsearchRepository<DocChunkDocument, String> {
}
