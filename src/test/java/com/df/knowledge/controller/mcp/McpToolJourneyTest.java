package com.df.knowledge.controller.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;

import com.df.knowledge.common.exception.*;
import com.df.knowledge.config.*;
import com.df.knowledge.controller.conversation.ConversationController;
import com.df.knowledge.controller.knowledge.*;
import com.df.knowledge.controller.model.InstanceSettingsController;
import com.df.knowledge.service.conversation.ConversationServiceImpl;
import com.df.knowledge.service.doc.*;
import com.df.knowledge.service.file.FileServiceImpl;
import com.df.knowledge.service.knowledge.*;
import com.df.knowledge.service.mcp.*;
import com.df.knowledge.service.model.*;
import com.df.knowledge.service.permission.*;
import com.df.knowledge.service.rag.*;
import com.fasterxml.jackson.databind.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.*;
import org.springframework.web.bind.annotation.*;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** MCP 的真实 HTTP、数据库与业务旅程；仅外部索引、向量模型和生成端口使用替身。 */
@EnabledIfSystemProperty(named = "p0.mysql.enabled", matches = "true")
@SpringBootTest(
        classes = McpToolJourneyTest.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "server.servlet.context-path=",
            "knowledge.model.mock-enabled=false",
            "spring.http.converters.preferred-json-mapper=jackson2"
        })
class McpToolJourneyTest {
    static final String DATABASE = "df_p0_mcp_tools_" + ProcessHandle.current().pid();
    static final String ROOT = "jdbc:mysql://127.0.0.1:3306/";

    static String user() {
        return Objects.requireNonNull(System.getenv("P0_MYSQL_USER"));
    }

    static String password() {
        return Objects.requireNonNull(System.getenv("P0_MYSQL_PASSWORD"));
    }

    static Connection connection() throws SQLException {
        return DriverManager.getConnection(ROOT + DATABASE, user(), password());
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        try (var c = DriverManager.getConnection(ROOT, user(), password())) {
            c.createStatement().execute("CREATE DATABASE " + DATABASE);
        }
        try (var c = connection()) {
            ScriptUtils.executeSqlScript(c, new FileSystemResource("sql/schema.sql"));
            c.createStatement()
                    .execute(
                            "INSERT INTO system_user(id,username,password,nickname,status)"
                                + " VALUES(1,'owner','x','所有者',0),(2,'stranger','x','非成员',0),(3,'member','x','查看者',0),(4,'admin','x','实例管理员',0)");
            c.createStatement()
                    .execute(
                            "INSERT INTO system_role(id,name,code,sort,status,type)"
                                    + " VALUES(1,'管理员','instance_admin',1,0,1)");
            c.createStatement()
                    .execute("INSERT INTO system_user_role(user_id,role_id) VALUES(4,1)");
            c.createStatement()
                    .execute(
                            "INSERT INTO knowledge_base(id,name,type,owner_id)"
                                    + " VALUES(1,'共享知识',2,1),(2,'他人私密知识',1,2)");
            c.createStatement()
                    .execute(
                            "INSERT INTO"
                                + " knowledge_base_member(id,knowledge_base_id,user_id,member_role)"
                                + " VALUES(1,1,3,2)");
            c.createStatement()
                    .execute(
                            "INSERT INTO"
                                + " knowledge_doc(id,knowledge_base_id,title,file_name,format,file_size,status)"
                                + " VALUES(5,1,'沸点','沸点.pdf',1,10,4),(6,2,'秘密','秘密.txt',5,10,4)");
            c.createStatement()
                    .execute(
                            "INSERT INTO"
                                + " knowledge_doc_chunk(id,doc_id,chunk_index,text,start_offset,end_offset,page_number,ocr)"
                                + " VALUES(100,5,0,'水在标准气压下沸点是100度',0,16,2,1),(101,6,0,'不应泄露的私密文档正文',0,14,null,0)");
        }
        registry.add("spring.datasource.url", () -> ROOT + DATABASE);
        registry.add("spring.datasource.username", McpToolJourneyTest::user);
        registry.add("spring.datasource.password", McpToolJourneyTest::password);
        registry.add("mybatis-plus.configuration.map-underscore-to-camel-case", () -> true);
    }

    @AfterAll
    static void clean() throws Exception {
        try (var c = DriverManager.getConnection(ROOT, user(), password())) {
            c.createStatement().execute("DROP DATABASE IF EXISTS " + DATABASE);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.df.knowledge.dal.mysql")
    @Import({
        McpCredentialController.class,
        McpCallLogController.class,
        McpProtocolController.class,
        McpCredentialServiceImpl.class,
        McpCallLogServiceImpl.class,
        McpProtocolServiceImpl.class,
        McpToolServiceImpl.class,
        InstanceSettingsServiceImpl.class,
        InstanceSettingsController.class,
        SaTokenConfiguration.class,
        GlobalExceptionHandler.class,
        MyBatisPlusConfiguration.class,
        DefaultDBFieldHandler.class,
        SecurityConfiguration.class,
        PermissionServiceImpl.class,
        StpInterfaceImpl.class,
        KnowledgeBasePermissionServiceImpl.class,
        KnowledgeBaseServiceImpl.class,
        KnowledgeBaseMemberServiceImpl.class,
        KnowledgeBaseMemberController.class,
        com.df.knowledge.service.user.UserServiceImpl.class,
        DocServiceImpl.class,
        FileServiceImpl.class,
        DocIngestServiceImpl.class,
        DocParseServiceImpl.class,
        DocOcrServiceImpl.class,
        DocIndexServiceImpl.class,
        DocIngestExecutorConfiguration.class,
        DocSearchServiceImpl.class,
        ModelRerankService.class,
        ModelConfigurationServiceImpl.class,
        OpenAiCompatibleModelClient.class,
        RagServiceImpl.class,
        ConversationServiceImpl.class,
        ConversationController.class,
        Login.class
    })
    static class Application {
        @Bean
        AdjustableClock mcpClock() {
            return new AdjustableClock();
        }

        @Bean
        DocEmbeddingService embedding() {
            return texts -> texts.stream().map(text -> new float[512]).toList();
        }

        @Bean
        DocChunkIndexService index() {
            return mock(DocChunkIndexService.class);
        }

        @Bean
        RagGenerationPort generation() {
            return mock(RagGenerationPort.class);
        }
    }

    @RestController
    static class Login {
        @PostMapping("/system/auth/login")
        String login(@RequestParam Long id) {
            StpUtil.login(id);
            return StpUtil.getTokenValue();
        }
    }

    static class AdjustableClock extends Clock {
        Instant current = Instant.parse("2026-10-24T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }

    @LocalServerPort int port;
    @Autowired AdjustableClock clock;
    @Autowired DocChunkIndexService index;
    @Autowired RagGenerationPort generation;
    final HttpClient http = HttpClient.newHttpClient();
    final ObjectMapper json = new ObjectMapper();
    String owner, member, admin;

    @BeforeEach
    void resetFixture() throws Exception {
        clock.current = Instant.parse("2026-10-24T00:00:00Z");
        try (var c = connection()) {
            c.createStatement().execute("DELETE FROM mcp_call_log");
            c.createStatement().execute("DELETE FROM mcp_credential");
            c.createStatement().execute("UPDATE system_user SET status=0");
            c.createStatement().execute("UPDATE knowledge_base_member SET deleted=0");
            c.createStatement().execute("UPDATE knowledge_doc SET status=4,deleted=0");
            c.createStatement()
                    .execute(
                            "UPDATE system_instance_settings SET"
                                    + " mcp_enabled=1,mcp_rate_limit_per_minute=60");
        }
        reset(index, generation);
        when(index.search(anyString(), any(), any(), any(), anyString(), anyInt()))
                .thenAnswer(
                        invocation ->
                                List.of(
                                        new DocIndexHit(
                                                100L,
                                                0.03,
                                                "FUZZY".equals(invocation.getArgument(4))
                                                        ? "SEMANTIC"
                                                        : "HYBRID"),
                                        new DocIndexHit(101L, 0.02, "HYBRID")));
        when(generation.generate(any()))
                .thenReturn(new RagGenerationPort.GenerationResult(true, "沸点是100度[1]", List.of(1)));
        owner = login(1);
        member = login(3);
        admin = login(4);
    }

    String login(long id) throws Exception {
        return http.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                "http://localhost:"
                                                        + port
                                                        + "/system/auth/login?id="
                                                        + id))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .body();
    }

    JsonNode web(String method, String path, String token, Object body) throws Exception {
        var builder =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Authorization", "Bearer " + token);
        if (body != null) builder.header("Content-Type", "application/json");
        var response =
                http.send(
                        builder.method(
                                        method,
                                        body == null
                                                ? HttpRequest.BodyPublishers.noBody()
                                                : HttpRequest.BodyPublishers.ofString(
                                                        json.writeValueAsString(body)))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        return json.readTree(response.body());
    }

    JsonNode create(String login, List<String> tools, Integer days) throws Exception {
        Map<String, Object> args = new HashMap<>(Map.of("name", "业务客户端", "tools", tools));
        if (days != null) args.put("validDays", days);
        return web("POST", "/mcp/credential/create", login, args);
    }

    String token(String login) throws Exception {
        return create(login, List.of("search", "ask", "list_knowledge_bases"), null)
                .path("data")
                .path("token")
                .asText();
    }

    HttpResponse<String> rpc(String credential, String method, Object params) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
                        .header("Authorization", "Bearer " + credential)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json, text/event-stream")
                        .header("MCP-Protocol-Version", "2025-11-25")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "jsonrpc", "2.0", "id", 1, "method", method,
                                                        "params", params))))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    JsonNode tool(String credential, String name, Object arguments) throws Exception {
        var response = rpc(credential, "tools/call", Map.of("name", name, "arguments", arguments));
        assertEquals(200, response.statusCode(), response.body());
        return json.readTree(response.body()).path("result");
    }

    @Test
    void testTools_threeRealBusinessChainsAreReadOnlyAndAuthorized() throws Exception {
        // 准备参数。
        String credential = token(owner);
        long conversations =
                web("GET", "/knowledge/conversation/list", owner, null).path("data").size();
        // 调用并断言：外部索引即使命中其他用户分块，也不能泄露正文或总数。
        JsonNode searched =
                tool(
                        credential,
                        "search",
                        Map.of(
                                "query",
                                "沸点",
                                "mode",
                                "FUZZY",
                                "formats",
                                List.of(1),
                                "pageNo",
                                1,
                                "pageSize",
                                20));
        assertFalse(searched.path("isError").asBoolean());
        JsonNode page = searched.path("structuredContent");
        assertEquals(1, page.path("total").asInt());
        assertEquals("SEMANTIC", page.path("list").get(0).path("matchType").asText());
        assertFalse(searched.toString().contains("私密"));
        assertEquals(2, page.path("list").get(0).path("pageNumber").asInt());
        JsonNode bases =
                tool(credential, "list_knowledge_bases", Map.of()).path("structuredContent");
        assertEquals(1, bases.path("total").asInt());
        // 所选证据路径不依赖外部检索，且不会创建或修改聊天会话。
        when(index.search(anyString(), any(), any(), any(), anyString(), anyInt()))
                .thenThrow(new IllegalStateException("索引不可用"));
        JsonNode asked =
                tool(credential, "ask", Map.of("question", "沸点？", "selectedChunkIds", List.of(100)))
                        .path("structuredContent");
        assertTrue(asked.path("answered").asBoolean());
        assertEquals(100, asked.path("citations").get(0).path("chunkId").asInt());
        assertEquals(
                conversations,
                web("GET", "/knowledge/conversation/list", owner, null).path("data").size());
        JsonNode records = web("GET", "/mcp/call-log/page", owner, null).path("data");
        assertTrue(records.toString().contains("SUCCESS"));
        assertFalse(records.toString().contains("水在标准"));
    }

    @Test
    void testCredential_expiryBoundsDefault90AndRevocationAffectNextRequest() throws Exception {
        // 准备参数并校验生命周期边界。
        assertNotEquals(0, create(owner, List.of("search"), 0).path("code").asInt());
        assertNotEquals(0, create(owner, List.of("search"), 366).path("code").asInt());
        assertEquals(0, create(owner, List.of("search"), 365).path("code").asInt());
        JsonNode created = create(owner, List.of("search"), null).path("data");
        assertEquals("2027-01-22T00:00:00", created.path("expiresAt").asText());
        String credential = created.path("token").asText();
        assertEquals(200, rpc(credential, "ping", Map.of()).statusCode());
        clock.current = clock.current.plus(Duration.ofDays(90));
        assertEquals(401, rpc(credential, "ping", Map.of()).statusCode());
        String fresh = token(owner);
        JsonNode list = web("GET", "/mcp/credential/list", owner, null).path("data");
        long id = list.get(0).path("id").asLong();
        assertEquals(
                0,
                web("POST", "/mcp/credential/revoke?id=" + id, owner, null).path("code").asInt());
        assertEquals(401, rpc(fresh, "tools/list", Map.of()).statusCode());
    }

    @Test
    void testAuthorization_membershipRemovalImmediatelyHidesContent() throws Exception {
        String credential = token(member);
        assertEquals(
                200,
                rpc(
                                credential,
                                "initialize",
                                Map.of(
                                        "protocolVersion",
                                        "2025-11-25",
                                        "capabilities",
                                        Map.of(),
                                        "clientInfo",
                                        Map.of("name", "test", "version", "1")))
                        .statusCode());
        assertEquals(
                1,
                tool(credential, "search", Map.of("query", "沸点"))
                        .path("structuredContent")
                        .path("total")
                        .asInt());
        assertEquals(
                0,
                web("DELETE", "/knowledge/base/member/delete?knowledgeBaseId=1&id=1", owner, null)
                        .path("code")
                        .asInt());
        assertEquals(
                0,
                tool(credential, "list_knowledge_bases", Map.of())
                        .path("structuredContent")
                        .path("total")
                        .asInt());
        assertEquals(
                0,
                tool(credential, "search", Map.of("query", "沸点"))
                        .path("structuredContent")
                        .path("total")
                        .asInt());
        assertFalse(
                tool(credential, "ask", Map.of("question", "沸点？", "selectedChunkIds", List.of(100)))
                        .toString()
                        .contains("水在标准"));
    }

    @Test
    void testLimits_atomicPerCredentialAndCurrentInstanceSwitches() throws Exception {
        String credential = token(owner);
        Map<String, Object> limited =
                Map.of(
                        "mcpEnabled",
                        true,
                        "mcpRateLimitPerMinute",
                        2,
                        "ingestGlobalConcurrency",
                        2,
                        "ingestKnowledgeBaseConcurrency",
                        1);
        assertEquals(0, web("PUT", "/system/settings/update", admin, limited).path("code").asInt());
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++)
                results.add(
                        executor.submit(
                                () ->
                                        rpc(
                                                        credential,
                                                        "tools/call",
                                                        Map.of(
                                                                "name",
                                                                "list_knowledge_bases",
                                                                "arguments",
                                                                Map.of()))
                                                .statusCode()));
            List<Integer> statuses = new ArrayList<>();
            for (var result : results) statuses.add(result.get());
            assertEquals(2, statuses.stream().filter(status -> status == 200).count());
            assertEquals(6, statuses.stream().filter(status -> status == 429).count());
        }
        clock.current = clock.current.plusSeconds(60);
        assertEquals(
                200,
                rpc(
                                credential,
                                "tools/call",
                                Map.of("name", "list_knowledge_bases", "arguments", Map.of()))
                        .statusCode());
        var disabled = new HashMap<>(limited);
        disabled.put("mcpEnabled", false);
        assertEquals(
                0, web("PUT", "/system/settings/update", admin, disabled).path("code").asInt());
        assertEquals(503, rpc(credential, "tools/list", Map.of()).statusCode());
        assertEquals(403, web("GET", "/mcp/call-log/statistics", owner, null).path("code").asInt());
        JsonNode stats = web("GET", "/mcp/call-log/statistics", admin, null).path("data");
        assertTrue(stats.isArray());
        assertFalse(stats.toString().contains("knowledgeBaseIds"));
        assertFalse(stats.toString().contains("userId"));
    }

    @Test
    void testFailures_toolToggleUserDisabledAndAuditRemainSafe() throws Exception {
        JsonNode created = create(owner, List.of("search", "ask"), 1).path("data");
        String credential = created.path("token").asText();
        long id = created.path("id").asLong();
        assertEquals(
                0,
                web(
                                "PUT",
                                "/mcp/credential/update-tools",
                                owner,
                                Map.of("id", id, "tools", List.of("ask")))
                        .path("code")
                        .asInt());
        assertEquals(
                403,
                rpc(
                                credential,
                                "tools/call",
                                Map.of("name", "search", "arguments", Map.of("query", "正文不应写入审计")))
                        .statusCode());
        when(generation.generate(any()))
                .thenThrow(
                        new ServiceException(
                                com.df.knowledge.enums.ErrorCodeConstants.RAG_MODEL_UNAVAILABLE));
        assertTrue(
                tool(
                                credential,
                                "ask",
                                Map.of("question", "正文不应写入审计", "selectedChunkIds", List.of(100)))
                        .path("isError")
                        .asBoolean());
        JsonNode logs = web("GET", "/mcp/call-log/page", owner, null).path("data");
        assertTrue(logs.toString().contains("FAILED"));
        assertTrue(logs.toString().contains("REJECTED"));
        assertFalse(logs.toString().contains(credential));
        assertFalse(logs.toString().contains("正文不应写入审计"));
        try (var c = connection()) {
            c.createStatement().execute("UPDATE system_user SET status=1 WHERE id=1");
        }
        assertEquals(401, rpc(credential, "tools/list", Map.of()).statusCode());
    }

    @Test
    @EnabledIfSystemProperty(named = "p0.mcp.client.enabled", matches = "true")
    void testOfficialClient_initializationDiscoveryAndAllTools() throws Exception {
        // npm 安装官方 SDK v1.32.1 到 target/mcp-client 后运行，不使用自写协议客户端代替 SDK。
        String credential = token(owner);
        Files.copy(
                Path.of("scripts/mcp-client-check.mjs"),
                Path.of("target/mcp-client/check.mjs"),
                StandardCopyOption.REPLACE_EXISTING);
        ProcessBuilder builder =
                new ProcessBuilder(
                        System.getenv().getOrDefault("P0_NODE_BINARY", "node"),
                        "target/mcp-client/check.mjs");
        builder.environment().put("MCP_BASE_URL", "http://localhost:" + port + "/mcp");
        builder.environment().put("MCP_TOKEN", credential);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        if (!process.waitFor(45, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("官方 MCP 客户端超时");
        }
        String output =
                new String(
                        process.getInputStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), output);
        assertTrue(output.contains("MCP_OFFICIAL_CLIENT_OK"), output);
        assertEquals(
                0, web("GET", "/knowledge/conversation/list", owner, null).path("data").size());
    }
}
