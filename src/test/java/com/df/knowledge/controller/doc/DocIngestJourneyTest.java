package com.df.knowledge.controller.doc;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.dal.dataobject.doc.*;
import com.df.knowledge.dal.dataobject.file.FileDO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.mysql.doc.*;
import com.df.knowledge.dal.mysql.knowledge.KnowledgeBaseMapper;
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.doc.*;
import com.df.knowledge.service.file.FileService;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.ByteArrayOutputStream;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 文档从 HTTP 创建到入库与原文定位的行为测试，数据库及外部向量服务使用替身。 */
class DocIngestJourneyTest {
    @Test
    void testCreate_excelRetainsWorksheetAndColumnRelationship() throws Exception {
        // 准备参数
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("成绩");
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(2).setCellValue("分数");
            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("张三");
            row.createCell(2).setCellValue(95);
            workbook.write(output);
            bytes = output.toByteArray();
        }
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var journey = new Journey("成绩.xlsx", bytes);
            // 调用与断言：创建后仍排队，后台处理后可从授权预览查看工作表与列关系
            journey.mvc.perform(post("/knowledge/doc/create").contentType("application/json")
                    .content("{\"knowledgeBaseId\":1,\"fileId\":10,\"fileUrl\":\"/download?id=10\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(5));
            journey.mvc.perform(get("/knowledge/doc/get").param("id", "5"))
                    .andExpect(jsonPath("$.data.status").value(DocStatusEnum.QUEUED.getCode()));
            journey.ingest.processDoc(5L);
            journey.mvc.perform(get("/knowledge/search/preview").param("chunkId", "100"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.sheetName").value("成绩"))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("姓名=张三")))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("分数=95")));
        }
    }

    @Test
    void testCreate_mixedPdfOcrFailureHasPageAndKeepsFirstReasonOnRetry() throws Exception {
        // 准备参数：第二页只有图像，第一页为可直接抽取的文字。
        byte[] bytes;
        try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument(); var output = new ByteArrayOutputStream()) {
            var first = new org.apache.pdfbox.pdmodel.PDPage(); pdf.addPage(first);
            try (var stream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, first)) {
                stream.beginText(); stream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 16);
                stream.newLineAtOffset(30, 700); stream.showText("Text page"); stream.endText();
            }
            var second = new org.apache.pdfbox.pdmodel.PDPage(); pdf.addPage(second);
            var image = new java.awt.image.BufferedImage(500, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics(); graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0,0,500,100);
            graphics.setColor(java.awt.Color.BLACK); graphics.setFont(new java.awt.Font("SansSerif", 0, 30));
            graphics.drawString("Scanned document", 10, 60); graphics.dispose();
            try (var stream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, second)) {
                stream.drawImage(org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(pdf, image), 30, 600, 500, 100);
            }
            pdf.save(output); bytes = output.toByteArray();
        }
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var journey = new Journey("混合.pdf", bytes);
            var ocr = mock(DocOcrService.class);
            wire(journey.parse, "docOcrService", ocr);
            when(ocr.recognize(any())).thenThrow(new java.io.IOException("扫描质量过低"))
                    .thenReturn("识别到的扫描内容");
            journey.mvc.perform(post("/knowledge/doc/create").contentType("application/json")
                    .content("{\"knowledgeBaseId\":1,\"fileId\":10,\"fileUrl\":\"/download?id=10\"}"))
                    .andExpect(status().isOk());
            journey.ingest.processDoc(5L);
            // 断言：第二页失败时整份失败，未留下第一页分块。
            journey.mvc.perform(get("/knowledge/doc/get").param("id", "5"))
                    .andExpect(jsonPath("$.data.status").value(DocStatusEnum.FAILED.getCode()))
                    .andExpect(jsonPath("$.data.failReason").value(org.hamcrest.Matchers.containsString("第 2 页")))
                    .andExpect(jsonPath("$.data.failReason").value(org.hamcrest.Matchers.containsString("扫描质量过低")));
            org.junit.jupiter.api.Assertions.assertTrue(journey.storedChunks.isEmpty());
            journey.mvc.perform(post("/knowledge/doc/retry").param("id", "5")).andExpect(status().isOk());
            journey.ingest.processDoc(5L);
            journey.mvc.perform(get("/knowledge/doc/get").param("id", "5"))
                    .andExpect(jsonPath("$.data.status").value(DocStatusEnum.AVAILABLE.getCode()))
                    .andExpect(jsonPath("$.data.failReason").value(org.hamcrest.Matchers.containsString("扫描质量过低")));
            journey.mvc.perform(get("/knowledge/search/preview").param("chunkId", "101"))
                    .andExpect(jsonPath("$.data.pageNumber").value(2))
                    .andExpect(jsonPath("$.data.ocr").value(true))
                    .andExpect(jsonPath("$.data.text").value("识别到的扫描内容"));
            journey.mvc.perform(get("/knowledge/search/preview").param("chunkId", "100"))
                    .andExpect(jsonPath("$.data.pageNumber").value(1)).andExpect(jsonPath("$.data.ocr").value(false));
        }
    }

    @Test
    void testCreate_wordTableRepeatsHeadersForDataRows() throws Exception {
        // 准备参数
        byte[] bytes;
        try (var word = new org.apache.poi.xwpf.usermodel.XWPFDocument(); var output = new ByteArrayOutputStream()) {
            var table = word.createTable(2, 2);
            table.getRow(0).getCell(0).setText("项目"); table.getRow(0).getCell(1).setText("费用");
            table.getRow(1).getCell(0).setText("设备"); table.getRow(1).getCell(1).setText("120");
            word.write(output); bytes = output.toByteArray();
        }
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var journey = new Journey("费用.docx", bytes);
            journey.mvc.perform(post("/knowledge/doc/create").contentType("application/json")
                    .content("{\"knowledgeBaseId\":1,\"fileId\":10,\"fileUrl\":\"/download?id=10\"}")).andExpect(status().isOk());
            journey.ingest.processDoc(5L);
            // 断言：预览不会把单元格压成无法辨别列归属的连续文字。
            journey.mvc.perform(get("/knowledge/search/preview").param("chunkId", "100"))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("项目=设备")))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("费用=120")));
        }
    }

    @Test
    void testCreate_globalAndKnowledgeBaseLimitsAndRestartRecovery() throws Exception {
        // 准备参数：真实工作线程在向量服务边界等待，用状态观察并发，不依赖固定睡眠。
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var journey = new Journey("笔记.txt", "同名文档可重复入库".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var executor = new com.df.knowledge.config.DocIngestExecutorConfiguration().docIngestExecutor();
            executor.initialize(); wire(journey.ingest, "docIngestExecutor", executor);
            var entered = new java.util.concurrent.CountDownLatch(2);
            var release = new java.util.concurrent.CountDownLatch(1);
            doAnswer(call -> {
                entered.countDown();
                if (!release.await(20, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("测试同步点超时");
                float[] vector = new float[512]; vector[0] = 1; return List.of(vector);
            }).when(journey.embedding).embedAll(any());
            try {
                for (long base : List.of(1L, 1L, 2L)) {
                    journey.mvc.perform(post("/knowledge/doc/create").contentType("application/json")
                            .content("{\"knowledgeBaseId\":" + base + ",\"fileId\":10,\"fileUrl\":\"/download?id=10\"}"))
                            .andExpect(status().isOk());
                }
                journey.mvc.perform(get("/knowledge/doc/get").param("id", "5"))
                        .andExpect(jsonPath("$.data.status").value(DocStatusEnum.QUEUED.getCode()));
                journey.ingest.dispatchPendingDocs();
                org.junit.jupiter.api.Assertions.assertTrue(entered.await(20, java.util.concurrent.TimeUnit.SECONDS));
                journey.mvc.perform(get("/knowledge/doc/get").param("id", "5"))
                        .andExpect(jsonPath("$.data.status").value(DocStatusEnum.INDEXING.getCode()));
                journey.mvc.perform(get("/knowledge/doc/get").param("id", "6"))
                        .andExpect(jsonPath("$.data.status").value(DocStatusEnum.QUEUED.getCode()));
                journey.mvc.perform(get("/knowledge/doc/get").param("id", "7"))
                        .andExpect(jsonPath("$.data.status").value(DocStatusEnum.INDEXING.getCode()));
            } finally {
                release.countDown();
                executor.getThreadPoolExecutor().shutdown();
                org.junit.jupiter.api.Assertions.assertTrue(executor.getThreadPoolExecutor().awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS));
            }
            // 调用：重建调度实例，仍从同一持久任务集合领取剩余文档。
            var restarted = new DocIngestServiceImpl();
            var nextExecutor = new com.df.knowledge.config.DocIngestExecutorConfiguration().docIngestExecutor();
            nextExecutor.initialize();
            wire(restarted, "docMapper", journey.docs, "docParseService", journey.parse,
                    "docIndexService", org.springframework.test.util.ReflectionTestUtils.getField(journey.ingest, "docIndexService"),
                    "docIngestExecutor", nextExecutor, "instanceSettingsService", journey.settings);
            try {
                restarted.dispatchPendingDocs();
            } finally {
                nextExecutor.getThreadPoolExecutor().shutdown();
                org.junit.jupiter.api.Assertions.assertTrue(nextExecutor.getThreadPoolExecutor().awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS));
            }
            journey.mvc.perform(get("/knowledge/doc/get").param("id", "6"))
                    .andExpect(jsonPath("$.data.status").value(DocStatusEnum.AVAILABLE.getCode()));
        }
    }

    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "P0_OCR_BINARY", matches = ".+")
    void testCreate_realChineseScanOcrThroughHttp() throws Exception {
        // 准备参数：字形渲染成图像后嵌入 PDF，PDF 本身没有可复制的文字层。
        byte[] bytes;
        try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage(); pdf.addPage(page);
            var image = new java.awt.image.BufferedImage(1200, 200, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics(); graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0,0,1200,200);
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(java.awt.Color.BLACK); graphics.setFont(new java.awt.Font("Microsoft YaHei", 0, 28));
            graphics.drawString("知识管理 文档检索", 30, 120); graphics.dispose();
            try (var stream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                stream.drawImage(org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(pdf, image), 30, 600, 550, 92);
            }
            pdf.save(output); bytes = output.toByteArray();
        }
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var journey = new Journey("中文扫描.pdf", bytes);
            var ocr = new DocOcrServiceImpl();
            wire(ocr, "binary", System.getenv("P0_OCR_BINARY"));
            wire(journey.parse, "docOcrService", ocr);
            journey.mvc.perform(post("/knowledge/doc/create").contentType("application/json")
                    .content("{\"knowledgeBaseId\":1,\"fileId\":10,\"fileUrl\":\"/download?id=10\"}"))
                    .andExpect(status().isOk());
            journey.ingest.processDoc(5L);
            // 断言：真实进程返回中文，HTTP 预览保留物理页码和 OCR 来源。
            journey.mvc.perform(get("/knowledge/doc/get").param("id", "5"))
                    .andExpect(jsonPath("$.data.status").value(DocStatusEnum.AVAILABLE.getCode()));
            journey.mvc.perform(get("/knowledge/search/preview").param("chunkId", "100"))
                    .andExpect(jsonPath("$.data.pageNumber").value(1)).andExpect(jsonPath("$.data.ocr").value(true))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("知识管理")))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("文档检索")));
        }
    }

    @Test
    void testCreate_pdfTableRetainsHeadersAndColumns() throws Exception {
        byte[] bytes;
        try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new org.apache.pdfbox.pdmodel.PDPage(); pdf.addPage(page);
            try (var stream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                for (int row = 0; row < 2; row++) {
                    for (int column = 0; column < 2; column++) {
                        stream.beginText(); stream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(
                                org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 14);
                        stream.newLineAtOffset(30 + column * 250, 700 - row * 30);
                        stream.showText(new String[][]{{"Name", "Score"}, {"Alice", "95"}}[row][column]);
                        stream.endText();
                    }
                }
            }
            pdf.save(output); bytes = output.toByteArray();
        }
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var journey = new Journey("表格.pdf", bytes);
            journey.mvc.perform(post("/knowledge/doc/create").contentType("application/json")
                    .content("{\"knowledgeBaseId\":1,\"fileId\":10,\"fileUrl\":\"/download?id=10\"}"))
                    .andExpect(status().isOk());
            journey.ingest.processDoc(5L);
            journey.mvc.perform(get("/knowledge/search/preview").param("chunkId", "100"))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("Name=Alice")))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("Score=95")));
        }
    }

    @Test
    void testCreate_pptTableRetainsSlideAndColumns() throws Exception {
        byte[] bytes;
        try (var slides = new org.apache.poi.xslf.usermodel.XMLSlideShow(); var output = new ByteArrayOutputStream()) {
            var table = slides.createSlide().createTable(2, 2);
            table.getCell(0, 0).setText("姓名"); table.getCell(0, 1).setText("分数");
            table.getCell(1, 0).setText("张三"); table.getCell(1, 1).setText("95");
            slides.write(output); bytes = output.toByteArray();
        }
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var journey = new Journey("成绩.pptx", bytes);
            journey.mvc.perform(post("/knowledge/doc/create").contentType("application/json")
                    .content("{\"knowledgeBaseId\":1,\"fileId\":10,\"fileUrl\":\"/download?id=10\"}"))
                    .andExpect(status().isOk());
            journey.ingest.processDoc(5L);
            journey.mvc.perform(get("/knowledge/search/preview").param("chunkId", "100"))
                    .andExpect(jsonPath("$.data.pageNumber").value(1))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("姓名=张三")))
                    .andExpect(jsonPath("$.data.text").value(org.hamcrest.Matchers.containsString("分数=95")));
        }
    }

    @Test
    void testCreate_rejectedExecutorLeavesTaskQueuedAndRecoverable() throws Exception {
        // 准备参数：真实线程池停止接收，触发与饱和一致的拒绝路径。
        try (var auth = mockStatic(StpUtil.class)) {
            auth.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            var journey = new Journey("排队.txt", "拒绝后仍可处理".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var executor = new com.df.knowledge.config.DocIngestExecutorConfiguration().docIngestExecutor();
            executor.initialize(); executor.shutdown();
            wire(journey.ingest, "docIngestExecutor", executor);
            journey.mvc.perform(post("/knowledge/doc/create").contentType("application/json")
                    .content("{\"knowledgeBaseId\":1,\"fileId\":10,\"fileUrl\":\"/download?id=10\"}"))
                    .andExpect(status().isOk());
            journey.ingest.dispatchPendingDocs();
            journey.mvc.perform(get("/knowledge/doc/get").param("id", "5"))
                    .andExpect(jsonPath("$.data.status").value(DocStatusEnum.QUEUED.getCode()));
            var next = new com.df.knowledge.config.DocIngestExecutorConfiguration().docIngestExecutor(); next.initialize();
            wire(journey.ingest, "docIngestExecutor", next);
            try {
                journey.ingest.dispatchPendingDocs();
            } finally {
                next.getThreadPoolExecutor().shutdown();
                org.junit.jupiter.api.Assertions.assertTrue(next.getThreadPoolExecutor().awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS));
            }
            journey.mvc.perform(get("/knowledge/doc/get").param("id", "5"))
                    .andExpect(jsonPath("$.data.status").value(DocStatusEnum.AVAILABLE.getCode()));
        }
    }

    static class Journey {
        final DocMapper docs;
        final DocChunkMapper chunks;
        final FileService files = mock(FileService.class);
        final Map<Long, DocDO> storedDocs = Collections.synchronizedMap(new LinkedHashMap<>());
        final List<DocChunkDO> storedChunks = Collections.synchronizedList(new ArrayList<>());
        final DocEmbeddingService embedding = mock(DocEmbeddingService.class);
        final java.util.concurrent.atomic.AtomicLong ids = new java.util.concurrent.atomic.AtomicLong(5);
        final com.df.knowledge.service.model.InstanceSettingsService settings = mock(com.df.knowledge.service.model.InstanceSettingsService.class);
        final DocIngestServiceImpl ingest = new DocIngestServiceImpl();
        final DocParseServiceImpl parse = new DocParseServiceImpl();
        final MockMvc mvc;
        Journey(String name, byte[] content) throws Exception {
            this(name, content, mock(DocMapper.class), mock(DocChunkMapper.class));
        }
        Journey(String name, byte[] content, DocMapper docs, DocChunkMapper chunks) throws Exception {
            this.docs = docs; this.chunks = chunks;
            when(settings.get()).thenReturn(new com.df.knowledge.dal.dataobject.model.InstanceSettingsDO()
                    .setIngestGlobalConcurrency(2).setIngestKnowledgeBaseConcurrency(1));
            var permission = mock(KnowledgeBasePermissionService.class);
            var indexService = new DocIndexServiceImpl();
            var index = mock(DocChunkIndexService.class);
            var service = new DocServiceImpl();
            var search = new DocSearchServiceImpl();
            var bases = mock(KnowledgeBaseMapper.class);
            when(bases.selectByDocIdForShare(any())).thenReturn(new KnowledgeBaseDO());
            when(bases.selectById(1L)).thenReturn(new KnowledgeBaseDO().setName("资料"));
            var file = new FileDO().setName(name).setSize((long) content.length); file.setId(10L);
            when(files.getAttachableFile(7L, 10L)).thenReturn(file);
            when(files.getContent(any())).thenReturn(content);
            if (mockingDetails(docs).isMock()) {
            when(docs.insert(any(DocDO.class))).thenAnswer(call -> {
                DocDO doc = call.getArgument(0); doc.setId(ids.getAndIncrement()); storedDocs.put(doc.getId(), doc); return 1;
            });
            when(docs.selectListByStatuses(any())).thenAnswer(call -> storedDocs.values().stream()
                    .filter(doc -> DocStatusEnum.isInFlight(doc.getStatus())).toList());
            when(docs.selectById(any())).thenAnswer(call -> storedDocs.get(call.getArgument(0)));
            when(docs.selectByIdForUpdate(any())).thenAnswer(call -> storedDocs.get(call.getArgument(0)));
            when(docs.updateStatus(anyLong(), anyInt(), anyInt(), nullable(String.class))).thenAnswer(call -> {
                DocDO doc = storedDocs.get(call.getArgument(0));
                if (!doc.getStatus().equals(call.getArgument(1))) return 0;
                doc.setStatus(call.getArgument(2));
                if (call.getArgument(3) != null) doc.setFailReason(call.getArgument(3));
                return 1;
            });
            when(chunks.selectListByDocId(any())).thenAnswer(call -> storedChunks.stream()
                    .filter(chunk -> chunk.getDocId().equals(call.getArgument(0))).toList());
            when(chunks.selectById(any())).thenAnswer(call -> storedChunks.stream()
                    .filter(chunk -> chunk.getId().equals(call.getArgument(0))).findFirst().orElse(null));
            when(chunks.insert(any(DocChunkDO.class))).thenAnswer(call -> {
                synchronized (storedChunks) {
                    DocChunkDO chunk = call.getArgument(0); chunk.setId(100L + storedChunks.size()); storedChunks.add(chunk); return 1;
                }
            });
            when(chunks.deleteByDocId(any())).thenAnswer(call -> { storedChunks.removeIf(c -> c.getDocId().equals(call.getArgument(0))); return 1; });
            }
            when(embedding.embedAll(any())).thenAnswer(call -> ((List<?>) call.getArgument(0)).stream().map(x -> {
                float[] vector = new float[512]; vector[0] = 1; return vector;
            }).toList());
            wire(parse, "docMapper", docs, "docChunkMapper", chunks, "fileService", files, "knowledgeBaseMapper", bases);
            wire(indexService, "docMapper", docs, "docChunkMapper", chunks, "docEmbeddingService", embedding, "docChunkIndexService", index, "knowledgeBaseMapper", bases);
            wire(ingest, "docMapper", docs, "docParseService", parse, "docIndexService", indexService, "instanceSettingsService", settings);
            wire(service, "docMapper", docs, "fileService", files, "knowledgeBasePermissionService", permission, "knowledgeBaseMapper", bases);
            wire(search, "docMapper", docs, "docChunkMapper", chunks, "knowledgeBaseMapper", bases, "permissionService", permission);
            var controller = new DocController(); wire(controller, "docService", service);
            var searchController = new DocSearchController(); wire(searchController, "docSearchService", search);
            mvc = MockMvcBuilders.standaloneSetup(controller, searchController).build();
        }
    }
    static void wire(Object target, Object... pairs) {
        for (int i = 0; i < pairs.length; i += 2) ReflectionTestUtils.setField(target, (String) pairs[i], pairs[i + 1]);
    }
}
