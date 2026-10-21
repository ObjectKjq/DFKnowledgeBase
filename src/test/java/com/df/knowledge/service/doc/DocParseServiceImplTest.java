package com.df.knowledge.service.doc;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.config.DefaultDBFieldHandler;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.enums.doc.DocFormatEnum;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.enums.ErrorCodeConstants;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextBox;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link DocParseServiceImpl} 的服务层测试，仅数据库与文档存储使用桩
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocParseServiceImplTest {

    @Mock
    private DocMapper docMapper;
    @Mock
    private DocChunkMapper docChunkMapper;
    @Mock
    private DocFileService docFileService;
    @Mock
    private DocEmbeddingService docEmbeddingService;
    @Mock
    private DocChunkIndexService docChunkIndexService;
    @InjectMocks
    private DocParseServiceImpl docParseService;

    private final List<DocChunkDO> storedChunks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(docChunkMapper.selectListByDocId(anyLong())).thenAnswer(invocation -> storedChunks.stream()
                .filter(chunk -> chunk.getDocId().equals(invocation.getArgument(0))).toList());
        when(docChunkMapper.insert(any(DocChunkDO.class))).thenAnswer(invocation -> {
            storedChunks.add(invocation.getArgument(0));
            return 1;
        });
        when(docChunkMapper.deleteByDocId(anyLong())).thenAnswer(invocation -> {
            storedChunks.removeIf(chunk -> chunk.getDocId().equals(invocation.getArgument(0)));
            return 1;
        });
    }

    @Test
    void testParse_txtChunkSizeOverlapAndOffsets() throws Exception {
        // 准备参数：无分隔符长文本必须退化为字符切分
        prepareDoc("txt", "甲".repeat(1000).getBytes(StandardCharsets.UTF_8));

        // 调用
        docParseService.parse(5L);
        List<DocChunkDO> chunks = docParseService.getDocChunks(5L);

        // 断言：500 字符上限，精确重叠 100 字符，定位采用左闭右开区间
        assertEquals(3, chunks.size());
        assertChunk(chunks.get(0), 0, 0, 500, null, "甲".repeat(500));
        assertChunk(chunks.get(1), 1, 400, 900, null, "甲".repeat(500));
        assertChunk(chunks.get(2), 2, 800, 1000, null, "甲".repeat(200));
    }

    @Test
    void testParse_pdfKeepsPhysicalPageNumbersIncludingBlankPage() throws Exception {
        // 准备参数：第二页为空，第三页仍必须定位到页码 3
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String text : List.of("Page one", "", "Page three")) {
                PDPage page = new PDPage();
                pdf.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(40, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            pdf.save(output);
            prepareDoc("pdf", output.toByteArray());
        }

        // 调用
        docParseService.parse(5L);
        List<DocChunkDO> chunks = docParseService.getDocChunks(5L);

        // 断言：不跨页合并，不因空白页改变实际页码
        assertEquals(2, chunks.size());
        assertEquals(1, chunks.get(0).getPageNumber());
        assertEquals(3, chunks.get(1).getPageNumber());
        assertEquals("Page one", chunks.get(0).getText().strip());
        assertEquals("Page three", chunks.get(1).getText().strip());
        assertTrue(chunks.get(1).getStartOffset() >= chunks.get(0).getEndOffset());
        for (DocChunkDO chunk : chunks) {
            assertEquals(chunk.getText().length(), chunk.getEndOffset() - chunk.getStartOffset());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"doc", "docx", "xls", "xlsx", "md", "markdown"})
    void testParse_nonPagedFormats(String extension) throws Exception {
        // 准备参数：Office 使用真实格式字节，Markdown 保留原文结构
        byte[] bytes;
        String expected = "知识检索示例";
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            switch (extension) {
                case "doc" -> {
                    try (var input = getClass().getResourceAsStream("/doc/simple.doc")) {
                        assertNotNull(input);
                        bytes = input.readAllBytes();
                    }
                    expected = "This is a simple file";
                }
                case "docx" -> {
                    try (XWPFDocument word = new XWPFDocument()) {
                        word.createParagraph().createRun().setText(expected);
                        word.write(output);
                    }
                    bytes = output.toByteArray();
                }
                case "xls", "xlsx" -> {
                    try (Workbook workbook = extension.equals("xls") ? new HSSFWorkbook() : new XSSFWorkbook()) {
                        workbook.createSheet("资料").createRow(0).createCell(0).setCellValue(expected);
                        workbook.write(output);
                    }
                    bytes = output.toByteArray();
                }
                default -> bytes = ("# 标题\n\n" + expected).getBytes(StandardCharsets.UTF_8);
            }
        }
        prepareDoc(extension, bytes);

        // 调用
        docParseService.parse(5L);
        List<DocChunkDO> chunks = docParseService.getDocChunks(5L);

        // 断言：六格式中的非分页格式不伪造页码，块文本与字符偏移一致
        assertEquals(1, chunks.size());
        DocChunkDO chunk = chunks.getFirst();
        assertTrue(chunk.getText().contains(expected), chunk.getText());
        assertChunk(chunk, 0, 0, chunk.getText().length(), null, chunk.getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ppt", "pptx"})
    void testParse_pptKeepsPagesAndSplitsLongSlide(String extension) throws Exception {
        // 准备参数：长文本第一页、空白第二页、短文本第三页
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (extension.equals("ppt")) {
                try (HSLFSlideShow slides = new HSLFSlideShow()) {
                    for (String text : List.of("甲".repeat(600), "", "尾页")) {
                        HSLFTextBox box = new HSLFTextBox();
                        box.setText(text);
                        slides.createSlide().addShape(box);
                    }
                    slides.write(output);
                }
            } else {
                try (XMLSlideShow slides = new XMLSlideShow()) {
                    for (String text : List.of("甲".repeat(600), "", "尾页")) {
                        slides.createSlide().createTextBox().setText(text);
                    }
                    slides.write(output);
                }
            }
            prepareDoc(extension, output.toByteArray());
        }

        // 调用
        docParseService.parse(5L);
        List<DocChunkDO> chunks = docParseService.getDocChunks(5L);

        // 断言：长页在页内切分，页间不重叠，空白页不占块序号但保留物理页码
        assertEquals(3, chunks.size());
        assertChunk(chunks.get(0), 0, 0, 500, 1, "甲".repeat(500));
        assertEquals(400, chunks.get(1).getStartOffset());
        assertEquals(1, chunks.get(1).getPageNumber());
        assertEquals(2, chunks.get(2).getChunkIndex());
        assertEquals(3, chunks.get(2).getPageNumber());
        assertEquals("尾页", chunks.get(2).getText().strip());
        assertTrue(chunks.get(2).getStartOffset() >= chunks.get(1).getEndOffset());
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getText().length() <= 500));
    }

    @Test
    void testParse_emptyFailsAndRetryReplacesOnlyOwnChunks() throws Exception {
        // 准备参数：旧分块与其他文档的分块都已存在，本次抽取结果为空
        DocChunkDO oldChunk = new DocChunkDO().setDocId(5L).setChunkIndex(0).setText("旧内容");
        DocChunkDO otherChunk = new DocChunkDO().setDocId(6L).setChunkIndex(0).setText("其他文档");
        storedChunks.addAll(List.of(oldChunk, otherChunk));
        prepareDoc("txt", " \n\t".getBytes(StandardCharsets.UTF_8));

        // 调用并断言：空文本不可当作成功入库，解析失败不破坏既有分块
        ServiceException exception = assertThrows(ServiceException.class, () -> docParseService.parse(5L));
        assertEquals(ErrorCodeConstants.DOC_PARSE_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(exception.getMessage().contains("未提取到文本"));
        assertEquals(List.of(oldChunk), docParseService.getDocChunks(5L));

        // 调用：修复文档内容后重试，相同文档重复执行也不会累积分块
        prepareDoc("txt", "新内容".getBytes(StandardCharsets.UTF_8));
        docParseService.parse(5L);
        docParseService.parse(5L);

        // 断言：重新解析的分块可查询，其他文档不受影响
        List<DocChunkDO> chunks = docParseService.getDocChunks(5L);
        assertEquals(1, chunks.size());
        assertChunk(chunks.getFirst(), 0, 0, 3, null, "新内容");
        assertEquals(List.of(otherChunk), docParseService.getDocChunks(6L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pdf", "docx", "pptx", "xlsx"})
    void testParse_corruptDocumentFailsWithReadableReason(String extension) throws Exception {
        // 准备参数：存储内容损坏
        prepareDoc(extension, "损坏内容".getBytes(StandardCharsets.UTF_8));

        // 调用并断言：统一为业务异常，供入库服务记录原因
        ServiceException exception = assertThrows(ServiceException.class, () -> docParseService.parse(5L));
        assertEquals(ErrorCodeConstants.DOC_PARSE_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(exception.getMessage().startsWith("文档解析失败："));
        assertTrue(docParseService.getDocChunks(5L).isEmpty());
    }

    @Test
    void testParse_recursiveBoundariesPreserveWhitespaceAndOffsets() throws Exception {
        // 准备参数：两处分段边界均早于字符上限
        String text = "甲".repeat(300) + "\n\n" + "乙".repeat(300) + "\n\n" + "丙".repeat(300);
        prepareDoc("txt", text.getBytes(StandardCharsets.UTF_8));

        // 调用
        docParseService.parse(5L);
        List<DocChunkDO> chunks = docParseService.getDocChunks(5L);

        // 断言：优先在段落边界切分，空白保留且相邻块精确重叠 100 字符
        assertEquals(3, chunks.size());
        assertChunk(chunks.get(0), 0, 0, 302, null, "甲".repeat(300) + "\n\n");
        assertChunk(chunks.get(1), 1, 202, 604, null, "甲".repeat(98) + "\n\n" + "乙".repeat(300) + "\n\n");
        assertChunk(chunks.get(2), 2, 504, 904, null, "乙".repeat(98) + "\n\n" + "丙".repeat(300));
    }

    @Test
    void testParse_docMissing() {
        // 准备参数：数据库不存在该文档
        when(docMapper.selectByIdForUpdate(5L)).thenReturn(null);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> docParseService.parse(5L));
        assertEquals(ErrorCodeConstants.DOC_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @ParameterizedTest
    @EnumSource(value = DocStatusEnum.class, names = {"INDEXING", "AVAILABLE", "DELETED"})
    void testParse_staleTaskDoesNotReplaceChunks(DocStatusEnum status) throws Exception {
        // 准备参数：旧解析任务等待行锁期间，另一任务已完成解析、索引或删除
        DocDO doc = prepareDoc("txt", "不应再次解析".getBytes(StandardCharsets.UTF_8));
        doc.setStatus(status.getCode());
        DocChunkDO existing = new DocChunkDO().setDocId(5L).setChunkIndex(0).setText("已建立索引的分块");
        existing.setId(700L);
        storedChunks.add(existing);

        // 调用
        docParseService.parse(5L);

        // 断言：保持原分块主键及内容，避免 ES 的分块编号与 MySQL 失配
        assertEquals(List.of(existing), docParseService.getDocChunks(5L));
        assertEquals(700L, docParseService.getDocChunks(5L).getFirst().getId());
        verifyNoInteractions(docFileService);
    }

    @Test
    void testProcessDoc_failureIsolationAndRetryUntilAvailable() throws Exception {
        // 准备参数：真实解析服务与入库状态机，数据库与存储为外部桩
        DocDO failed = prepareDoc("txt", "\n".getBytes(StandardCharsets.UTF_8));
        failed.setStatus(DocStatusEnum.QUEUED.getCode());
        DocDO healthy = new DocDO();
        healthy.setId(6L);
        healthy.setFilePath("healthy.txt");
        healthy.setFormat(DocFormatEnum.TXT.getCode());
        healthy.setStatus(DocStatusEnum.QUEUED.getCode());
        when(docMapper.selectById(6L)).thenReturn(healthy);
        when(docMapper.selectByIdForUpdate(6L)).thenReturn(healthy);
        when(docFileService.open("healthy.txt")).thenAnswer(invocation ->
                new ByteArrayInputStream("正常文档".getBytes(StandardCharsets.UTF_8)));
        for (DocDO doc : List.of(failed, healthy)) {
            when(docMapper.updateStatus(eq(doc.getId()), anyInt(), anyInt(), nullable(String.class)))
                    .thenAnswer(invocation -> {
                        if (!doc.getStatus().equals(invocation.getArgument(1))) {
                            return 0;
                        }
                        doc.setStatus(invocation.getArgument(2));
                        if (invocation.getArgument(3) != null) {
                            doc.setFailReason(invocation.getArgument(3));
                        }
                        return 1;
                    });
        }
        DocIngestServiceImpl ingest = new DocIngestServiceImpl();
        ReflectionTestUtils.setField(ingest, "docMapper", docMapper);
        ReflectionTestUtils.setField(ingest, "docParseService", docParseService);
        DocIndexServiceImpl index = new DocIndexServiceImpl();
        ReflectionTestUtils.setField(index, "docMapper", docMapper);
        ReflectionTestUtils.setField(index, "docChunkMapper", docChunkMapper);
        ReflectionTestUtils.setField(index, "docEmbeddingService", docEmbeddingService);
        ReflectionTestUtils.setField(index, "docChunkIndexService", docChunkIndexService);
        ReflectionTestUtils.setField(ingest, "docIndexService", index);
        when(docEmbeddingService.embedAll(any())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return texts.stream().map(text -> {
                float[] vector = new float[512];
                vector[0] = 1;
                return vector;
            }).toList();
        });

        // 调用：单文档失败后仍处理另一文档，真实解析与索引编排贯通
        ingest.processDoc(5L);
        ingest.processDoc(6L);

        // 断言：失败原因可读，其他文档已完成完整入库流程
        assertEquals(DocStatusEnum.FAILED.getCode(), failed.getStatus());
        String firstReason = failed.getFailReason();
        assertTrue(firstReason.contains("未提取到文本"));
        assertEquals(DocStatusEnum.AVAILABLE.getCode(), healthy.getStatus());
        assertEquals("正常文档", docParseService.getDocChunks(6L).getFirst().getText());

        // 调用：模拟用户重试回排队，再走完整解析流程
        when(docFileService.open(failed.getFilePath())).thenAnswer(invocation ->
                new ByteArrayInputStream("修复后内容".getBytes(StandardCharsets.UTF_8)));
        failed.setStatus(DocStatusEnum.QUEUED.getCode());
        ingest.processDoc(5L);

        // 断言：重试完成解析与索引，保留首次失败原因
        assertEquals(DocStatusEnum.AVAILABLE.getCode(), failed.getStatus());
        assertEquals(firstReason, failed.getFailReason());
        assertEquals("修复后内容", docParseService.getDocChunks(5L).getFirst().getText());
    }

    @Test
    void testParse_unicodeCharactersAreNeverSplitInsideSurrogatePair() throws Exception {
        // 准备参数：补充平面字符在 UTF-16 中占两个代码单元
        prepareDoc("txt", "😀".repeat(600).getBytes(StandardCharsets.UTF_8));

        // 调用
        docParseService.parse(5L);
        List<DocChunkDO> chunks = docParseService.getDocChunks(5L);

        // 断言：500/100 按 Unicode 字符切分，引用偏移仍使用 Java UTF-16 索引
        assertEquals(2, chunks.size());
        assertChunk(chunks.get(0), 0, 0, 1000, null, "😀".repeat(500));
        assertChunk(chunks.get(1), 1, 800, 1200, null, "😀".repeat(200));
    }

    @Test
    void testParse_backgroundPersistenceWithoutWebContext() throws Exception {
        // 准备参数：数据库桩执行与真实 MyBatis-Plus 相同的审计填充，线程没有 Web 上下文
        prepareDoc("txt", "后台入库".getBytes(StandardCharsets.UTF_8));
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), DocChunkDO.class);
        DefaultDBFieldHandler handler = new DefaultDBFieldHandler();
        when(docChunkMapper.insert(any(DocChunkDO.class))).thenAnswer(invocation -> {
            DocChunkDO chunk = invocation.getArgument(0);
            handler.insertFill(SystemMetaObject.forObject(chunk));
            storedChunks.add(chunk);
            return 1;
        });

        // 调用
        docParseService.parse(5L);

        // 断言：后台入库可写分块，时间正常填充，创建者为空而不读取 Web 登录态
        DocChunkDO chunk = docParseService.getDocChunks(5L).getFirst();
        assertEquals("后台入库", chunk.getText());
        assertEquals("", chunk.getCreator());
        assertNotNull(chunk.getCreateTime());
    }

    private DocDO prepareDoc(String extension, byte[] bytes) throws Exception {
        DocDO doc = new DocDO();
        doc.setId(5L);
        doc.setFileName("资料." + extension);
        doc.setFilePath("stored." + extension);
        doc.setFormat(DocFormatEnum.resolveByExtension(extension).getCode());
        doc.setStatus(DocStatusEnum.PARSING.getCode());
        when(docMapper.selectById(5L)).thenReturn(doc);
        when(docMapper.selectByIdForUpdate(5L)).thenReturn(doc);
        when(docFileService.open(doc.getFilePath())).thenAnswer(invocation -> new ByteArrayInputStream(bytes));
        return doc;
    }

    private void assertChunk(DocChunkDO chunk, int index, int start, int end, Integer page, String text) {
        assertEquals(5L, chunk.getDocId());
        assertEquals(index, chunk.getChunkIndex());
        assertEquals(start, chunk.getStartOffset());
        assertEquals(end, chunk.getEndOffset());
        assertEquals(page, chunk.getPageNumber());
        assertEquals(text, chunk.getText());
    }
}
