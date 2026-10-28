package com.df.knowledge.service.doc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.doc.vo.DocCreateReqVO;
import com.df.knowledge.controller.doc.vo.DocPageReqVO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.dataobject.file.FileDO;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.doc.DocFormatEnum;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.file.FileService;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link DocServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class DocServiceImplTest {

    @Mock
    private DocMapper docMapper;

    @Mock
    private DocChunkMapper docChunkMapper;

    @Mock
    private DocChunkIndexService docChunkIndexService;

    @Mock
    private FileService fileService;

    @Mock
    private DocIngestService docIngestService;

    @Mock
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @InjectMocks
    private DocServiceImpl docService;

    @Test
    void testCreateDoc_success() {
        // 准备参数：编辑者创建文档（文件已通过文件上传接口落库）
        DocCreateReqVO reqVO = buildCreateReqVO();
        when(fileService.getAttachableFile(10L, 1L)).thenReturn(buildFile(1L, "期末复习笔记.pdf"));
        when(fileService.buildDownloadUrl(1L)).thenReturn("/api/system/file/download?id=1");
        // mock 插入回填自增主键
        doAnswer(invocation -> {
            DocDO doc = invocation.getArgument(0);
            doc.setId(5L);
            return 1;
        }).when(docMapper).insert(any(DocDO.class));

        // 调用
        Long docId = docService.createDoc(10L, reqVO);

        // 断言：插入文档（状态排队）保存文件引用，等待定时任务扫描
        assertEquals(5L, docId.longValue());
        ArgumentCaptor<DocDO> captor = ArgumentCaptor.forClass(DocDO.class);
        verify(docMapper).insert(captor.capture());
        DocDO inserted = captor.getValue();
        assertEquals(1L, inserted.getKnowledgeBaseId().longValue());
        assertEquals("期末复习笔记.pdf", inserted.getTitle());
        assertEquals("期末复习笔记.pdf", inserted.getFileName());
        assertEquals(DocFormatEnum.PDF.getCode(), inserted.getFormat());
        assertEquals(3L, inserted.getFileSize().longValue());
        assertEquals(1L, inserted.getFileId().longValue());
        assertEquals("/api/system/file/download?id=1", inserted.getFileUrl());
        assertEquals(DocStatusEnum.QUEUED.getCode(), inserted.getStatus());
        verifyNoInteractions(docIngestService);
    }

    @Test
    void testCreateDoc_fileNotFound() {
        // 准备参数：文件引用不存在
        DocCreateReqVO reqVO = buildCreateReqVO();
        when(fileService.getAttachableFile(10L, 1L)).thenReturn(null);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.createDoc(10L, reqVO));
        assertEquals(ErrorCodeConstants.FILE_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(docMapper, docIngestService);
    }

    @Test
    void testCreateDoc_formatNotSupported() {
        // 准备参数：白名单外的格式
        DocCreateReqVO reqVO = buildCreateReqVO();
        when(fileService.getAttachableFile(10L, 1L)).thenReturn(buildFile(1L, "病毒.exe"));

        // 调用并断言：创建前拦截并列明支持格式
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.createDoc(10L, reqVO));
        assertEquals(ErrorCodeConstants.DOC_FORMAT_NOT_SUPPORTED.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(docMapper, docIngestService);
    }

    @Test
    void testCreateDoc_viewerNoPermission() {
        // 准备参数：查看者创建被权限判定拒绝
        DocCreateReqVO reqVO = buildCreateReqVO();
        doThrow(new ServiceException(ErrorCodeConstants.KB_NO_PERMISSION))
                .when(knowledgeBasePermissionService).checkContentWritePermission(20L, 1L);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.createDoc(20L, reqVO));
        assertEquals(ErrorCodeConstants.KB_NO_PERMISSION.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(fileService, docMapper, docIngestService);
    }

    @Test
    void testGetDocPage_success() {
        // 准备参数：仅在有权访问的知识库范围内分页
        DocPageReqVO reqVO = new DocPageReqVO();
        when(knowledgeBasePermissionService.getAccessibleKnowledgeBaseIds(10L)).thenReturn(List.of(1L, 2L));
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.AVAILABLE);
        Page<DocDO> page = new Page<>(1L, 10L);
        page.setRecords(List.of(doc));
        page.setTotal(1L);
        when(docMapper.selectPage(reqVO, List.of(1L, 2L))).thenReturn(page);

        // 调用并断言
        PageResult<DocDO> result = docService.getDocPage(10L, reqVO);
        assertEquals(1L, result.getTotal().longValue());
        assertEquals(1, result.getList().size());
        assertEquals(5L, result.getList().get(0).getId().longValue());
    }

    @Test
    void testGetDocPage_noAccessible_empty() {
        // 准备参数：无任何有权访问的知识库
        DocPageReqVO reqVO = new DocPageReqVO();
        when(knowledgeBasePermissionService.getAccessibleKnowledgeBaseIds(10L)).thenReturn(List.of());

        // 调用并断言：直接返回空分页，不查询数据库
        PageResult<DocDO> result = docService.getDocPage(10L, reqVO);
        assertEquals(0L, result.getTotal().longValue());
        assertTrue(result.getList().isEmpty());
    }

    @Test
    void testGetDoc_success() {
        // 准备参数
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.AVAILABLE);
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用并断言：有权访问时返回文档
        DocDO result = docService.getDoc(10L, 5L);
        assertEquals(5L, result.getId().longValue());
    }

    @Test
    void testGetDoc_notFound() {
        // 准备参数：文档不存在
        when(docMapper.selectById(5L)).thenReturn(null);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.getDoc(10L, 5L));
        assertEquals(ErrorCodeConstants.DOC_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testRetryDoc_success() {
        // 准备参数：失败文档重试
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.FAILED);
        doc.setFailReason("文件损坏");
        when(docMapper.selectByIdForUpdate(5L)).thenReturn(doc);

        // 调用
        docService.retryDoc(10L, 5L);

        // 断言：回到排队重新执行，失败原因保留首次值，等待定时任务扫描
        ArgumentCaptor<DocDO> captor = ArgumentCaptor.forClass(DocDO.class);
        verify(docMapper).updateById(captor.capture());
        assertEquals(DocStatusEnum.QUEUED.getCode(), captor.getValue().getStatus());
        assertEquals("文件损坏", captor.getValue().getFailReason());
        verifyNoInteractions(docIngestService);
    }

    @Test
    void testRetryDoc_notFailed() {
        // 准备参数：可用文档不支持重试
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.AVAILABLE);
        when(docMapper.selectByIdForUpdate(5L)).thenReturn(doc);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.retryDoc(10L, 5L));
        assertEquals(ErrorCodeConstants.DOC_RETRY_NOT_ALLOWED.getCode(), exception.getErrorCode().getCode());
        verify(docMapper, never()).updateById(any(DocDO.class));
    }

    @Test
    void testRetryDoc_noPermission() {
        // 准备参数：查看者重试被权限判定拒绝
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.FAILED);
        when(docMapper.selectByIdForUpdate(5L)).thenReturn(doc);
        doThrow(new ServiceException(ErrorCodeConstants.KB_NO_PERMISSION))
                .when(knowledgeBasePermissionService).checkContentWritePermission(20L, 1L);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.retryDoc(20L, 5L));
        assertEquals(ErrorCodeConstants.KB_NO_PERMISSION.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(docIngestService);
    }

    @Test
    void testDeleteDoc_success() {
        // 准备参数：删除可用文档
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.AVAILABLE);
        when(docMapper.selectByIdForUpdate(5L)).thenReturn(doc);

        // 调用
        docService.deleteDoc(10L, 5L);

        // 断言：清理索引、分块、文档及不再引用的文件。
        verify(docChunkIndexService).deleteByDocId(5L);
        verify(docChunkMapper).deleteByDocId(5L);
        verify(docMapper).purgeById(5L);
        verify(fileService).delete(1L);
    }

    @Test
    void testDeleteDoc_indexCleanupFailsKeepsDocument() {
        // 准备参数：ES 清理失败，数据库文档不能被当作已删除
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.AVAILABLE);
        when(docMapper.selectByIdForUpdate(5L)).thenReturn(doc);
        doThrow(new ServiceException(ErrorCodeConstants.DOC_INDEX_FAILED))
                .when(docChunkIndexService).deleteByDocId(5L);

        // 调用并断言：操作失败后保留原状态，用户可以重试删除
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.deleteDoc(10L, 5L));
        assertEquals(ErrorCodeConstants.DOC_INDEX_FAILED.getCode(), exception.getErrorCode().getCode());
        assertEquals(DocStatusEnum.AVAILABLE.getCode(), doc.getStatus());
        verify(docMapper, never()).purgeById(anyLong());
        verifyNoInteractions(docChunkMapper, fileService);
    }

    /**
     * 构造创建文档 Request VO
     */
    private static DocCreateReqVO buildCreateReqVO() {
        DocCreateReqVO reqVO = new DocCreateReqVO();
        reqVO.setKnowledgeBaseId(1L);
        reqVO.setFileId(1L);
        reqVO.setFileUrl("/api/system/file/download?id=1");
        return reqVO;
    }

    /**
     * 构造文件 DO
     */
    private static FileDO buildFile(Long id, String name) {
        FileDO file = new FileDO();
        file.setId(id);
        file.setName(name);
        file.setType("application/pdf");
        file.setSize(3L);
        return file;
    }

    /**
     * 构造文档 DO
     */
    private static DocDO buildDoc(Long id, Long knowledgeBaseId, DocStatusEnum status) {
        DocDO doc = new DocDO();
        doc.setId(id);
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setTitle("期末复习笔记.pdf");
        doc.setFileName("期末复习笔记.pdf");
        doc.setFormat(DocFormatEnum.PDF.getCode());
        doc.setFileSize(1024L);
        doc.setFileId(1L);
        doc.setFileUrl("/api/system/file/download?id=1");
        doc.setStatus(status.getCode());
        return doc;
    }
}
