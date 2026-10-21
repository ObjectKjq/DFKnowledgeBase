package com.df.knowledge.service.doc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.common.PageResult;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.doc.vo.DocPageReqVO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.doc.DocConstants;
import com.df.knowledge.enums.doc.DocFormatEnum;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
    private DocFileService docFileService;

    @Mock
    private DocIngestService docIngestService;

    @Mock
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @InjectMocks
    private DocServiceImpl docService;

    @Test
    void testUploadDoc_success() {
        // 准备参数：编辑者上传 PDF 文件
        MultipartFile file = new MockMultipartFile("file", "期末复习笔记.pdf", "application/pdf", new byte[]{1, 2, 3});
        when(docFileService.store(1L, "pdf", file)).thenReturn("./data/docs/1/abc.pdf");
        // mock 插入回填自增主键
        doAnswer(invocation -> {
            DocDO doc = invocation.getArgument(0);
            doc.setId(5L);
            return 1;
        }).when(docMapper).insert(any(DocDO.class));

        // 调用
        Long docId = docService.uploadDoc(10L, 1L, file);

        // 断言：落盘后插入文档（状态排队）并提交入库任务
        assertEquals(5L, docId.longValue());
        ArgumentCaptor<DocDO> captor = ArgumentCaptor.forClass(DocDO.class);
        verify(docMapper).insert(captor.capture());
        DocDO inserted = captor.getValue();
        assertEquals(1L, inserted.getKnowledgeBaseId().longValue());
        assertEquals("期末复习笔记.pdf", inserted.getTitle());
        assertEquals("期末复习笔记.pdf", inserted.getFileName());
        assertEquals(DocFormatEnum.PDF.getCode(), inserted.getFormat());
        assertEquals(3L, inserted.getFileSize().longValue());
        assertEquals("./data/docs/1/abc.pdf", inserted.getFilePath());
        assertEquals(DocStatusEnum.QUEUED.getCode(), inserted.getStatus());
        verify(docIngestService).submit(5L);
    }

    @Test
    void testUploadDoc_formatNotSupported() {
        // 准备参数：白名单外的格式
        MultipartFile file = new MockMultipartFile("file", "病毒.exe", "application/octet-stream", new byte[]{1});

        // 调用并断言：上传前拦截并列明支持格式
        ServiceException exception = assertThrows(ServiceException.class,
                () -> docService.uploadDoc(10L, 1L, file));
        assertEquals(ErrorCodeConstants.DOC_FORMAT_NOT_SUPPORTED.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(docFileService, docMapper, docIngestService);
    }

    @Test
    void testUploadDoc_fileTooLarge() {
        // 准备参数：超过 50 MB 的文件
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("大文件.pdf");
        when(file.getSize()).thenReturn(DocConstants.MAX_FILE_SIZE + 1);

        // 调用并断言：超过大小上限被拦截
        ServiceException exception = assertThrows(ServiceException.class,
                () -> docService.uploadDoc(10L, 1L, file));
        assertEquals(ErrorCodeConstants.DOC_FILE_TOO_LARGE.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(docFileService, docMapper, docIngestService);
    }

    @Test
    void testUploadDoc_viewerNoPermission() {
        // 准备参数：查看者上传被权限判定拒绝
        MultipartFile file = new MockMultipartFile("file", "笔记.txt", "text/plain", new byte[]{1});
        doThrow(new ServiceException(ErrorCodeConstants.KB_NO_PERMISSION))
                .when(knowledgeBasePermissionService).checkContentWritePermission(20L, 1L);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> docService.uploadDoc(20L, 1L, file));
        assertEquals(ErrorCodeConstants.KB_NO_PERMISSION.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(docFileService, docMapper, docIngestService);
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
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用
        docService.retryDoc(10L, 5L);

        // 断言：回到排队重新执行，失败原因保留首次值，并重新提交入库任务
        ArgumentCaptor<DocDO> captor = ArgumentCaptor.forClass(DocDO.class);
        verify(docMapper).updateById(captor.capture());
        assertEquals(DocStatusEnum.QUEUED.getCode(), captor.getValue().getStatus());
        assertEquals("文件损坏", captor.getValue().getFailReason());
        verify(docIngestService).submit(5L);
    }

    @Test
    void testRetryDoc_notFailed() {
        // 准备参数：可用文档不支持重试
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.AVAILABLE);
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.retryDoc(10L, 5L));
        assertEquals(ErrorCodeConstants.DOC_RETRY_NOT_ALLOWED.getCode(), exception.getErrorCode().getCode());
        verify(docMapper, never()).updateById(any(DocDO.class));
    }

    @Test
    void testRetryDoc_noPermission() {
        // 准备参数：查看者重试被权限判定拒绝
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.FAILED);
        when(docMapper.selectById(5L)).thenReturn(doc);
        doThrow(new ServiceException(ErrorCodeConstants.KB_NO_PERMISSION))
                .when(knowledgeBasePermissionService).checkContentWritePermission(20L, 1L);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.retryDoc(20L, 5L));
        assertEquals(ErrorCodeConstants.KB_NO_PERMISSION.getCode(), exception.getErrorCode().getCode());
        verify(docIngestService, never()).submit(anyLong());
    }

    @Test
    void testDeleteDoc_success() {
        // 准备参数：删除可用文档
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.AVAILABLE);
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用
        docService.deleteDoc(10L, 5L);

        // 断言：软删除并记录删除前状态与删除时间
        ArgumentCaptor<DocDO> captor = ArgumentCaptor.forClass(DocDO.class);
        verify(docMapper).updateById(captor.capture());
        DocDO updated = captor.getValue();
        assertEquals(DocStatusEnum.DELETED.getCode(), updated.getStatus());
        assertEquals(DocStatusEnum.AVAILABLE.getCode(), updated.getPrevStatus());
        assertNotNull(updated.getDeleteTime());
    }

    @Test
    void testDeleteDoc_alreadyDeleted() {
        // 准备参数：已删除文档不可重复删除
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.DELETED);
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.deleteDoc(10L, 5L));
        assertEquals(ErrorCodeConstants.DOC_DELETE_NOT_ALLOWED.getCode(), exception.getErrorCode().getCode());
        verify(docMapper, never()).updateById(any(DocDO.class));
    }

    @Test
    void testRestoreDoc_success() {
        // 准备参数：30 天保留期内恢复删除前为「可用」的文档
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.DELETED);
        doc.setPrevStatus(DocStatusEnum.AVAILABLE.getCode());
        doc.setDeleteTime(LocalDateTime.now().minusDays(10));
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用
        docService.restoreDoc(10L, 5L);

        // 断言：恢复到删除前状态并清空删除标记
        ArgumentCaptor<DocDO> captor = ArgumentCaptor.forClass(DocDO.class);
        verify(docMapper).updateById(captor.capture());
        DocDO updated = captor.getValue();
        assertEquals(DocStatusEnum.AVAILABLE.getCode(), updated.getStatus());
        assertNull(updated.getPrevStatus());
        assertNull(updated.getDeleteTime());
        verify(docIngestService, never()).submit(anyLong());
    }

    @Test
    void testRestoreDoc_inFlightPrevStatus_resubmit() {
        // 准备参数：删除前仍在入库流程中（排队）的文档
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.DELETED);
        doc.setPrevStatus(DocStatusEnum.QUEUED.getCode());
        doc.setDeleteTime(LocalDateTime.now().minusDays(1));
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用
        docService.restoreDoc(10L, 5L);

        // 断言：恢复到删除前状态并重新入队继续处理
        verify(docIngestService).submit(5L);
    }

    @Test
    void testRestoreDoc_notDeleted() {
        // 准备参数：未删除的文档不支持恢复
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.AVAILABLE);
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.restoreDoc(10L, 5L));
        assertEquals(ErrorCodeConstants.DOC_RESTORE_NOT_ALLOWED.getCode(), exception.getErrorCode().getCode());
        verify(docMapper, never()).updateById(any(DocDO.class));
    }

    @Test
    void testRestoreDoc_expired() {
        // 准备参数：删除超过 30 天保留期
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.DELETED);
        doc.setPrevStatus(DocStatusEnum.AVAILABLE.getCode());
        doc.setDeleteTime(LocalDateTime.now().minusDays(31));
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用并断言：超期不可恢复
        ServiceException exception = assertThrows(ServiceException.class, () -> docService.restoreDoc(10L, 5L));
        assertEquals(ErrorCodeConstants.DOC_RESTORE_EXPIRED.getCode(), exception.getErrorCode().getCode());
        verify(docMapper, never()).updateById(any(DocDO.class));
    }

    @Test
    void testPurgeExpiredDeletedDocs_success() {
        // 准备参数：超过保留期的软删除文档
        DocDO doc = buildDoc(5L, 1L, DocStatusEnum.DELETED);
        doc.setFilePath("./data/docs/1/abc.pdf");
        when(docMapper.selectListByStatusAndDeleteTimeBefore(any(), any(LocalDateTime.class))).thenReturn(List.of(doc));

        // 调用
        docService.purgeExpiredDeletedDocs();

        // 断言：移除记录与存储文件
        verify(docMapper).deleteById(5L);
        verify(docChunkMapper).deleteByDocId(5L);
        verify(docFileService).delete("./data/docs/1/abc.pdf");
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
        doc.setFilePath("./data/docs/1/abc.pdf");
        doc.setStatus(status.getCode());
        return doc;
    }
}
