package com.df.knowledge.controller.model;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.exception.GlobalExceptionHandler;
import com.df.knowledge.config.SaTokenConfiguration;
import com.df.knowledge.service.model.*;
import com.df.knowledge.service.permission.*;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.*;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** 真实 MySQL、真实 HTTP 与可控外部模型的管理旅程。 */
@SpringBootTest(classes = ModelConfigurationControllerTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=test", "knowledge.model.mock-enabled=true", "knowledge.rag.base-url=http://127.0.0.1:1/v1",
                "knowledge.rag.api-key=legacy-key", "knowledge.rag.model=legacy-model", "server.servlet.context-path=", "spring.http.converters.preferred-json-mapper=jackson2"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "p0.mysql.enabled", matches = "true")
class ModelConfigurationControllerTest {
    private static final String DATABASE = "df_p0_model_" + ProcessHandle.current().pid();
    private static final String DB_ROOT = "jdbc:mysql://localhost:3306/";
    private static final String DB_USER = System.getenv().getOrDefault("P0_MYSQL_USER", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("P0_MYSQL_PASSWORD", "root");
    @LocalServerPort int port;
    private final ObjectMapper json = new ObjectMapper();
    private HttpServer modelServer;
    private java.util.concurrent.ExecutorService modelExecutor;
    private final AtomicInteger status = new AtomicInteger(200);
    private String adminToken;
    private String userToken;

    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) throws Exception {
        try (Connection connection = DriverManager.getConnection(DB_ROOT, DB_USER, DB_PASSWORD)) {
            connection.createStatement().execute("CREATE DATABASE " + DATABASE);
        }
        try (Connection connection = DriverManager.getConnection(DB_ROOT + DATABASE, DB_USER, DB_PASSWORD)) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/schema.sql"));
            connection.createStatement().execute("INSERT INTO system_role(id,name,code,sort,status,type) VALUES (1,'管理员','instance_admin',1,0,1)");
            connection.createStatement().execute("INSERT INTO system_user_role(user_id,role_id) VALUES (1,1)");
        }
        registry.add("spring.datasource.url", () -> DB_ROOT + DATABASE);
        registry.add("spring.datasource.username", () -> DB_USER);
        registry.add("spring.datasource.password", () -> DB_PASSWORD);
        registry.add("mybatis-plus.configuration.map-underscore-to-camel-case", () -> true);
    }
    @BeforeAll void start() throws Exception {
        modelServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        modelServer.createContext("/v1/chat/completions", exchange -> {
            String request = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String body = status.get() == 200 ? "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}"
                    : "secret-key provider body " + request;
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        modelServer.createContext("/v1/rerank", exchange -> {
            byte[] bytes = "{\"results\":[{\"index\":0,\"relevance_score\":0.9}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        modelServer.createContext("/slow/chat/completions", exchange -> {
            try {
                new java.util.concurrent.CountDownLatch(1).await(2, java.util.concurrent.TimeUnit.SECONDS);
                exchange.sendResponseHeaders(200, 0);
            } catch (Exception ignored) {
                // 客户端超时后连接关闭是本场景预期行为
            } finally { exchange.close(); }
        });
        // 超时请求不能占住单个测试服务线程，阻塞本应立即成功的其他连接。
        modelExecutor = java.util.concurrent.Executors.newCachedThreadPool();
        modelServer.setExecutor(modelExecutor);
        modelServer.start();
        adminToken = call("POST", "/system/auth/login?id=1", null, null).path("data").asText();
        userToken = call("POST", "/system/auth/login?id=2", null, null).path("data").asText();
    }
    @AfterAll void stop() throws Exception {
        if (modelServer != null) modelServer.stop(0);
        if (modelExecutor != null) modelExecutor.shutdownNow();
        try (Connection connection = DriverManager.getConnection(DB_ROOT, DB_USER, DB_PASSWORD)) {
            connection.createStatement().execute("DROP DATABASE " + DATABASE);
        }
    }
    @Test @Order(1) void testCreate_namedModelCanBeRetrievedWithoutSecret() throws Exception {
        // 准备参数
        Map<String,Object> request = configuration("管理旅程");
        JsonNode created = call("POST", "/system/model/create", adminToken, request);
        assertEquals(0, created.path("code").asInt());
        long id = created.path("data").asLong();
        // 调用并断言
        JsonNode saved = call("GET", "/system/model/get?id=" + id, adminToken, null).path("data");
        assertEquals("管理旅程", saved.path("name").asText());
        assertTrue(saved.path("hasApiKey").asBoolean());
        assertFalse(saved.toString().contains("secret-key"));
        assertFalse(saved.has("apiKey"));
        assertEquals("UNTESTED", saved.path("testStatus").asText());
        JsonNode tested = call("POST", "/system/model/test?id=" + id, adminToken, null).path("data");
        assertEquals("SUCCESS", tested.path("testStatus").asText());
        assertFalse(tested.path("lastTestTime").isNull());
        assertEquals("SUCCESS", call("GET", "/system/model/get?id=" + id, adminToken, null).path("data").path("testStatus").asText());
        request.put("id", id); request.remove("apiKey"); request.put("name", "重命名后保留密钥");
        assertEquals(0, call("PUT", "/system/model/update", adminToken, request).path("code").asInt());
        JsonNode updated = call("GET", "/system/model/get?id=" + id, adminToken, null).path("data");
        assertTrue(updated.path("hasApiKey").asBoolean());
        assertEquals("UNTESTED", updated.path("testStatus").asText());
        assertTrue(updated.path("lastTestTime").isNull());
        assertTrue(updated.path("testError").isNull());
        assertEquals(403, call("POST", "/system/model/create", userToken, request).path("code").asInt());
        assertEquals(403, call("DELETE", "/system/model/delete?id=" + id, userToken, null).path("code").asInt());
        assertEquals(403, call("GET", "/system/model/list", userToken, null).path("code").asInt());
        assertEquals(403, call("GET", "/system/model/get?id=" + id, userToken, null).path("code").asInt());
        assertEquals(403, call("PUT", "/system/model/update", userToken, request).path("code").asInt());
        assertEquals(403, call("POST", "/system/model/test?id=" + id, userToken, null).path("code").asInt());
        JsonNode choices = call("GET", "/system/model/simple-list", userToken, null).path("data");
        assertTrue(choices.toString().contains("重命名后保留密钥"));
        assertFalse(choices.toString().contains("baseUrl"));
    }
    @Test @Order(1) void testConnection_authFailureIsPersistedWithoutSecretOrProviderBody() throws Exception {
        // 准备参数
        long id = call("POST", "/system/model/create", adminToken, configuration("鉴权失败旅程")).path("data").asLong();
        status.set(401);
        try {
            // 调用并断言
            JsonNode result = call("POST", "/system/model/test?id=" + id, adminToken, null).path("data");
            assertEquals("FAILED", result.path("testStatus").asText());
            assertTrue(result.path("testError").asText().contains("鉴权失败"));
            assertFalse(result.toString().contains("secret-key"));
            assertFalse(result.toString().contains("provider body"));
        } finally { status.set(200); }
    }
    @Test @Order(1) void testSettings_defaultsPersistUpdatesAndRejectOrdinaryUser() throws Exception {
        // 调用并断言
        JsonNode initial = call("GET", "/system/settings/get", adminToken, null).path("data");
        assertEquals(60, initial.path("mcpRateLimitPerMinute").asInt());
        assertEquals(2, initial.path("ingestGlobalConcurrency").asInt());
        assertEquals(1, initial.path("ingestKnowledgeBaseConcurrency").asInt());
        Map<String,Object> request = Map.of("mcpEnabled", false, "mcpRateLimitPerMinute", 25,
                "ingestGlobalConcurrency", 4, "ingestKnowledgeBaseConcurrency", 2);
        assertEquals(403, call("PUT", "/system/settings/update", userToken, request).path("code").asInt());
        assertEquals(0, call("PUT", "/system/settings/update", adminToken, request).path("code").asInt());
        JsonNode saved = call("GET", "/system/settings/get", adminToken, null).path("data");
        assertFalse(saved.path("mcpEnabled").asBoolean());
        assertEquals(25, saved.path("mcpRateLimitPerMinute").asInt());
        Map<String,Object> bad = new HashMap<>(request); bad.put("ingestGlobalConcurrency", 0);
        assertEquals(400, call("PUT", "/system/settings/update", adminToken, bad).path("code").asInt());
    }
    @Test @Order(1) void testConnection_timeoutAndUnreachableHaveSafeReasons() throws Exception {
        // 准备参数
        Map<String,Object> slow = configuration("超时旅程");
        slow.put("baseUrl", "http://127.0.0.1:" + modelServer.getAddress().getPort() + "/slow");
        long id = call("POST", "/system/model/create", adminToken, slow).path("data").asLong();
        // 调用并断言
        JsonNode result = call("POST", "/system/model/test?id=" + id, adminToken, null).path("data");
        assertEquals("FAILED", result.path("testStatus").asText());
        assertTrue(result.path("testError").asText().contains("超时"));
        Map<String,Object> unreachable = configuration("不可达旅程");
        unreachable.put("baseUrl", "http://127.0.0.1:1/v1");
        id = call("POST", "/system/model/create", adminToken, unreachable).path("data").asLong();
        result = call("POST", "/system/model/test?id=" + id, adminToken, null).path("data");
        assertEquals("FAILED", result.path("testStatus").asText());
        assertTrue(result.path("testError").asText().contains("不可达"));
    }
    @Test @Order(1) void testRerank_realConnectionAndSingleConfiguration() throws Exception {
        // 准备参数
        Map<String,Object> request = configuration("重排模型");
        request.put("kind", "RERANK");
        long id = call("POST", "/system/model/create", adminToken, request).path("data").asLong();
        // 调用并断言
        assertEquals("SUCCESS", call("POST", "/system/model/test?id=" + id, adminToken, null)
                .path("data").path("testStatus").asText());
        request.put("id", id);
        request.put("name", "不得伪造编号绕过单例");
        assertEquals(1_001_010_009, call("POST", "/system/model/create", adminToken, request).path("code").asInt());
        call("DELETE", "/system/model/delete?id=" + id, adminToken, null);
    }
    @Test @Order(1) void testSimulation_isNamedAndClearlySeparatedFromRealConnection() throws Exception {
        // 调用并断言：用户明确选择模拟项，不伪装成功连接外部模型
        JsonNode models = call("GET", "/system/model/simple-list", userToken, null).path("data");
        JsonNode simulated = null;
        for (JsonNode model : models) {
            if (model.path("simulated").asBoolean()) simulated = model;
        }
        assertNotNull(simulated);
        assertTrue(simulated.path("name").asText().contains("非真实生成"));
        long id = simulated.path("id").asLong();
        JsonNode tested = call("POST", "/system/model/test?id=" + id, adminToken, null).path("data");
        assertEquals("SIMULATED", tested.path("testStatus").asText());
        Map<String,Object> forbidden = configuration("不得自建 mock 地址");
        forbidden.put("baseUrl", "mock://local");
        assertEquals(1_001_010_008, call("POST", "/system/model/create", adminToken, forbidden).path("code").asInt());
    }
    @Test @Order(100) void testMigration_restartPreservesModelsAndDoesNotResurrectDeletedLegacyConfiguration() throws Exception {
        // 准备参数：初次启动已经迁移环境配置，删除后验证下一次启动不会复活
        JsonNode models = call("GET", "/system/model/list", adminToken, null).path("data");
        JsonNode legacy = null;
        for (JsonNode model : models) {
            if ("旧配置迁移的对话模型".equals(model.path("name").asText())) legacy = model;
        }
        assertNotNull(legacy);
        assertTrue(legacy.path("hasApiKey").asBoolean());
        assertFalse(legacy.toString().contains("legacy-key"));
        long id = legacy.path("id").asLong();
        assertEquals(0, call("DELETE", "/system/model/delete?id=" + id, adminToken, null).path("code").asInt());
        // 调用：启动独立的新应用实例读取同一个隔离数据库，不复用服务或缓存
        try (var restarted = new org.springframework.boot.builder.SpringApplicationBuilder(TestApplication.class).run(
                "--server.port=0", "--server.servlet.context-path=", "--spring.profiles.active=test",
                "--spring.datasource.url=" + DB_ROOT + DATABASE, "--spring.datasource.username=" + DB_USER,
                "--spring.datasource.password=" + DB_PASSWORD,
                "--knowledge.model.mock-enabled=true", "--knowledge.rag.base-url=http://127.0.0.1:1/v1",
                "--knowledge.rag.api-key=legacy-key", "--knowledge.rag.model=legacy-model")) {
            port = Integer.parseInt(restarted.getEnvironment().getProperty("local.server.port"));
            adminToken = call("POST", "/system/auth/login?id=1", null, null).path("data").asText();
            JsonNode afterRestart = call("GET", "/system/model/list", adminToken, null).path("data");
            // 断言
            assertFalse(afterRestart.toString().contains("旧配置迁移的对话模型"));
            assertTrue(afterRestart.toString().contains("重命名后保留密钥"));
            JsonNode settings = call("GET", "/system/settings/get", adminToken, null).path("data");
            assertEquals(25, settings.path("mcpRateLimitPerMinute").asInt());
            assertFalse(settings.path("mcpEnabled").asBoolean());
        }
    }
    private Map<String,Object> configuration(String name) {
        Map<String,Object> value = new HashMap<>(); value.put("name", name); value.put("kind", "CHAT");
        value.put("baseUrl", "http://127.0.0.1:" + modelServer.getAddress().getPort() + "/v1");
        value.put("apiKey", "secret-key"); value.put("model", "test-model"); value.put("timeoutSeconds", 1);
        return value;
    }
    private JsonNode call(String method, String path, String token, Object body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        try (HttpClient client = HttpClient.newHttpClient()) {
            return json.readTree(client.send(request.build(), HttpResponse.BodyHandlers.ofString()).body());
        }
    }
    @SpringBootConfiguration @EnableAutoConfiguration
    @MapperScan({"com.df.knowledge.dal.mysql.model", "com.df.knowledge.dal.mysql.permission"})
    @Import({ModelConfigurationController.class, InstanceSettingsController.class, ModelConfigurationServiceImpl.class,
            InstanceSettingsServiceImpl.class, OpenAiCompatibleModelClient.class, PermissionServiceImpl.class,
            StpInterfaceImpl.class, SaTokenConfiguration.class, GlobalExceptionHandler.class, LegacyModelConfigurationMigration.class, LocalMockModelConfiguration.class, LoginFixture.class})
    static class TestApplication { }
    /** 仅测试装配的登录入口，用户角色仍通过真实 MySQL RBAC 读取。 */
    @RestController static class LoginFixture {
        @PostMapping("/system/auth/login") CommonResult<String> login(@RequestParam Long id) {
            StpUtil.login(id); return CommonResult.success(StpUtil.getTokenValue());
        }
    }
}
