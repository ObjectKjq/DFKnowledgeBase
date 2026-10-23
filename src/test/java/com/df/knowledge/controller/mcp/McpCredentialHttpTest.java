package com.df.knowledge.controller.mcp;

import static org.junit.jupiter.api.Assertions.*;

import cn.dev33.satoken.stp.StpUtil;

import com.df.knowledge.common.exception.GlobalExceptionHandler;
import com.df.knowledge.config.*;
import com.df.knowledge.service.mcp.*;
import com.df.knowledge.service.model.InstanceSettingsServiceImpl;
import com.fasterxml.jackson.databind.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.*;
import org.springframework.web.bind.annotation.*;

import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;

/** 凭证管理真实数据库与 HTTP 旅程。 */
@EnabledIfSystemProperty(named = "p0.mysql.enabled", matches = "true")
@SpringBootTest(
        classes = McpCredentialHttpTest.Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "server.servlet.context-path=",
            "spring.http.converters.preferred-json-mapper=jackson2"
        })
class McpCredentialHttpTest {
    static final String DB = "df_p0_mcp_" + ProcessHandle.current().pid();
    static final String URL = "jdbc:mysql://127.0.0.1:3306/";

    static String user() {
        return Objects.requireNonNull(System.getenv("P0_MYSQL_USER"), "请设置 P0_MYSQL_USER");
    }

    static String password() {
        return Objects.requireNonNull(System.getenv("P0_MYSQL_PASSWORD"), "请设置 P0_MYSQL_PASSWORD");
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        try (var c = DriverManager.getConnection(URL, user(), password())) {
            c.createStatement().execute("CREATE DATABASE " + DB);
        }
        try (var c = DriverManager.getConnection(URL + DB, user(), password())) {
            ScriptUtils.executeSqlScript(c, new FileSystemResource("sql/schema.sql"));
            c.createStatement()
                    .execute(
                            "INSERT INTO system_user(id,username,password,nickname,status) VALUES"
                                    + " (1,'mcp-owner','x','用户一',0),(2,'mcp-other','x','用户二',0)");
        }
        registry.add("spring.datasource.url", () -> URL + DB);
        registry.add("spring.datasource.username", McpCredentialHttpTest::user);
        registry.add("spring.datasource.password", McpCredentialHttpTest::password);
        registry.add("mybatis-plus.configuration.map-underscore-to-camel-case", () -> true);
    }

    @AfterAll
    static void clean() throws Exception {
        try (var c = DriverManager.getConnection(URL, user(), password())) {
            c.createStatement().execute("DROP DATABASE IF EXISTS " + DB);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.df.knowledge.dal.mysql")
    @Import({
        McpCredentialController.class,
        McpCredentialServiceImpl.class,
        InstanceSettingsServiceImpl.class,
        McpConfiguration.class,
        SaTokenConfiguration.class,
        GlobalExceptionHandler.class,
        MyBatisPlusConfiguration.class,
        DefaultDBFieldHandler.class,
        Login.class,
        McpCallLogController.class,
        McpCallLogServiceImpl.class,
        McpProtocolController.class,
        McpProtocolServiceImpl.class
    })
    static class Application {
        @Bean
        McpToolServiceImpl tools() {
            return new McpToolServiceImpl(
                    null, null, null, new ObjectMapper().findAndRegisterModules());
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

    @BeforeEach
    void reset() throws Exception {
        try (var c = DriverManager.getConnection(URL + DB, user(), password())) {
            c.createStatement().execute("DELETE FROM mcp_call_log");
            c.createStatement().execute("DELETE FROM mcp_credential");
        }
    }

    @LocalServerPort int port;
    final ObjectMapper json = new ObjectMapper();
    final HttpClient client = HttpClient.newHttpClient();

    String login(long id) throws Exception {
        return client.send(
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

    JsonNode call(String method, String path, String token, Object body) throws Exception {
        var builder =
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Authorization", "Bearer " + token);
        if (body != null) builder.header("Content-Type", "application/json");
        var response =
                client.send(
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

    @Test
    void testAudit_protocolRejectionIsPrivateAndContainsNoSecrets() throws Exception {
        // 准备参数。
        String owner = login(1), other = login(2);
        JsonNode created =
                call(
                        "POST",
                        "/mcp/credential/create",
                        owner,
                        Map.of("name", "审计客户端", "tools", List.of()));
        String credential = created.path("data").path("token").asText();
        var response =
                client.send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
                                .header("Authorization", "Bearer " + credential)
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json, text/event-stream")
                                .header("MCP-Protocol-Version", "2025-11-25")
                                .POST(
                                        HttpRequest.BodyPublishers.ofString(
                                                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"search\",\"arguments\":{\"query\":\"不应保存的正文\"}}}"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
        // 调用并断言：本人记录可追踪拒绝，其他用户不可见。
        JsonNode records = call("GET", "/mcp/call-log/page", owner, null).path("data");
        assertTrue(records.path("total").asLong() > 0);
        assertTrue(records.toString().contains("TOOL_DENIED"));
        assertFalse(records.toString().contains(credential));
        assertFalse(records.toString().contains("不应保存的正文"));
        assertEquals(
                0,
                call("GET", "/mcp/call-log/page", other, null).path("data").path("total").asLong());
    }

    @Test
    void testCreate_onlyCreationReturnsTokenAndListIsPrivate() throws Exception {
        // 准备参数。
        String owner = login(1), other = login(2);
        JsonNode created =
                call(
                        "POST",
                        "/mcp/credential/create",
                        owner,
                        Map.of("name", "客户端", "tools", List.of("list_knowledge_bases")));
        assertEquals(0, created.path("code").asInt(), created.toString());
        assertTrue(created.path("data").path("token").asText().startsWith("dfkb_mcp_"));
        // 调用并断言。
        JsonNode list = call("GET", "/mcp/credential/list", owner, null).path("data");
        assertEquals(1, list.size());
        assertFalse(list.get(0).has("token"));
        assertFalse(list.get(0).has("tokenHash"));
        assertEquals(0, call("GET", "/mcp/credential/list", other, null).path("data").size());
        long id = created.path("data").path("id").asLong();
        assertNotEquals(
                0,
                call("POST", "/mcp/credential/revoke?id=" + id, other, null).path("code").asInt());
        assertEquals(
                0,
                call("POST", "/mcp/credential/revoke?id=" + id, owner, null).path("code").asInt());
        assertFalse(
                call("GET", "/mcp/credential/list", owner, null)
                        .path("data")
                        .get(0)
                        .path("revokedAt")
                        .isNull());
    }
}
