package com.df.knowledge.service.doc;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link DocSearchServiceImpl} 的服务层测试，分块索引端口使用桩
 */
@ExtendWith(MockitoExtension.class)
class DocSearchServiceImplTest {

    @Mock private KnowledgeBasePermissionService permissionService;
    @Mock private KnowledgeBaseMapper knowledgeBaseMapper;
    @Mock private DocMapper docMapper;
    @Mock private DocChunkMapper docChunkMapper;
    @Mock private DocEmbeddingService embeddingService;
    @Mock private DocChunkIndexService chunkIndexService;
    @InjectMocks private DocSearchServiceImpl searchService;

    @Test
    void testSearch_filtersScopeAndAvailableDocumentsAndKeepsRankOrder() {
        // 准备参数：请求包含无权库，索引桩返回已失败文档的残留分块
        DocDO available = doc(5L, 2L, DocStatusEnum.AVAILABLE.getCode());
        DocDO failed = doc(6L, 2L, DocStatusEnum.FAILED.getCode());
        DocChunkDO first = chunk(101L, 5L, 1);
        DocChunkDO second = chunk(100L, 5L, 0);
        DocChunkDO stale = chunk(102L, 6L, 0);
        float[] vector = new float[512];
        vector[0] = 1;
        when(permissionService.getAccessibleKnowledgeBaseIds(7L)).thenReturn(List.of(2L, 3L));
        when(docMapper.selectListByKnowledgeBaseIdsAndStatus(List.of(2L), DocStatusEnum.AVAILABLE.getCode()))
                .thenReturn(List.of(available));
        when(embeddingService.embedAll(List.of("检索内容"))).thenReturn(List.of(vector));
        when(chunkIndexService.searchHybrid(eq("检索内容"), eq(vector), eq(List.of(2L)), eq(List.of(5L)), eq(20)))
                .thenReturn(List.of(101L, 102L, 100L));
        when(docChunkMapper.selectBatchIds(List.of(101L, 102L, 100L))).thenReturn(List.of(second, stale, first));
        when(docMapper.selectBatchIds(List.of(5L, 6L))).thenReturn(List.of(available, failed));
        when(knowledgeBaseMapper.selectBatchIds(List.of(2L))).thenReturn(List.of(knowledgeBase()));

        // 调用
        List<DocChunkResult> results = searchService.search(7L, " 检索内容 ", List.of(2L, 99L));

        // 断言：无权范围未传给 ES，残留分块未暴露，融合排序不被数据库查询顺序覆盖
        assertEquals(List.of(101L, 100L), results.stream().map(DocChunkResult::getChunkId).toList());
        assertEquals("我的知识库", results.getFirst().getKnowledgeBaseName());
        assertEquals("文档 5", results.getFirst().getDocTitle());
        assertEquals("分块 1", results.getFirst().getText());
        verify(chunkIndexService).searchHybrid(eq("检索内容"), eq(vector), eq(List.of(2L)), eq(List.of(5L)), eq(20));
    }

    @Test
    void testSearch_noAccessibleScopeReturnsEmptyWithoutCallingIndex() {
        // 准备参数
        when(permissionService.getAccessibleKnowledgeBaseIds(7L)).thenReturn(List.of(2L));

        // 调用并断言：无权库不能通过结果或索引查询计数泄露存在性
        assertEquals(List.of(), searchService.search(7L, "检索内容", List.of(99L)));
        verifyNoInteractions(docMapper, docChunkMapper, embeddingService, chunkIndexService);
    }

    @Test
    void testGetPreview_returnsOriginalLocationForAvailableDocument() {
        // 准备参数
        DocChunkDO chunk = chunk(101L, 5L, 1).setPageNumber(3).setStartOffset(400).setEndOffset(420);
        when(docChunkMapper.selectById(101L)).thenReturn(chunk);
        when(docMapper.selectById(5L)).thenReturn(doc(5L, 2L, DocStatusEnum.AVAILABLE.getCode()));
        when(knowledgeBaseMapper.selectById(2L)).thenReturn(knowledgeBase());

        // 调用
        DocChunkResult result = searchService.getPreview(7L, 101L);

        // 断言
        assertEquals(101L, result.getChunkId());
        assertEquals(3, result.getPageNumber());
        assertEquals(1, result.getChunkIndex());
        assertEquals(400, result.getStartOffset());
        assertEquals("分块 1", result.getText());
        verify(permissionService).checkViewPermission(7L, 2L);
    }

    @Test
    void testGetPreview_deletedDocumentDoesNotExposeOriginalText() {
        // 准备参数
        when(docChunkMapper.selectById(101L)).thenReturn(chunk(101L, 5L, 1));
        when(docMapper.selectById(5L)).thenReturn(doc(5L, 2L, DocStatusEnum.DELETED.getCode()));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> searchService.getPreview(7L, 101L));
        assertEquals(ErrorCodeConstants.DOC_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
        ServiceException missing = assertThrows(ServiceException.class, () -> searchService.getPreview(7L, 999L));
        assertEquals(missing.getErrorCode().getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(knowledgeBaseMapper);
    }

    @Test
    void testGetPreview_noViewPermissionDoesNotReturnOriginalText() {
        // 准备参数：分块属于当前用户已无权访问的知识库
        when(docChunkMapper.selectById(101L)).thenReturn(chunk(101L, 5L, 1));
        when(docMapper.selectById(5L)).thenReturn(doc(5L, 2L, DocStatusEnum.AVAILABLE.getCode()));
        doThrow(new ServiceException(ErrorCodeConstants.KB_NOT_FOUND))
                .when(permissionService).checkViewPermission(7L, 2L);

        // 调用并断言：无权分块与未知分块返回同一错误，不泄露存在性
        ServiceException exception = assertThrows(ServiceException.class, () -> searchService.getPreview(7L, 101L));
        assertEquals(ErrorCodeConstants.DOC_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(knowledgeBaseMapper);
    }

    private static DocDO doc(Long id, Long kbId, Integer status) {
        DocDO doc = new DocDO().setKnowledgeBaseId(kbId).setTitle("文档 " + id).setStatus(status);
        doc.setId(id);
        return doc;
    }

    private static DocChunkDO chunk(Long id, Long docId, int index) {
        DocChunkDO chunk = new DocChunkDO().setDocId(docId).setChunkIndex(index).setText("分块 " + index);
        chunk.setId(id);
        return chunk;
    }

    private static KnowledgeBaseDO knowledgeBase() {
        KnowledgeBaseDO knowledgeBase = new KnowledgeBaseDO().setName("我的知识库");
        knowledgeBase.setId(2L);
        return knowledgeBase;
    }
}
