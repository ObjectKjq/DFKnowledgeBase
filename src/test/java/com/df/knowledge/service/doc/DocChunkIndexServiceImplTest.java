package com.df.knowledge.service.doc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DocChunkIndexServiceImpl} 的服务层测试，使用 ES 客户端桩，不连接真实 ES
 */
@ExtendWith(MockitoExtension.class)
class DocChunkIndexServiceImplTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;
    @Mock
    private ElasticsearchIndicesClient indicesClient;
    @InjectMocks
    private DocChunkIndexServiceImpl docChunkIndexService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(docChunkIndexService, "indexName", "knowledge_doc_chunk");
    }

    @Test
    void testWriteBatch_preservesIndexContractAndWaitsForRefresh() throws Exception {
        // 准备参数：带定位信息的分块和已向量化结果
        DocChunkDO chunk = chunk();
        float[] vector = new float[512];
        vector[0] = 1;
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse(false));

        // 调用
        docChunkIndexService.writeBatch(2L, List.of(chunk), List.of(vector));

        // 断言：ES 编号幂等，字段与 DSL 一致，返回前等待检索可见
        ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
        verify(elasticsearchClient).bulk(captor.capture());
        BulkRequest request = captor.getValue();
        assertEquals("knowledge_doc_chunk", request.index());
        assertEquals(Refresh.WaitFor, request.refresh());
        assertEquals(1, request.operations().size());
        var operation = request.operations().getFirst().index();
        assertEquals("100", operation.id());
        Map<?, ?> source = (Map<?, ?>) operation.document();
        assertEquals("2", source.get("kb_id"));
        assertEquals("5", source.get("doc_id"));
        assertEquals("100", source.get("chunk_id"));
        assertEquals(1, source.get("chunk_index"));
        assertEquals("知识检索", source.get("text"));
        assertEquals(400, source.get("start_offset"));
        assertEquals(404, source.get("end_offset"));
        assertEquals(2, source.get("page_number"));
        assertArrayEquals(vector, (float[]) source.get("vector"));
    }

    @Test
    void testWriteBatch_httpSuccessWithItemFailureRejected() throws Exception {
        // 准备参数：HTTP 成功但响应中有分块写入错误
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse(true));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> docChunkIndexService.writeBatch(2L, List.of(chunk()), List.of(new float[512])));
        assertEquals(ErrorCodeConstants.DOC_INDEX_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(exception.getMessage().contains("向量维度错误"));
    }

    @Test
    void testDeleteByDocId_refreshesAndDeletesOnlyTargetDocument() throws Exception {
        // 准备参数：只保留客户端的请求构建逻辑，外部删除响应由桩返回
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(elasticsearchClient.deleteByQuery(any(Function.class))).thenCallRealMethod();
        when(elasticsearchClient.deleteByQuery(any(DeleteByQueryRequest.class))).thenReturn(
                DeleteByQueryResponse.of(builder -> builder.timedOut(false).versionConflicts(0L).failures(List.of())));

        // 调用
        docChunkIndexService.deleteByDocId(5L);

        // 断言：删除前刷新快照，删除后立即不可检索，不误删其他文档
        verify(indicesClient).refresh(any(Function.class));
        ArgumentCaptor<DeleteByQueryRequest> captor = ArgumentCaptor.forClass(DeleteByQueryRequest.class);
        verify(elasticsearchClient).deleteByQuery(captor.capture());
        DeleteByQueryRequest request = captor.getValue();
        assertEquals(List.of("knowledge_doc_chunk"), request.index());
        assertEquals("doc_id", request.query().term().field());
        assertEquals("5", request.query().term().value().stringValue());
        assertEquals(Boolean.TRUE, request.refresh());
        assertEquals(Boolean.TRUE, request.waitForCompletion());
    }

    @Test
    void testDeleteByDocId_timeoutRejected() throws Exception {
        // 准备参数：删除请求超时，不能当作清理成功
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
        when(elasticsearchClient.deleteByQuery(any(Function.class))).thenReturn(
                DeleteByQueryResponse.of(builder -> builder.timedOut(true).versionConflicts(0L).failures(List.of())));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docChunkIndexService.deleteByDocId(5L));
        assertEquals(ErrorCodeConstants.DOC_INDEX_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(exception.getMessage().contains("未完全删除"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSearchHybrid_usesSingleRrfRequestWithBothScopeFilters() throws Exception {
        // 准备参数：ES 响应按融合相关性返回两个分块
        SearchResponse<Void> response = mock(SearchResponse.class);
        HitsMetadata<Void> hits = mock(HitsMetadata.class);
        Hit<Void> first = mock(Hit.class);
        Hit<Void> second = mock(Hit.class);
        when(first.id()).thenReturn("101");
        when(second.id()).thenReturn("100");
        when(hits.hits()).thenReturn(List.of(first, second));
        when(response.hits()).thenReturn(hits);
        when(elasticsearchClient.search(any(SearchRequest.class), eq(Void.class))).thenReturn(response);
        float[] vector = new float[512];
        vector[0] = 1;

        // 调用
        List<Long> chunkIds = docChunkIndexService.searchHybrid("知识检索", vector, List.of(2L), List.of(5L), 20);

        // 断言：单次请求，根过滤同时作用于 BM25 和 kNN，两路结果保留融合顺序
        assertEquals(List.of(101L, 100L), chunkIds);
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(elasticsearchClient).search(captor.capture(), eq(Void.class));
        SearchRequest request = captor.getValue();
        assertEquals(List.of("knowledge_doc_chunk"), request.index());
        assertEquals(20, request.size());
        assertEquals(Boolean.FALSE, request.source().fetch());
        var rrf = request.retriever().rrf();
        assertEquals(2, rrf.filter().size());
        assertEquals("kb_id", rrf.filter().get(0).terms().field());
        assertEquals("2", rrf.filter().get(0).terms().terms().value().getFirst().stringValue());
        assertEquals("doc_id", rrf.filter().get(1).terms().field());
        assertEquals("5", rrf.filter().get(1).terms().terms().value().getFirst().stringValue());
        assertEquals("ik_smart", rrf.retrievers().get(0).retriever().standard().query().match().analyzer());
        assertEquals("text", rrf.retrievers().get(0).retriever().standard().query().match().field());
        assertEquals("vector", rrf.retrievers().get(1).retriever().knn().field());
        assertEquals(512, rrf.retrievers().get(1).retriever().knn().queryVector().size());
    }

    private static DocChunkDO chunk() {
        DocChunkDO chunk = new DocChunkDO().setDocId(5L).setChunkIndex(1).setText("知识检索")
                .setStartOffset(400).setEndOffset(404).setPageNumber(2);
        chunk.setId(100L);
        return chunk;
    }

    private static BulkResponse bulkResponse(boolean errors) {
        return BulkResponse.of(builder -> builder.took(1L).errors(errors).items(item -> {
            item.operationType(OperationType.Index).index("knowledge_doc_chunk").id("100").status(errors ? 400 : 201);
            if (errors) {
                item.error(error -> error.type("mapper_parsing_exception").reason("向量维度错误"));
            }
            return item;
        }));
    }
}
