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

    @Test
    void testMigration_oldSchema012Through016RetainsHttpDataAndRebuildsRealIndex() throws Exception {
        // 准备参数：从固定旧基点创建独立实例，包含真实旧文件、分块、会话和答案。
        var git = new ProcessBuilder("git", "show", "b84b7fd:sql/schema.sql").start();
        byte[] oldSchema = git.getInputStream().readAllBytes();
        assertTrue(git.waitFor(10, TimeUnit.SECONDS));
        assertEquals(0, git.exitValue());
        byte[] original = "legacy-text\n".getBytes(StandardCharsets.UTF_8);
        try (var connection = connect(database)) {
            ScriptUtils.executeSqlScript(connection, new org.springframework.core.io.support.EncodedResource(
                    new org.springframework.core.io.ByteArrayResource(oldSchema), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/data.sql"));
            connection.createStatement().execute("INSERT INTO knowledge_base(id,name,type,owner_id) VALUES(701,'旧知识库',1,1)");
            connection.createStatement().execute("INSERT INTO system_file(id,name,type,size,creator) VALUES(701,'legacy.txt','text/plain',12,'1')");
            try (var statement = connection.prepareStatement("INSERT INTO system_file_content(file_id,content,creator) VALUES(701,?,'1')")) {
                statement.setBytes(1, original);
                statement.executeUpdate();
            }
            connection.createStatement().execute("INSERT INTO knowledge_doc(id,knowledge_base_id,title,file_name,format,file_size,file_id,file_url,status) "
                    + "VALUES(701,701,'旧文档','legacy.txt',5,12,701,'/api/system/file/download?id=701',4)");
            connection.createStatement().execute("INSERT INTO knowledge_doc_chunk(id,doc_id,chunk_index,text,start_offset,end_offset) VALUES(701,701,0,'legacy-text',0,11)");
            connection.createStatement().execute("INSERT INTO knowledge_conversation(id,user_id,title,retrieval_scope) VALUES(701,1,'旧会话','[701]')");
            connection.createStatement().execute("INSERT INTO knowledge_message(id,conversation_id,type,content) VALUES(701,701,'answer',"
                    + "'{\"answer\":\"历史答案[1]\",\"citations\":[{\"number\":1,\"knowledgeBaseId\":701,\"docId\":701,\"chunkId\":701}]}')");
            List<Path> migrations;
            try (var paths = Files.list(Path.of("sql/upgrade"))) {
                migrations = paths.filter(path -> path.getFileName().toString().matches("01[2-6]-.*\\.sql")).sorted().toList();
            }
            assertEquals(5, migrations.size(), "完整存量验收必须包含SQL012至016");
            for (Path migration : migrations) {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(migration));
            }
        }
        startApplication(100);
        String admin = login("admin", "admin123");
        // 调用与断言：迁移后的真实HTTP保留旧文件及会话，恢复文档时经过真实向量和索引链路。
        var conversation = ok("GET", "/knowledge/conversation/get?id=701", admin, null);
        assertEquals("HYBRID", conversation.path("state").path("mode").asText());
        assertEquals(701, conversation.path("state").path("knowledgeBaseIds").get(0).asInt());
        assertEquals("历史答案[1]", conversation.path("messages").get(0).path("content").path("answer").asText());
        assertArrayEquals(original, raw("GET", base() + "/system/file/download?id=701", admin, null, null).body());
        assertEquals("legacy-text", ok("GET", "/knowledge/search/preview?chunkId=701", admin, null).path("text").asText());
        assertFalse(ok("POST", "/mcp/credential/create", admin, Map.of("name", "迁移后MCP", "tools", List.of("search"))).path("token").asText().isBlank());
        ok("DELETE", "/knowledge/doc/delete?id=701", admin, null);
        ok("POST", "/knowledge/doc/restore?id=701", admin, null);
        awaitAvailable(admin, 701);
        assertTrue(ok("POST", "/knowledge/search/search", admin,
                Map.of("query", "legacy", "knowledgeBaseIds", List.of(701))).path("total").asInt() > 0);
        assertEquals("历史答案[1]", ok("GET", "/knowledge/conversation/get?id=701", admin, null)
                .path("messages").get(0).path("content").path("answer").asText());
        checkpoint("旧基点012至016升级后HTTP保留文件/历史会话，真实重建索引与MCP凭证创建通过");
    }

    @Test
    void testMcp_officialSdkRealDependenciesAndSameConnectionRevocation() throws Exception {
        // 准备参数：使用实际应用和真实入库，官方客户端不替代任何业务链路。
        try (var connection = connect(database)) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/schema.sql"));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/data.sql"));
        }
        startApplication(100);
        String admin = login("admin", "admin123");
        for (String username : List.of("mcp_owner", "mcp_viewer")) {
            ok("POST", "/system/user/create", admin, Map.of("username", username, "password", "Journey123!",
                    "nickname", username, "roleIds", List.of(2)));
        }
        String owner = login("mcp_owner", "Journey123!");
        String viewer = login("mcp_viewer", "Journey123!");
        long knowledgeBaseId = ok("POST", "/knowledge/base/create", owner,
                Map.of("name", "MCP真实依赖验收", "type", 2)).asLong();
        ok("POST", "/knowledge/base/member/invite", owner,
                Map.of("knowledgeBaseId", knowledgeBaseId, "username", "mcp_viewer", "memberRole", 2));
        ok("PUT", "/system/settings/update", admin, Map.of("mcpEnabled", true, "mcpRateLimitPerMinute", 60,
                "ingestGlobalConcurrency", 1, "ingestKnowledgeBaseConcurrency", 1));
        byte[] text = "知识管理用于保存和检索授权文档，答案引用应指向可核对的原文。".repeat(500).getBytes(StandardCharsets.UTF_8);
        var file = upload(owner, "MCP真实长文.txt", text);
        long document = ok("POST", "/knowledge/doc/create", owner, Map.of("knowledgeBaseId", knowledgeBaseId,
                "fileId", file.path("id").asLong(), "fileUrl", file.path("url").asText())).asLong();
        awaitAvailable(owner, document);
        long chunk = ok("POST", "/knowledge/search/search", viewer, Map.of("query", "知识管理",
                "knowledgeBaseIds", List.of(knowledgeBaseId))).path("list").get(0).path("chunkId").asLong();
        long model = ok("GET", "/system/model/simple-list", viewer, null).get(0).path("id").asLong();
        long member = 0;
        for (var item : ok("GET", "/knowledge/base/member/list?knowledgeBaseId=" + knowledgeBaseId, owner, null)) {
            if ("mcp_viewer".equals(item.path("username").asText())) member = item.path("id").asLong();
        }
        assertTrue(member > 0);
        var credential = ok("POST", "/mcp/credential/create", viewer,
                Map.of("name", "官方SDK真实链路", "tools", List.of("search", "ask", "list_knowledge_bases")));
        Path script = Path.of("target/mcp-client/real-dependencies.mjs");
        Files.copy(Path.of("scripts/mcp-real-dependency-check.mjs"), script, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        var builder = new ProcessBuilder(System.getenv().getOrDefault("P0_NODE_BINARY", "node"), script.toString());
        var environment = builder.environment();
        environment.put("MCP_BASE_URL", base() + "/mcp");
        environment.put("MCP_WEB_BASE_URL", base() + "/");
        environment.put("MCP_WEB_OWNER_TOKEN", owner);
        environment.put("MCP_WEB_VIEWER_TOKEN", viewer);
        environment.put("MCP_TOKEN", credential.path("token").asText());
        environment.put("MCP_CREDENTIAL_ID", credential.path("id").asText());
        environment.put("MCP_MEMBER_ID", Long.toString(member));
        environment.put("MCP_KNOWLEDGE_BASE_ID", Long.toString(knowledgeBaseId));
        environment.put("MCP_CHUNK_ID", Long.toString(chunk));
        environment.put("MCP_MODEL_ID", Long.toString(model));
        // 调用：SDK始终保持同一客户端，网页入口撤权与撤销凭证后继续请求。
        Process sdk = builder.redirectErrorStream(true).start();
        try {
            assertTrue(sdk.waitFor(60, TimeUnit.SECONDS), "真实依赖官方MCP客户端超时");
            String output = new String(sdk.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // 断言：握手、三工具、只读审计与动态授权均由SDK脚本核验。
            assertEquals(0, sdk.exitValue(), output);
            assertTrue(output.contains("P0_REAL_MCP_SDK_OK"), output);
            checkpoint(output.strip());
        } finally {
            if (sdk.isAlive()) {
                sdk.destroyForcibly();
                assertTrue(sdk.waitFor(10, TimeUnit.SECONDS));
            }
        }
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
