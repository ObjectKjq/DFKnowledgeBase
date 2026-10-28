package com.df.knowledge.service.doc;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchShardStatistics;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.types.ConflictsType;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 验证 Spring Data 索引操作、权限过滤和 Java 端混合排序，不连接真实 ES。 */
@ExtendWith(MockitoExtension.class)
class DocChunkIndexServiceImplTest {

    private static final IndexCoordinates INDEX = IndexCoordinates.of("knowledge_doc_chunk");

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private IndexOperations indexOperations;

    @InjectMocks
    private DocChunkIndexServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "indexName", "knowledge_doc_chunk");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testWriteBatch_preservesMappingAndWaitsForRefresh() {
        // 准备参数
        float[] vector = new float[512];
        vector[0] = 1;

        // 调用
        service.writeBatch(2L, List.of(chunk()), List.of(vector));

        // 断言：幂等编号、keyword 字符串和定位字段不变，不写入额外类型字段。
        ArgumentCaptor<List<IndexQuery>> queries = ArgumentCaptor.forClass((Class) List.class);
        ArgumentCaptor<BulkOptions> options = ArgumentCaptor.forClass(BulkOptions.class);
        verify(elasticsearchOperations).bulkIndex(queries.capture(), options.capture(), eq(INDEX));
        IndexQuery query = queries.getValue().getFirst();
        assertEquals("100", query.getId());
        Document source = Document.parse(query.getSource());
        assertEquals("2", source.get("kb_id"));
        assertEquals("5", source.get("doc_id"));
        assertEquals("100", source.get("chunk_id"));
        assertEquals(1, source.get("chunk_index"));
        assertEquals("知识检索", source.get("text"));
        assertEquals(400, source.get("start_offset"));
        assertEquals(404, source.get("end_offset"));
        assertEquals(2, source.get("page_number"));
        assertEquals(512, ((List<?>) source.get("vector")).size());
        assertEquals(1, ((Number) ((List<?>) source.get("vector")).getFirst()).doubleValue());
        assertFalse(source.containsKey("_class"));
        assertEquals(RefreshPolicy.WAIT_UNTIL, options.getValue().getRefreshPolicy());
    }

    @Test
    void testWriteBatch_partialFailureRejected() {
        // 准备参数：Spring Data 将逐条写入错误转换为 BulkFailureException。
        when(elasticsearchOperations.bulkIndex(anyList(), any(BulkOptions.class), eq(INDEX)))
                .thenThrow(new BulkFailureException("部分失败",
                        Map.of("100", new BulkFailureException.FailureDetails(400, "向量维度错误"))));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.writeBatch(2L, List.of(chunk()), List.of(new float[512])));
        assertEquals(ErrorCodeConstants.DOC_INDEX_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(exception.getMessage().contains("向量维度错误"));
    }

    @Test
    void testDeleteByDocId_refreshesAndDeletesOnlyTargetDocument() {
        // 准备参数
        when(elasticsearchOperations.indexOps(INDEX)).thenReturn(indexOperations);
        when(elasticsearchOperations.delete(any(DeleteQuery.class), eq(Document.class), eq(INDEX)))
                .thenReturn(ByQueryResponse.builder().build());

        // 调用
        service.deleteByDocId(5L);

        // 断言：先刷新，再按文档编号删除并刷新检索结果。
        ArgumentCaptor<DeleteQuery> deletion = ArgumentCaptor.forClass(DeleteQuery.class);
        var order = inOrder(indexOperations, elasticsearchOperations);
        order.verify(indexOperations).refresh();
        order.verify(elasticsearchOperations).delete(deletion.capture(), eq(Document.class), eq(INDEX));
        NativeQuery query = (NativeQuery) deletion.getValue().getQuery();
        assertEquals("doc_id", query.getQuery().term().field());
        assertEquals("5", query.getQuery().term().value().stringValue());
        assertTrue(deletion.getValue().getRefresh());
        assertEquals(ConflictsType.Abort, deletion.getValue().getConflicts());
    }

    @Test
    void testDeleteByDocId_incompleteDeletionRejected() {
        // 准备参数：依次返回超时和版本冲突。
        when(elasticsearchOperations.indexOps(INDEX)).thenReturn(indexOperations);
        when(elasticsearchOperations.delete(any(DeleteQuery.class), eq(Document.class), eq(INDEX)))
                .thenReturn(ByQueryResponse.builder().withTimedOut(true).build(),
                        ByQueryResponse.builder().withVersionConflicts(1).build());

        // 调用并断言
        for (int attempt = 0; attempt < 2; attempt++) {
            ServiceException exception = assertThrows(ServiceException.class, () -> service.deleteByDocId(5L));
            assertEquals(ErrorCodeConstants.DOC_INDEX_FAILED.getCode(), exception.getErrorCode().getCode());
        }
    }

    @Test
    void testSearchHybrid_fusesTwoAuthorizedRequests() {
        // 准备参数：语义次序为 101、100，关键词仅命中 100。
        SearchHits<Document> semanticResponse = hits("101", "100");
        SearchHits<Document> keywordResponse = hits("100");
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Document.class), eq(INDEX)))
                .thenReturn(semanticResponse, keywordResponse);

        // 调用并断言
        var result = service.search("知识检索", new float[512], List.of(2L), List.of(5L), "HYBRID", 20);
        assertEquals(List.of(100L, 101L), result.stream().map(DocIndexHit::chunkId).toList());
        assertEquals("HYBRID", result.getFirst().matchType());
        assertEquals("SEMANTIC", result.get(1).matchType());
        assertEquals(0.032522, result.getFirst().score(), 0.000001);
        ArgumentCaptor<NativeQuery> query = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations, times(2)).search(query.capture(), eq(Document.class), eq(INDEX));
        var semantic = query.getAllValues().getFirst();
        var keyword = query.getAllValues().getLast();
        var filters = semantic.getKnnSearches().getFirst().filter();
        assertEquals("kb_id", filters.getFirst().terms().field());
        assertEquals(List.of("2"), filters.getFirst().terms().terms().value().stream().map(value -> value.stringValue()).toList());
        assertEquals("doc_id", filters.getLast().terms().field());
        assertEquals(2, keyword.getQuery().bool().filter().size());
        assertEquals("ik_smart", keyword.getQuery().bool().must().getFirst().match().analyzer());
        assertEquals(20, semantic.getMaxResults());
        assertFalse(semantic.getSourceFilter().fetchSource());
    }

    @Test
    void testSearchFuzzy_usesOnlySemanticQuery() {
        // 准备参数
        SearchHits<Document> response = hits("101");
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Document.class), eq(INDEX)))
                .thenReturn(response);

        // 调用并断言
        var result = service.search("知识检索", new float[512], List.of(2L), List.of(5L), "FUZZY", 20);
        assertEquals("SEMANTIC", result.getFirst().matchType());
        verify(elasticsearchOperations).search(any(NativeQuery.class), eq(Document.class), eq(INDEX));
    }

    @Test
    void testSearch_partialShardFailureRejected() {
        // 准备参数
        SearchHits<Document> response = hits();
        when(response.getSearchShardStatistics()).thenReturn(SearchShardStatistics.of(1, 0, 1, 0, List.of()));
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Document.class), eq(INDEX))).thenReturn(response);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.search("知识检索", new float[512], List.of(2L), List.of(5L), "FUZZY", 20));
        assertEquals(ErrorCodeConstants.DOC_SEARCH_FAILED.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testSearch_requestFailureRejected() {
        // 准备参数：连接错误、超时等失败由 Spring Data 抛出。
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(Document.class), eq(INDEX)))
                .thenThrow(new DataAccessResourceFailureException("检索超时"));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.search("知识检索", new float[512], List.of(2L), List.of(5L), "HYBRID", 20));
        assertEquals(ErrorCodeConstants.DOC_SEARCH_FAILED.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testSearch_emptyPermissionScopeNeverQueriesEs() {
        // 调用并断言：不允许省略权限过滤后执行全索引查询。
        assertTrue(service.search("知识检索", new float[512], List.of(), List.of(5L), "HYBRID", 20).isEmpty());
        assertTrue(service.search("知识检索", new float[512], List.of(2L), List.of(), "HYBRID", 20).isEmpty());
        verifyNoInteractions(elasticsearchOperations);
    }

    private static DocChunkDO chunk() {
        DocChunkDO chunk = new DocChunkDO().setDocId(5L).setChunkIndex(1).setText("知识检索")
                .setStartOffset(400).setEndOffset(404).setPageNumber(2);
        chunk.setId(100L);
        return chunk;
    }

    @SuppressWarnings("unchecked")
    private static SearchHits<Document> hits(String... ids) {
        SearchHits<Document> response = mock(SearchHits.class);
        List<SearchHit<Document>> result = java.util.Arrays.stream(ids).map(id ->
                new SearchHit<Document>("knowledge_doc_chunk", id, null, 1, null,
                        null, null, null, null, null, Document.create())).toList();
        org.mockito.Mockito.lenient().when(response.getSearchHits()).thenReturn(result);
        return response;
    }
}
