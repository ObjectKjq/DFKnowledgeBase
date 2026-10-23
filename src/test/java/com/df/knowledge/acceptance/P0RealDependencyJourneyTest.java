package com.df.knowledge.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** 实际后端子进程、MySQL、文件字节、ONNX、ES Basic 和中文 OCR 的完整 HTTP 验收。 */
@EnabledIfSystemProperty(named = "p0.acceptance.enabled", matches = "true")
class P0RealDependencyJourneyTest {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final String ES = System.getenv().getOrDefault("P0_ES_URL", "http://127.0.0.1:19200");
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private String database;
    private String index;
    private String mysqlUser;
    private String mysqlPassword;
    private int port;
    private int starts;
    private Process application;
    private boolean databaseCreated;
    private boolean indexCreated;

    @BeforeEach
    void prepareIsolatedDependencies() throws Exception {
        mysqlUser = requireEnvironment("P0_MYSQL_USER");
        mysqlPassword = requireEnvironment("P0_MYSQL_PASSWORD");
        assertTrue(Files.isRegularFile(Path.of(requireEnvironment("P0_OCR_BINARY"))), "请配置真实OCR桥接器");
        String identity = ProcessHandle.current().pid() + "_" + SEQUENCE.incrementAndGet();
        database = "df_p0_journey_" + identity;
        index = "df_p0_journey_" + identity;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        try (var connection = connect("")) {
            connection.createStatement().execute("CREATE DATABASE " + database + " CHARACTER SET utf8mb4");
            databaseCreated = true;
        }
        String mapping = Files.readString(Path.of("es/create-doc-chunk-index.http"));
        var jsonStart = java.util.regex.Pattern.compile("(?m)^\\{").matcher(mapping);
        assertTrue(jsonStart.find(), "ES脚本必须包含独立JSON请求体");
        mapping = mapping.substring(jsonStart.start());
        var response = raw("PUT", ES + "/" + index, null, "application/json", mapping.getBytes(StandardCharsets.UTF_8));
        assertEquals(200, response.statusCode(), "独立验收索引创建失败");
        indexCreated = true;
        var license = raw("GET", ES + "/_license", null, null, null);
        assertEquals("basic", json.readTree(license.body()).path("license").path("type").asText());
    }

    @AfterEach
    void cleanOnlyOwnedResources() throws Exception {
        stopApplication();
        assertTrue(database.startsWith("df_p0_journey_"));
        assertTrue(index.startsWith("df_p0_journey_"));
        if (indexCreated) {
            assertEquals(200, raw("DELETE", ES + "/" + index, null, null, null).statusCode());
        }
        if (databaseCreated) {
            try (var connection = connect("")) {
                connection.createStatement().execute("DROP DATABASE " + database);
            }
        }
    }

    @Test
    void testJourney_realFilesOnnxOcrSearchRagRevocationAndJvmRecovery() throws Exception {
        // 准备参数：只装载隔离库，所有身份及业务操作经真实登录与HTTP入口执行。
        try (var connection = connect(database)) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/schema.sql"));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/data.sql"));
        }
        startApplication(3_600_000);
        String admin = login("admin", "admin123");
        for (String username : List.of("p0_owner", "p0_viewer", "p0_editor", "p0_outsider")) {
            ok("POST", "/system/user/create", admin, Map.of("username", username, "password", "Journey123!",
                    "nickname", username, "roleIds", List.of(2)));
        }
        String owner = login("p0_owner", "Journey123!");
        String viewer = login("p0_viewer", "Journey123!");
        String outsider = login("p0_outsider", "Journey123!");
        long knowledgeBaseId = ok("POST", "/knowledge/base/create", owner,
                Map.of("name", "真实依赖验收", "type", 2)).asLong();
        ok("POST", "/knowledge/base/member/invite", owner,
                Map.of("knowledgeBaseId", knowledgeBaseId, "username", "p0_viewer", "memberRole", 2));
        ok("POST", "/knowledge/base/member/invite", owner,
                Map.of("knowledgeBaseId", knowledgeBaseId, "username", "p0_editor", "memberRole", 1));
        ok("PUT", "/system/settings/update", admin, Map.of("mcpEnabled", true, "mcpRateLimitPerMinute", 60,
                "ingestGlobalConcurrency", 1, "ingestKnowledgeBaseConcurrency", 1));
        Map<String, byte[]> samples = samples();
        List<Long> documents = new ArrayList<>();
        List<Long> files = new ArrayList<>();
        for (var sample : samples.entrySet()) {
            var uploaded = upload(owner, sample.getKey(), sample.getValue());
            files.add(uploaded.path("id").asLong());
            assertFileDenied(outsider, uploaded.path("id").asLong());
            documents.add(ok("POST", "/knowledge/doc/create", owner, Map.of("knowledgeBaseId", knowledgeBaseId,
                    "fileId", uploaded.path("id").asLong(), "fileUrl", uploaded.path("url").asText())).asLong());
        }
        long textDocument = documents.getFirst();
        assertEquals(1, ok("GET", "/knowledge/doc/get?id=" + textDocument, owner, null).path("status").asInt());
        long terminatedPid = application.pid();
        stopApplication();
        assertFalse(ProcessHandle.of(terminatedPid).map(ProcessHandle::isAlive).orElse(false));
        checkpoint("实际JVM强杀，已提交文档仍为排队，随后从同一隔离库恢复");
        startApplication(100);
        owner = login("p0_owner", "Journey123!");
        viewer = login("p0_viewer", "Journey123!");
        outsider = login("p0_outsider", "Journey123!");
        admin = login("admin", "admin123");
        for (Long document : documents) {
            awaitAvailable(owner, document);
        }
        checkpoint("六格式整份入库通过：真实MySQL文件存储、ONNX512维、ES Basic及中文OCR");

        // 调用与断言：真实混合/模糊、格式过滤与分页，实例管理员不能越过内容授权。
        Map<String, Object> query = new LinkedHashMap<>(Map.of("query", "知识管理", "knowledgeBaseIds", List.of(knowledgeBaseId),
                "formats", List.of(5), "pageSize", 20));
        var first = ok("POST", "/knowledge/search/search", viewer, query);
        assertTrue(first.path("total").asInt() > 20, "长文档应跨页，不允许只验证一条假命中");
        assertEquals(20, first.path("list").size());
        assertTrue(first.path("list").get(0).path("score").asDouble() > 0);
        query.put("pageNo", 2);
        assertFalse(ok("POST", "/knowledge/search/search", viewer, query).path("list").isEmpty());
        query.put("mode", "FUZZY");
        assertEquals("SEMANTIC", ok("POST", "/knowledge/search/search", viewer, query).path("list").get(0).path("matchType").asText());
        query.put("formats", List.of(99));
        assertEquals(0, ok("POST", "/knowledge/search/search", viewer, query).path("total").asInt());
        query.put("formats", List.of(5));
        assertEquals(0, ok("POST", "/knowledge/search/search", outsider, query).path("total").asInt());
        assertEquals(0, ok("POST", "/knowledge/search/search", admin, query).path("total").asInt());
        long chunkId = first.path("list").get(0).path("chunkId").asLong();
        var documentPreview = ok("GET", "/knowledge/search/document-preview?chunkId=" + chunkId, viewer, null);
        assertTrue(documentPreview.path("chunks").size() > 20);
        assertArrayEquals(samples.get("真实长文.txt"), raw("GET", base() + "/system/file/download?id=" + files.getFirst(), viewer, null, null).body());
        assertNotEquals(0, call("POST", "/knowledge/doc/create", viewer, Map.of("knowledgeBaseId", knowledgeBaseId,
                "fileId", files.getFirst(), "fileUrl", "/api/system/file/download?id=" + files.getFirst())).path("code").asInt());
        var pdfSearch = ok("POST", "/knowledge/search/search", viewer, Map.of("query", "文档检索", "knowledgeBaseIds", List.of(knowledgeBaseId), "formats", List.of(1)));
        assertFalse(pdfSearch.path("list").isEmpty());
        var pdf = ok("GET", "/knowledge/search/document-preview?chunkId=" + pdfSearch.path("list").get(0).path("chunkId").asLong(), viewer, null);
        boolean recognized = false;
        for (var chunk : pdf.path("chunks")) {
            if (chunk.path("ocr").asBoolean()) {
                assertEquals(2, chunk.path("pageNumber").asInt());
                assertTrue(chunk.path("text").asText().contains("知识管理"));
                assertTrue(chunk.path("text").asText().contains("文档检索"));
                recognized = true;
            }
        }
        assertTrue(recognized, "混合PDF必须有第二页真实OCR证据");
        var model = ok("GET", "/system/model/simple-list", viewer, null).get(0);
        assertTrue(model.path("simulated").asBoolean());
        long conversation = ok("POST", "/knowledge/conversation/create", viewer,
                Map.of("title", "真实依赖中的所选证据", "knowledgeBaseIds", List.of(knowledgeBaseId))).asLong();
        var answer = ok("POST", "/knowledge/rag/ask", viewer, Map.of("conversationId", conversation,
                "question", "知识管理的用途是什么？", "selectedChunkIds", List.of(chunkId), "modelId", model.path("id").asLong()));
        assertTrue(answer.path("answered").asBoolean());
        assertTrue(answer.path("answer").asText().contains("本地模拟"));
        assertEquals(chunkId, answer.path("citations").get(0).path("chunkId").asLong());
        checkpoint("真实检索过滤分页、全文及原文件授权、所选上下文模拟RAG引用通过");

        // 删除恢复、当前成员撤权与最终文件字节物理回收。
        ok("DELETE", "/knowledge/doc/delete?id=" + textDocument, owner, null);
        assertEquals(0, ok("POST", "/knowledge/search/search", viewer, query).path("total").asInt());
        assertNotEquals(0, call("POST", "/knowledge/doc/restore?id=" + textDocument, viewer, null).path("code").asInt());
        ok("POST", "/knowledge/doc/restore?id=" + textDocument, owner, null);
        awaitAvailable(owner, textDocument);
        ok("DELETE", "/knowledge/base/delete?id=" + knowledgeBaseId, owner, null);
        assertEquals(0, ok("POST", "/knowledge/search/search", viewer, query).path("total").asInt());
        assertFileDenied(viewer, files.getFirst());
        ok("POST", "/knowledge/base/restore?id=" + knowledgeBaseId, owner, null);
        for (Long document : documents) {
            awaitAvailable(owner, document);
        }
        JsonNode members = ok("GET", "/knowledge/base/member/list?knowledgeBaseId=" + knowledgeBaseId, owner, null);
        long member = 0;
        for (var item : members) {
            if ("p0_viewer".equals(item.path("username").asText())) member = item.path("id").asLong();
        }
        assertTrue(member > 0, "恢复知识库应保留原成员关系");
        ok("DELETE", "/knowledge/base/member/delete?knowledgeBaseId=" + knowledgeBaseId + "&id=" + member, owner, null);
        assertFileDenied(viewer, files.getFirst());
        assertEquals(0, ok("POST", "/knowledge/search/search", viewer, query).path("total").asInt());
        assertFalse(ok("POST", "/knowledge/rag/ask", viewer, Map.of("conversationId", conversation,
                "question", "撤权后继续问" )).path("answered").asBoolean());
        for (Long document : documents) {
            ok("DELETE", "/knowledge/doc/delete?id=" + document, owner, null);
            ok("DELETE", "/knowledge/doc/purge?id=" + document + "&confirmed=true", owner, null);
        }
        try (var connection = connect(database)) {
            var rows = connection.createStatement().executeQuery("SELECT COUNT(*) FROM system_file_content");
            rows.next();
            assertEquals(0, rows.getInt(1), "HTTP彻底删除后必须实际回收隔离样本文件字节");
        }
        checkpoint("撤权即时生效、文档与知识库恢复、彻底删除后真实字节回收通过");
    }

    private void startApplication(long pollDelay) throws Exception {
        String testClasspath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        // 实际应用不能扫描测试类目录，避免把其他测试装配或替身混入真实验收。
        String classpath = java.util.Arrays.stream(testClasspath.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator)))
                .filter(entry -> !entry.replace('\\', '/').endsWith("/target/test-classes"))
                .collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        Path arguments = Path.of("target", "p0-application-" + ProcessHandle.current().pid() + ".args").toAbsolutePath();
        Files.writeString(arguments, "-Xmx512m\n-cp\n\"" + classpath.replace("\\", "/") + "\"\ncom.df.knowledge.KnowledgeBaseApplication\n");
        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        var builder = new ProcessBuilder(java, "@" + arguments);
        var environment = builder.environment();
        environment.put("SPRING_PROFILES_ACTIVE", "test");
        environment.put("SPRING_DATASOURCE_URL", "jdbc:mysql://127.0.0.1:3306/" + database + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true");
        environment.put("SPRING_DATASOURCE_USERNAME", mysqlUser);
        environment.put("SPRING_DATASOURCE_PASSWORD", mysqlPassword);
        environment.put("SPRING_ELASTICSEARCH_URIS", ES);
        environment.put("SERVER_PORT", Integer.toString(port));
        environment.put("KNOWLEDGE_ELASTICSEARCH_INDEX_NAME", index);
        environment.put("KNOWLEDGE_MODEL_MOCK_ENABLED", "true");
        environment.put("KNOWLEDGE_RAG_BASE_URL", "");
        environment.put("KNOWLEDGE_RAG_API_KEY", "");
        environment.put("KNOWLEDGE_RAG_MODEL", "");
        environment.put("KNOWLEDGE_INGEST_POLL_DELAY_MS", Long.toString(pollDelay));
        environment.put("KNOWLEDGE_OCR_BINARY", requireEnvironment("P0_OCR_BINARY"));
        Path log = Path.of("target", "p0-application-" + ProcessHandle.current().pid() + "-" + ++starts + ".log");
        application = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        while (System.nanoTime() < deadline) {
            assertTrue(application.isAlive(), "实际应用已退出，请检查 " + log);
            try {
                if (raw("GET", base() + "/v3/api-docs", null, null, null).statusCode() == 200) return;
            } catch (java.io.IOException ignored) {
                // 仅轮询实际端口状态，不靠任意等待假定服务就绪。
            }
            Thread.sleep(200);
        }
        fail("实际应用启动超时，请检查 " + log);
    }

    private void stopApplication() throws Exception {
        if (application != null && application.isAlive()) {
            application.destroyForcibly();
            assertTrue(application.waitFor(20, TimeUnit.SECONDS), "本次创建的后端JVM未退出");
        }
    }

    private void awaitAvailable(String token, long document) throws Exception {
        long deadline = System.nanoTime() + Duration.ofMinutes(4).toNanos();
        while (System.nanoTime() < deadline) {
            var current = ok("GET", "/knowledge/doc/get?id=" + document, token, null);
            if (current.path("status").asInt() == 4) return;
            assertNotEquals(5, current.path("status").asInt(), "真实入库失败：" + current.path("failReason").asText());
            Thread.sleep(200);
        }
        fail("真实依赖文档入库超时：" + document);
    }

    private String login(String username, String password) throws Exception {
        return ok("POST", "/system/auth/login", null, Map.of("username", username, "password", password)).path("token").asText();
    }

    private JsonNode upload(String token, String name, byte[] bytes) throws Exception {
        String boundary = "P0Boundary" + System.nanoTime();
        var output = new ByteArrayOutputStream();
        String mime = name.endsWith("pdf") ? "application/pdf" : name.endsWith("txt") || name.endsWith("md") ? "text/plain" :
                name.endsWith("docx") ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" :
                name.endsWith("pptx") ? "application/vnd.openxmlformats-officedocument.presentationml.presentation" :
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + name + "\"\r\nContent-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var response = raw("POST", base() + "/system/file/upload", token, "multipart/form-data; boundary=" + boundary, output.toByteArray());
        var value = json.readTree(response.body());
        assertEquals(0, value.path("code").asInt(), value.path("msg").asText());
        return value.path("data");
    }

    private JsonNode ok(String method, String path, String token, Object body) throws Exception {
        var value = call(method, path, token, body);
        assertEquals(0, value.path("code").asInt(), path + "：" + value.path("msg").asText());
        return value.path("data");
    }

    private JsonNode call(String method, String path, String token, Object body) throws Exception {
        return json.readTree(raw(method, base() + path, token, "application/json", body == null ? null : json.writeValueAsBytes(body)).body());
    }

    private HttpResponse<byte[]> raw(String method, String url, String token, String contentType, byte[] body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30));
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (contentType != null) request.header("Content-Type", contentType);
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body)).build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private void assertFileDenied(String token, long file) throws Exception {
        var response = raw("GET", base() + "/system/file/download?id=" + file, token, null, null);
        assertTrue(response.statusCode() == 404 || json.readTree(response.body()).path("code").asInt() != 0);
    }

    private Connection connect(String name) throws Exception {
        return DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/" + name + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false", mysqlUser, mysqlPassword);
    }

    private String base() {
        return "http://127.0.0.1:" + port + "/api";
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        assertNotNull(value, "必须由根进程安全注入 " + name);
        return value;
    }

    private static void checkpoint(String text) {
        System.out.println("P0真实依赖验收：" + text);
    }

    private static Map<String, byte[]> samples() throws Exception {
        Map<String, byte[]> samples = new LinkedHashMap<>();
        String sentence = "知识管理用于保存和检索授权文档，答案引用应指向可核对的原文。";
        samples.put("真实长文.txt", sentence.repeat(500).getBytes(StandardCharsets.UTF_8));
        samples.put("操作手册.md", "# 知识管理\n文档检索支持核对原文。".getBytes(StandardCharsets.UTF_8));
        try (var pdf = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var textPage = new PDPage();
            pdf.addPage(textPage);
            try (var stream = new PDPageContentStream(pdf, textPage)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 16);
                stream.newLineAtOffset(30, 700);
                stream.showText("Knowledge retrieval provides verifiable evidence.");
                stream.endText();
            }
            var scanPage = new PDPage();
            pdf.addPage(scanPage);
            var image = new BufferedImage(1200, 200, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 1200, 200);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("Microsoft YaHei", Font.PLAIN, 28));
            graphics.drawString("知识管理 文档检索", 30, 120);
            graphics.dispose();
            try (var stream = new PDPageContentStream(pdf, scanPage)) {
                stream.drawImage(LosslessFactory.createFromImage(pdf, image), 30, 600, 550, 92);
            }
            pdf.save(output);
            samples.put("中文混合扫描.pdf", output.toByteArray());
        }
        try (var word = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
            word.createParagraph().createRun().setText("知识管理与授权文档检索");
            var table = word.createTable(2, 2);
            table.getRow(0).getCell(0).setText("姓名");
            table.getRow(0).getCell(1).setText("分数");
            table.getRow(1).getCell(0).setText("张三");
            table.getRow(1).getCell(1).setText("95");
            word.write(output);
            samples.put("知识管理.docx", output.toByteArray());
        }
        try (var slides = new XMLSlideShow(); var output = new ByteArrayOutputStream()) {
            slides.createSlide().createTextBox().setText("知识管理：文档检索与原文核对");
            slides.write(output);
            samples.put("知识管理.pptx", output.toByteArray());
        }
        try (var excel = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = excel.createSheet("成绩");
            sheet.createRow(0).createCell(0).setCellValue("姓名");
            sheet.getRow(0).createCell(1).setCellValue("分数");
            sheet.createRow(1).createCell(0).setCellValue("张三");
            sheet.getRow(1).createCell(1).setCellValue(95);
            excel.write(output);
            samples.put("成绩.xlsx", output.toByteArray());
        }
        return samples;
    }
}
