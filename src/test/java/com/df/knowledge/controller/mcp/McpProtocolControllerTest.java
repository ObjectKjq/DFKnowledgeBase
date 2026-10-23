package com.df.knowledge.controller.mcp;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.df.knowledge.dal.dataobject.mcp.McpCredentialDO;
import com.df.knowledge.dal.dataobject.model.InstanceSettingsDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.dal.mysql.mcp.McpCallLogMapper;
import com.df.knowledge.dal.mysql.mcp.McpCredentialMapper;
import com.df.knowledge.dal.mysql.user.UserMapper;
import com.df.knowledge.service.mcp.McpCredentialServiceImpl;
import com.df.knowledge.service.mcp.McpProtocolServiceImpl;
import com.df.knowledge.service.mcp.McpToolServiceImpl;
import com.df.knowledge.service.model.InstanceSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.*;

/** MCP 传输边界测试，不替代真实数据库与工具旅程。 */
class McpProtocolControllerTest {
    private org.springframework.test.web.servlet.MockMvc authenticatedMvc() {
        McpCredentialMapper credentials =
                mock(McpCredentialMapper.class, withSettings().mockMaker("mock-maker-proxy"));
        UserMapper users = mock(UserMapper.class, withSettings().mockMaker("mock-maker-proxy"));
        var settingsMapper =
                mock(
                        com.df.knowledge.dal.mysql.model.InstanceSettingsMapper.class,
                        withSettings().mockMaker("mock-maker-proxy"));
        InstanceSettingsService settings =
                new com.df.knowledge.service.model.InstanceSettingsServiceImpl(settingsMapper);
        when(credentials.selectByTokenHash(anyString()))
                .thenReturn(
                        new McpCredentialDO()
                                .setId(1L)
                                .setUserId(2L)
                                .setTools("list_knowledge_bases")
                                .setExpiresAt(LocalDateTime.of(2027, 1, 1, 0, 0)));
        when(users.selectById(2L)).thenReturn(new UserDO().setStatus(0));
        when(settingsMapper.selectById(1L))
                .thenReturn(
                        new InstanceSettingsDO().setMcpEnabled(true).setMcpRateLimitPerMinute(60));
        Clock clock = Clock.fixed(Instant.parse("2026-10-24T00:00:00Z"), ZoneOffset.UTC);
        var credentialService = new McpCredentialServiceImpl(credentials, users, settings, clock);
        var protocol =
                new McpProtocolServiceImpl(
                        credentialService,
                        mock(McpCallLogMapper.class, withSettings().mockMaker("mock-maker-proxy")),
                        new McpToolServiceImpl(null, null, null),
                        new ObjectMapper(),
                        clock,
                        "");
        return MockMvcBuilders.standaloneSetup(new McpProtocolController(protocol)).build();
    }

    @Test
    void testRequest_trailingJsonIsRejected() throws Exception {
        // 调用并断言：一个 HTTP 请求只允许一个 JSON-RPC 消息。
        authenticatedMvc()
                .perform(
                        post("/mcp")
                                .header("Authorization", "Bearer dfkb_mcp_test")
                                .header("MCP-Protocol-Version", "2025-11-25")
                                .contentType("application/json")
                                .accept("application/json", "text/event-stream")
                                .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"} {}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(-32700));
    }

    @Test
    void testNotification_initializedHasNoJsonRpcResponse() throws Exception {
        authenticatedMvc()
                .perform(
                        post("/mcp")
                                .header("Authorization", "Bearer dfkb_mcp_test")
                                .header("MCP-Protocol-Version", "2025-11-25")
                                .contentType("application/json")
                                .accept("application/json", "text/event-stream")
                                .content(
                                        "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @Test
    void testInitialize_validCredentialNegotiatesSupportedVersion() throws Exception {
        // 准备参数：仅数据库边界与时间使用替身，协议和凭证校验为真实实现。
        McpCredentialMapper credentials =
                mock(McpCredentialMapper.class, withSettings().mockMaker("mock-maker-proxy"));
        UserMapper users = mock(UserMapper.class, withSettings().mockMaker("mock-maker-proxy"));
        var settingsMapper =
                mock(
                        com.df.knowledge.dal.mysql.model.InstanceSettingsMapper.class,
                        withSettings().mockMaker("mock-maker-proxy"));
        InstanceSettingsService settings =
                new com.df.knowledge.service.model.InstanceSettingsServiceImpl(settingsMapper);
        when(credentials.selectByTokenHash(anyString()))
                .thenReturn(
                        new McpCredentialDO()
                                .setId(1L)
                                .setUserId(2L)
                                .setTools("list_knowledge_bases")
                                .setExpiresAt(LocalDateTime.of(2027, 1, 1, 0, 0)));
        when(users.selectById(2L)).thenReturn(new UserDO().setStatus(0));
        when(settingsMapper.selectById(1L))
                .thenReturn(
                        new InstanceSettingsDO().setMcpEnabled(true).setMcpRateLimitPerMinute(60));
        Clock clock = Clock.fixed(Instant.parse("2026-10-24T00:00:00Z"), ZoneOffset.UTC);
        var credentialService = new McpCredentialServiceImpl(credentials, users, settings, clock);
        var protocol =
                new McpProtocolServiceImpl(
                        credentialService,
                        mock(McpCallLogMapper.class, withSettings().mockMaker("mock-maker-proxy")),
                        new McpToolServiceImpl(null, null, null),
                        new ObjectMapper(),
                        clock,
                        "");
        // 调用并断言。
        MockMvcBuilders.standaloneSetup(new McpProtocolController(protocol))
                .build()
                .perform(
                        post("/mcp")
                                .header("Authorization", "Bearer dfkb_mcp_test")
                                .contentType("application/json")
                                .accept("application/json", "text/event-stream")
                                .content(
                                        "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-11-25\",\"capabilities\":{},\"clientInfo\":{\"name\":\"test\",\"version\":\"1\"}}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.result.protocolVersion").value("2025-11-25"))
                .andExpect(jsonPath("$.result.capabilities.tools.listChanged").value(false));
    }

    @Test
    void testInitialize_missingCredentialIsRejectedAsProtocolResponse() throws Exception {
        // 调用并断言：认证错误不能包成网页 CommonResult。
        authenticatedMvc()
                .perform(
                        post("/mcp")
                                .contentType("application/json")
                                .accept("application/json", "text/event-stream")
                                .content(
                                        "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").exists())
                .andExpect(jsonPath("$.code").doesNotExist());
    }
}
