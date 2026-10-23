package com.df.knowledge.service.doc;

import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.enums.doc.DocStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link DocIngestServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class DocIngestServiceImplTest {

    @Mock
    private DocMapper docMapper;

    @Mock
    private DocParseService docParseService;

    @Mock
    private DocIndexService docIndexService;

    @Mock
    private ThreadPoolTaskExecutor docIngestExecutor;

    @Mock
    private com.df.knowledge.service.model.InstanceSettingsService instanceSettingsService;

    @InjectMocks
    private DocIngestServiceImpl docIngestService;

    @Test
    void testProcessDoc_success() {
        // 准备参数：排队中的文档，状态迁移均成功
        DocDO doc = buildDoc(5L, DocStatusEnum.QUEUED, null);
        when(docMapper.selectById(5L)).thenReturn(doc);
        when(docMapper.updateStatus(5L, DocStatusEnum.QUEUED.getCode(), DocStatusEnum.PARSING.getCode(), null)).thenReturn(1);
        when(docMapper.updateStatus(5L, DocStatusEnum.PARSING.getCode(), DocStatusEnum.INDEXING.getCode(), null)).thenReturn(1);

        // 调用
        docIngestService.processDoc(5L);

        // 断言：排队 → 解析中 → 索引中，交接给索引服务发布可用状态
        verify(docMapper).updateStatus(5L, DocStatusEnum.QUEUED.getCode(), DocStatusEnum.PARSING.getCode(), null);
        verify(docMapper).updateStatus(5L, DocStatusEnum.PARSING.getCode(), DocStatusEnum.INDEXING.getCode(), null);
        verify(docParseService).parse(5L);
        verify(docIndexService).index(5L);
    }

    @Test
    void testProcessDoc_parseFailed_firstFailReason() {
        // 准备参数：解析阶段异常，文档尚无失败原因
        DocDO doc = buildDoc(5L, DocStatusEnum.QUEUED, null);
        when(docMapper.selectById(5L)).thenReturn(doc);
        when(docMapper.updateStatus(5L, DocStatusEnum.QUEUED.getCode(), DocStatusEnum.PARSING.getCode(), null)).thenReturn(1);
        doThrow(new RuntimeException("文件损坏")).when(docParseService).parse(5L);

        // 调用
        docIngestService.processDoc(5L);

        // 断言：迁移到失败并记录首次失败原因，不执行索引阶段
        verify(docMapper).updateStatus(5L, DocStatusEnum.PARSING.getCode(), DocStatusEnum.FAILED.getCode(), "文件损坏");
        verify(docIndexService, never()).index(anyLong());
    }

    @Test
    void testProcessDoc_indexFailed_keepFirstFailReason() {
        // 准备参数：索引阶段异常，文档已有首次失败原因（重试后再次失败）
        DocDO doc = buildDoc(5L, DocStatusEnum.PARSING, "扫描质量过低");
        when(docMapper.selectById(5L)).thenReturn(doc);
        when(docMapper.updateStatus(5L, DocStatusEnum.PARSING.getCode(), DocStatusEnum.INDEXING.getCode(), null)).thenReturn(1);
        doThrow(new RuntimeException("索引超时")).when(docIndexService).index(5L);

        // 调用
        docIngestService.processDoc(5L);

        // 断言：迁移到失败但保留首次失败原因
        verify(docMapper).updateStatus(5L, DocStatusEnum.INDEXING.getCode(), DocStatusEnum.FAILED.getCode(), "扫描质量过低");
    }

    @Test
    void testProcessDoc_resumeParsing_skipSelfTransition() {
        // 准备参数：重启后停留在解析中的文档，续跑解析（不自迁移）
        DocDO doc = buildDoc(5L, DocStatusEnum.PARSING, null);
        when(docMapper.selectById(5L)).thenReturn(doc);
        when(docMapper.updateStatus(5L, DocStatusEnum.PARSING.getCode(), DocStatusEnum.INDEXING.getCode(), null)).thenReturn(1);

        // 调用
        docIngestService.processDoc(5L);

        // 断言：解析与索引都执行，本层只迁移到索引中，可用状态由索引服务发布
        verify(docParseService).parse(5L);
        verify(docIndexService).index(5L);
        verify(docMapper, never()).updateStatus(5L, DocStatusEnum.PARSING.getCode(), DocStatusEnum.PARSING.getCode(), null);
        verify(docMapper).updateStatus(5L, DocStatusEnum.PARSING.getCode(), DocStatusEnum.INDEXING.getCode(), null);
    }

    @Test
    void testProcessDoc_resumeIndexing_skipParse() {
        // 准备参数：重启后停留在索引中的文档，解析已完成，只续跑索引
        DocDO doc = buildDoc(5L, DocStatusEnum.INDEXING, null);
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用
        docIngestService.processDoc(5L);

        // 断言：不重复解析，交由索引服务续跑并发布可用状态
        verify(docParseService, never()).parse(anyLong());
        verify(docIndexService).index(5L);
    }

    @Test
    void testProcessDoc_docMissing_skip() {
        // 准备参数：文档不存在（已彻底删除）
        when(docMapper.selectById(5L)).thenReturn(null);

        // 调用
        docIngestService.processDoc(5L);

        // 断言：不迁移状态、不执行任何加工
        verify(docMapper, never()).updateStatus(any(), any(), any(), any());
        verifyNoInteractions(docParseService, docIndexService);
    }

    @Test
    void testProcessDoc_docDeleted_skip() {
        // 准备参数：文档已被软删除
        DocDO doc = buildDoc(5L, DocStatusEnum.DELETED, null);
        when(docMapper.selectById(5L)).thenReturn(doc);

        // 调用
        docIngestService.processDoc(5L);

        // 断言：已删除文档不回到入库流程
        verify(docMapper, never()).updateStatus(any(), any(), any(), any());
        verifyNoInteractions(docParseService, docIndexService);
    }

    @Test
    void testProcessDoc_statusChangedBeforeParse_skip() {
        // 准备参数：提交后状态被并发变更（如删除），CAS 迁移失败
        DocDO doc = buildDoc(5L, DocStatusEnum.QUEUED, null);
        when(docMapper.selectById(5L)).thenReturn(doc);
        when(docMapper.updateStatus(5L, DocStatusEnum.QUEUED.getCode(), DocStatusEnum.PARSING.getCode(), null)).thenReturn(0);

        // 调用
        docIngestService.processDoc(5L);

        // 断言：跳过整条入库流程
        verifyNoInteractions(docParseService, docIndexService);
    }

    @Test
    void testProcessDoc_statusChangedDuringParse_skipIndex() {
        // 准备参数：解析期间文档被软删除，解析中 → 索引中 的 CAS 迁移失败
        DocDO doc = buildDoc(5L, DocStatusEnum.QUEUED, null);
        when(docMapper.selectById(5L)).thenReturn(doc);
        when(docMapper.updateStatus(5L, DocStatusEnum.QUEUED.getCode(), DocStatusEnum.PARSING.getCode(), null)).thenReturn(1);
        when(docMapper.updateStatus(5L, DocStatusEnum.PARSING.getCode(), DocStatusEnum.INDEXING.getCode(), null)).thenReturn(0);

        // 调用
        docIngestService.processDoc(5L);

        // 断言：解析完成也不再索引，避免把已删除文档拉回流程
        verify(docParseService).parse(5L);
        verify(docIndexService, never()).index(anyLong());
    }

    @Test
    void testRecoverInterruptedDocs_success() {
        // 准备参数：重启后停留在入库进行中的文档
        DocDO queued = buildDoc(5L, DocStatusEnum.QUEUED, null);
        DocDO parsing = buildDoc(6L, DocStatusEnum.PARSING, null);
        DocDO indexing = buildDoc(7L, DocStatusEnum.INDEXING, null);
        when(docMapper.selectListByStatuses(DocStatusEnum.IN_FLIGHT_STATUS_CODES)).thenReturn(List.of(queued, parsing, indexing));

        // 调用
        when(instanceSettingsService.get()).thenReturn(new com.df.knowledge.dal.dataobject.model.InstanceSettingsDO()
                .setIngestGlobalConcurrency(2).setIngestKnowledgeBaseConcurrency(1));
        docIngestService.recoverInterruptedDocs();

        // 断言：同知识库只领取一个，其余仍由持久状态保留供下次轮询
        verify(docIngestExecutor, times(1)).execute(any());
    }

    /**
     * 构造文档 DO
     */
    private static DocDO buildDoc(Long id, DocStatusEnum status, String failReason) {
        DocDO doc = new DocDO();
        doc.setId(id);
        doc.setKnowledgeBaseId(1L);
        doc.setTitle("期末复习笔记.pdf");
        doc.setFileName("期末复习笔记.pdf");
        doc.setFileId(1L);
        doc.setFileUrl("/api/system/file/download?id=1");
        doc.setStatus(status.getCode());
        doc.setFailReason(failReason);
        return doc;
    }
}
