package com.df.knowledge.service.doc;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link DocIndexServiceImpl} 的服务层测试，向量化与分块索引为外部端口桩
 */
@ExtendWith(MockitoExtension.class)
class DocIndexServiceImplTest {

    @Mock
    private DocMapper docMapper;
    @Mock
    private DocChunkMapper docChunkMapper;
    @Mock
    private DocEmbeddingService docEmbeddingService;
    @Mock
    private DocChunkIndexService docChunkIndexService;
    @InjectMocks
    private DocIndexServiceImpl docIndexService;

    @Test
    void testIndex_batchesPreserveOrderAndBecomeAvailable() {
        // 准备参数：65 个分块需分三批，端口桩收集真实对外写入的数据
        DocDO doc = prepareDoc(65);
        List<Integer> batchSizes = new ArrayList<>();
        List<DocChunkDO> indexed = new ArrayList<>();
        List<float[]> indexedVectors = new ArrayList<>();
        when(docEmbeddingService.embedAll(any())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            batchSizes.add(texts.size());
            return texts.stream().map(text -> vector(Float.parseFloat(text))).toList();
        });
        doAnswer(invocation -> {
            assertEquals(2L, (Long) invocation.getArgument(0));
            indexed.addAll(invocation.getArgument(1));
            indexedVectors.addAll(invocation.getArgument(2));
            return null;
        }).when(docChunkIndexService).writeBatch(eq(2L), any(), any());

        // 调用
        docIndexService.index(5L);

        // 断言：按输入顺序批量向量化，分块定位不变，全部落索引后才可用
        assertEquals(List.of(32, 32, 1), batchSizes);
        assertEquals(65, indexed.size());
        assertEquals(100L, indexed.getFirst().getId());
        assertEquals(164L, indexed.getLast().getId());
        assertEquals(1, indexed.getFirst().getPageNumber());
        assertEquals(0, indexed.getFirst().getStartOffset());
        assertArrayEquals(vector(0), indexedVectors.getFirst());
        assertArrayEquals(vector(64), indexedVectors.getLast());
        assertEquals(DocStatusEnum.AVAILABLE.getCode(), doc.getStatus());
    }

    @Test
    void testProcessDoc_partialWriteFailureCleansAndRecoveryRebuilds() {
        // 准备参数：索引桩包含旧分块，第二批在写入一部分后失败
        DocDO doc = prepareDoc(33);
        Map<Long, float[]> indexed = new HashMap<>();
        indexed.put(999L, vector(99));
        AtomicBoolean fail = new AtomicBoolean(true);
        AtomicInteger writes = new AtomicInteger();
        when(docEmbeddingService.embedAll(any())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return texts.stream().map(text -> vector(Float.parseFloat(text))).toList();
        });
        doAnswer(invocation -> {
            indexed.clear();
            return null;
        }).when(docChunkIndexService).deleteByDocId(5L);
        doAnswer(invocation -> {
            List<DocChunkDO> chunks = invocation.getArgument(1);
            List<float[]> vectors = invocation.getArgument(2);
            for (int i = 0; i < chunks.size(); i++) {
                indexed.put(chunks.get(i).getId(), vectors.get(i));
            }
            if (writes.incrementAndGet() == 2 && fail.get()) {
                throw new RuntimeException("ES 写入超时");
            }
            return null;
        }).when(docChunkIndexService).writeBatch(eq(2L), any(), any());
        DocIngestServiceImpl ingest = ingestService();

        // 调用：真实索引编排的失败交回入库状态机
        ingest.processDoc(5L);

        // 断言：失败原因可读，失败批次以及旧索引均已清理
        assertEquals(DocStatusEnum.FAILED.getCode(), doc.getStatus());
        assertTrue(doc.getFailReason().contains("ES 写入超时"));
        assertTrue(indexed.isEmpty());
        String firstReason = doc.getFailReason();

        // 调用：模拟重试完成解析后或重启恢复到索引阶段，再执行两次也不会重复写入
        fail.set(false);
        doc.setStatus(DocStatusEnum.INDEXING.getCode());
        ingest.processDoc(5L);
        ingest.processDoc(5L);

        // 断言：恢复后全量重建成功，首次原因仍保留，旧分块编号不存在
        assertEquals(DocStatusEnum.AVAILABLE.getCode(), doc.getStatus());
        assertEquals(33, indexed.size());
        assertTrue(indexed.keySet().stream().allMatch(id -> id >= 100 && id <= 132));
        assertEquals(firstReason, doc.getFailReason());
    }

    @Test
    void testProcessDoc_embeddingFailureMarksFailedWithoutWriting() {
        // 准备参数：向量化端口失败
        DocDO doc = prepareDoc(1);
        when(docEmbeddingService.embedAll(List.of("0"))).thenThrow(new RuntimeException("模型加载失败"));

        // 调用
        ingestService().processDoc(5L);

        // 断言：不能写入索引或提前标记可用
        assertEquals(DocStatusEnum.FAILED.getCode(), doc.getStatus());
        assertTrue(doc.getFailReason().contains("模型加载失败"));
        verify(docChunkIndexService, never()).writeBatch(any(), any(), any());
    }

    @Test
    void testIndex_invalidDimensionRejected() {
        // 准备参数：模型结果不是协议约定的 512 维
        DocDO doc = prepareDoc(1);
        when(docEmbeddingService.embedAll(List.of("0"))).thenReturn(List.of(new float[511]));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docIndexService.index(5L));
        assertEquals(ErrorCodeConstants.DOC_EMBEDDING_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(exception.getMessage().contains("512"));
        assertEquals(DocStatusEnum.INDEXING.getCode(), doc.getStatus());
        verify(docChunkIndexService, never()).writeBatch(any(), any(), any());
    }

    @Test
    void testIndex_resultCountMismatchRejected() {
        // 准备参数：两个分块只返回一个向量，不能静默错配
        prepareDoc(2);
        when(docEmbeddingService.embedAll(any())).thenReturn(List.of(vector(0)));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docIndexService.index(5L));
        assertEquals(ErrorCodeConstants.DOC_EMBEDDING_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(exception.getMessage().contains("数量"));
        verify(docChunkIndexService, never()).writeBatch(any(), any(), any());
    }

    @Test
    void testIndex_emptyChunksNeverBecomeAvailable() {
        // 准备参数：索引中却缺少权威分块
        DocDO doc = prepareDoc(0);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docIndexService.index(5L));
        assertEquals(ErrorCodeConstants.DOC_INDEX_FAILED.getCode(), exception.getErrorCode().getCode());
        assertEquals(DocStatusEnum.INDEXING.getCode(), doc.getStatus());
        verifyNoInteractions(docEmbeddingService);
    }

    @Test
    void testIndex_deletedOrCompletedDocumentSkipped() {
        // 准备参数：排队后文档被删除，或重复任务处理已完成文档
        DocDO doc = prepareDoc(1);
        doc.setStatus(DocStatusEnum.DELETED.getCode());

        // 调用
        docIndexService.index(5L);
        doc.setStatus(DocStatusEnum.AVAILABLE.getCode());
        docIndexService.index(5L);

        // 断言：不能把已删除文档重新写回，也不能清理已成功文档的索引
        verifyNoInteractions(docEmbeddingService, docChunkIndexService);
    }

    private DocIngestServiceImpl ingestService() {
        DocIngestServiceImpl ingest = new DocIngestServiceImpl();
        ReflectionTestUtils.setField(ingest, "docMapper", docMapper);
        ReflectionTestUtils.setField(ingest, "docIndexService", docIndexService);
        return ingest;
    }

    private DocDO prepareDoc(int chunkCount) {
        DocDO doc = new DocDO();
        doc.setId(5L);
        doc.setKnowledgeBaseId(2L);
        doc.setStatus(DocStatusEnum.INDEXING.getCode());
        lenient().when(docMapper.selectByIdForUpdate(5L)).thenReturn(doc);
        lenient().when(docMapper.selectById(5L)).thenReturn(doc);
        List<DocChunkDO> chunks = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            DocChunkDO chunk = new DocChunkDO().setDocId(5L).setChunkIndex(i).setText(String.valueOf(i))
                    .setPageNumber(1).setStartOffset(i * 400).setEndOffset(i * 400 + 500);
            chunk.setId(100L + i);
            chunks.add(chunk);
        }
        lenient().when(docChunkMapper.selectListByDocId(5L)).thenReturn(chunks);
        lenient().when(docMapper.updateStatus(eq(5L), any(), any(), any())).thenAnswer(invocation -> {
            if (!doc.getStatus().equals(invocation.getArgument(1))) {
                return 0;
            }
            doc.setStatus(invocation.getArgument(2));
            if (invocation.getArgument(3) != null) {
                doc.setFailReason(invocation.getArgument(3));
            }
            return 1;
        });
        return doc;
    }

    private static float[] vector(float marker) {
        float[] vector = new float[512];
        vector[0] = 1;
        vector[1] = marker;
        return vector;
    }
}
