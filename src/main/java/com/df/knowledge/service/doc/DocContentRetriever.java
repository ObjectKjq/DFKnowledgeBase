package com.df.knowledge.service.doc;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;

import java.util.List;

/**
 * 按当前用户与所选范围调用自管 ES 混合检索的 RAG 桥接器
 */
public class DocContentRetriever implements ContentRetriever {

    private final DocSearchService docSearchService;
    private final Long userId;
    private final List<Long> knowledgeBaseIds;

    public DocContentRetriever(DocSearchService docSearchService, Long userId, List<Long> knowledgeBaseIds) {
        this.docSearchService = docSearchService;
        this.userId = userId;
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    @Override
    public List<Content> retrieve(Query query) {
        return docSearchService.search(userId, query.text(), knowledgeBaseIds).stream().map(hit -> {
            Metadata metadata = new Metadata().put("kb_id", hit.getKnowledgeBaseId())
                    .put("doc_id", hit.getDocId()).put("chunk_id", hit.getChunkId())
                    .put("chunk_index", hit.getChunkIndex());
            if (hit.getPageNumber() != null) {
                metadata.put("page_number", hit.getPageNumber());
            }
            return Content.from(TextSegment.from(hit.getText(), metadata));
        }).toList();
    }
}
