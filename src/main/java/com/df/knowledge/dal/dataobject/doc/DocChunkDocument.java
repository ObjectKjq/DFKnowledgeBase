package com.df.knowledge.dal.dataobject.doc;

import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.KnnAlgorithmType;
import org.springframework.data.elasticsearch.annotations.KnnIndexOptions;
import org.springframework.data.elasticsearch.annotations.KnnSimilarity;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;

/**
 * 文档分块的 Elasticsearch 索引实体，映射与部署脚本保持一致
 */
@Document(indexName = "#{@environment.getProperty('knowledge.elasticsearch.index-name', 'knowledge_doc_chunk')}",
        createIndex = false, writeTypeHint = WriteTypeHint.FALSE, dynamic = Dynamic.STRICT,
        storeIdInSource = true)
@Data
public class DocChunkDocument {

    /** 知识库编号，关联 {@link KnowledgeBaseDO#getId()}，以字符串匹配 keyword 字段 */
    @Field(name = "kb_id", type = FieldType.Keyword)
    private String knowledgeBaseId;

    /** 文档编号，关联 {@link DocDO#getId()} */
    @Field(name = "doc_id", type = FieldType.Keyword)
    private String docId;

    /** 分块编号，关联 {@link DocChunkDO#getId()}，同时作为索引文档编号 */
    @Id
    @Field(name = "chunk_id", type = FieldType.Keyword)
    private String chunkId;

    /** 块序号，从 0 开始 */
    @Field(name = "chunk_index", type = FieldType.Integer)
    private Integer chunkIndex;

    /** 分块文本，使用中文分词器建立全文索引 */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String text;

    /** 文档抽取文本中的字符起点（含），按 Java UTF-16 字符计数 */
    @Field(name = "start_offset", type = FieldType.Integer)
    private Integer startOffset;

    /** 文档抽取文本中的字符终点（不含） */
    @Field(name = "end_offset", type = FieldType.Integer)
    private Integer endOffset;

    /** 页码，从 1 开始；无分页信息时为空 */
    @Field(name = "page_number", type = FieldType.Integer)
    private Integer pageNumber;

    /** 内置中文向量模型生成的 512 维向量 */
    @Field(type = FieldType.Dense_Vector, dims = 512, knnSimilarity = KnnSimilarity.COSINE,
            knnIndexOptions = @KnnIndexOptions(type = KnnAlgorithmType.HNSW))
    private float[] vector;
}
