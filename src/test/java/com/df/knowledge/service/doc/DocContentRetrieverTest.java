package com.df.knowledge.service.doc;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DocContentRetriever} 的 RAG 内容边界测试
 */
class DocContentRetrieverTest {

    @Test
    void testRetrieve_preservesSourceMetadataForCitation() {
        // 准备参数：服务结果已完成权限及可用状态过滤
        DocSearchService searchService = mock(DocSearchService.class);
        DocChunkResult hit = new DocChunkResult().setKnowledgeBaseId(2L).setDocId(5L)
                .setChunkId(101L).setChunkIndex(1).setPageNumber(3).setText("原文分块");
        when(searchService.search(7L, "检索内容", List.of(2L))).thenReturn(List.of(hit));

        // 调用
        List<Content> contents = new DocContentRetriever(searchService, 7L, List.of(2L))
                .retrieve(Query.from("检索内容"));

        // 断言：RAG 引用可回到相同分块与页码
        assertEquals(1, contents.size());
        assertEquals("原文分块", contents.getFirst().textSegment().text());
        assertEquals(101L, contents.getFirst().textSegment().metadata().getLong("chunk_id"));
        assertEquals(3, contents.getFirst().textSegment().metadata().getInteger("page_number"));
    }
}
