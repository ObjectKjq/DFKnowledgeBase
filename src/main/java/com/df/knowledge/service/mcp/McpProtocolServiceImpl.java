package com.df.knowledge.service.mcp;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.mcp.*;
import com.df.knowledge.dal.mysql.mcp.McpCallLogMapper;
import com.fasterxml.jackson.databind.*;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.*;
import java.util.*;

/** 仅支持明确声明的 2025-11-25 协议，所有调用重新认证。 */
@Service
@Slf4j
@Validated
public class McpProtocolServiceImpl implements McpProtocolService {
    private static final String VERSION = "2025-11-25";
    private final McpCredentialService credentials;
    private final McpCallLogMapper logs;
    private final McpToolService tools;
    private final ObjectMapper json;
    private final Clock clock;
    private final Set<String> allowedOrigins;

    public McpProtocolServiceImpl(
            McpCredentialService credentials,
            McpCallLogMapper logs,
            McpToolService tools,
            ObjectMapper json,
            Clock mcpClock,
            @Value("${knowledge.mcp.allowed-origins:}") String origins) {
        this.credentials = credentials;
        this.logs = logs;
        this.tools = tools;
        this.json = json;
        this.clock = mcpClock;
        this.allowedOrigins = new HashSet<>();
        for (String origin : origins.split(","))
            if (!origin.isBlank()) allowedOrigins.add(origin.trim());
    }

    @Override
    public ResponseEntity<?> unsupported(HttpServletRequest request) {
        if (!validOrigin(request)) return error(403, null, 403, "MCP 来源不受信任");
        return ResponseEntity.status(405).header("Allow", "POST").build();
    }

    @Override
    public ResponseEntity<?> post(String body, HttpServletRequest request) {
        McpCredentialDO credential = credentials.find(request.getHeader("Authorization"));
        McpCallLogDO audit =
                new McpCallLogDO()
                        .setCredentialId(credential == null ? null : credential.getId())
                        .setUserId(credential == null ? null : credential.getUserId())
                        .setCallTime(LocalDateTime.now(clock))
                        .setToolName("unknown")
                        .setKnowledgeBaseIds("[]")
                        .setResultCount(0)
                        .setOutcome("REJECTED")
                        .setReasonCode("INVALID_REQUEST");
        Object id = null;
        try {
            if (!validOrigin(request))
                return rejected(audit, 403, id, 403, "MCP 来源不受信任", "ORIGIN_DENIED");
            int authentication = credentials.authenticationStatus(credential);
            if (authentication != 200)
                return rejected(
                        audit,
                        authentication,
                        id,
                        authentication,
                        authentication == 503 ? "MCP 接入已关闭" : "MCP 凭证无效",
                        authentication == 503 ? "DISABLED" : "INVALID_CREDENTIAL");
            if (body == null || body.length() > 65536)
                return rejected(audit, 400, id, -32600, "请求内容无效", "INVALID_REQUEST");
            JsonNode input;
            try {
                input =
                        json.reader()
                                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                                .readTree(body);
            } catch (Exception ex) {
                return rejected(audit, 400, id, -32700, "JSON 解析失败", "PARSE_ERROR");
            }
            if (input == null
                    || !input.isObject()
                    || !"2.0".equals(input.path("jsonrpc").asText())
                    || !input.path("method").isTextual()
                    || (input.has("params") && !input.get("params").isObject())
                    || input.has("result")
                    || input.has("error"))
                return rejected(audit, 400, id, -32600, "无效 JSON-RPC 请求", "INVALID_REQUEST");
            if (input.has("id")) {
                JsonNode suppliedId = input.get("id");
                if (!suppliedId.isTextual()
                        && !(suppliedId.isIntegralNumber() && suppliedId.canConvertToLong()))
                    return rejected(audit, 400, id, -32600, "请求编号必须是字符串或整数", "INVALID_REQUEST");
                id = suppliedId.isTextual() ? suppliedId.asText() : suppliedId.longValue();
            }
            String method = input.path("method").asText();
            JsonNode params = input.path("params");
            String contentType = request.getContentType();
            if (contentType == null
                    || !MediaType.APPLICATION_JSON.isCompatibleWith(
                            MediaType.parseMediaType(contentType)))
                return rejected(
                        audit,
                        415,
                        id,
                        -32600,
                        "Content-Type 必须是 application/json",
                        "CONTENT_TYPE");
            if (!acceptsBoth(request.getHeader("Accept")))
                return rejected(
                        audit,
                        406,
                        id,
                        -32600,
                        "Accept 必须支持 application/json 和 text/event-stream",
                        "ACCEPT");
            String version = request.getHeader("MCP-Protocol-Version");
            if ((!"initialize".equals(method) || version != null) && !VERSION.equals(version))
                return rejected(
                        audit, 400, id, -32600, "仅支持 MCP 2025-11-25，请重新初始化", "PROTOCOL_VERSION");
            if (id == null) {
                if (!"notifications/initialized".equals(method)
                        && !"notifications/cancelled".equals(method))
                    return rejected(audit, 400, null, -32600, "不支持的通知", "INVALID_NOTIFICATION");
                audit.setOutcome("SUCCESS").setReasonCode("OK");
                return ResponseEntity.accepted().build();
            }
            Object result;
            switch (method) {
                case "initialize" -> {
                    if (!params.path("protocolVersion").isTextual()
                            || !params.path("capabilities").isObject()
                            || !params.path("clientInfo").path("name").isTextual()
                            || !params.path("clientInfo").path("version").isTextual())
                        return rejected(audit, 200, id, -32602, "初始化参数无效", "INVALID_PARAMS");
                    result =
                            Map.of(
                                    "protocolVersion",
                                    VERSION,
                                    "capabilities",
                                    Map.of("tools", Map.of("listChanged", false)),
                                    "serverInfo",
                                    Map.of("name", "df-knowledge-base", "version", "0.1.0"));
                }
                case "ping" -> result = Map.of();
                case "tools/list" -> result = Map.of("tools", tools.definitions(credential));
                case "tools/call" -> {
                    if (!params.path("name").isTextual()
                            || (params.has("arguments") && !params.path("arguments").isObject()))
                        return rejected(audit, 200, id, -32602, "工具调用参数无效", "INVALID_PARAMS");
                    String name = params.path("name").asText();
                    if (!McpToolService.TOOL_NAMES.contains(name))
                        return rejected(audit, 200, id, -32602, "工具不存在", "UNKNOWN_TOOL");
                    audit.setToolName(name);
                    if (!Arrays.asList(credential.getTools().split(",")).contains(name))
                        return rejected(audit, 403, id, 403, "凭证未开放此工具", "TOOL_DENIED");
                    if (!credentials.claimCall(credential))
                        return rejected(audit, 429, id, 429, "工具调用过于频繁，请稍后重试", "RATE_LIMIT");
                    var outcome =
                            tools.call(credential.getUserId(), name, params.path("arguments"));
                    audit.setKnowledgeBaseIds(json.writeValueAsString(outcome.knowledgeBaseIds()))
                            .setResultCount(outcome.count());
                    result =
                            Map.of(
                                    "content",
                                    List.of(
                                            Map.of(
                                                    "type",
                                                    "text",
                                                    "text",
                                                    json.writeValueAsString(outcome.value()))),
                                    "structuredContent",
                                    outcome.value(),
                                    "isError",
                                    false);
                }
                default -> {
                    return rejected(audit, 200, id, -32601, "方法不存在", "METHOD_NOT_FOUND");
                }
            }
            audit.setOutcome("SUCCESS").setReasonCode("OK");
            return ResponseEntity.ok(Map.of("jsonrpc", "2.0", "id", id, "result", result));
        } catch (IllegalArgumentException ex) {
            audit.setOutcome("FAILED").setReasonCode("INVALID_PARAMS");
            return toolFailure(id, "工具参数无效，请检查参数范围及格式");
        } catch (ServiceException ex) {
            audit.setOutcome("FAILED").setReasonCode("BUSINESS_FAILURE");
            return toolFailure(id, "操作未完成，请检查内容权限、文档状态及模型配置");
        } catch (Exception ex) {
            audit.setOutcome("FAILED").setReasonCode("INTERNAL_ERROR");
            return error(500, id, -32603, "MCP 服务暂时不可用，请重试");
        } finally {
            // 协议层不持有业务事务，失败与拒绝记录不会随业务回滚而丢失。
            logs.insert(audit);
        }
    }

    private ResponseEntity<?> rejected(
            McpCallLogDO audit, int status, Object id, int code, String message, String reason) {
        audit.setReasonCode(reason);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (status == 429)
            response.header(
                    "Retry-After",
                    Long.toString(60 - Math.floorMod(clock.instant().getEpochSecond(), 60)));
        if (status == 401) response.header("WWW-Authenticate", "Bearer");
        return response.body(errorBody(id, code, message));
    }

    private ResponseEntity<?> toolFailure(Object id, String message) {
        if (id == null) return error(400, null, -32602, message);
        return ResponseEntity.ok(
                Map.of(
                        "jsonrpc",
                        "2.0",
                        "id",
                        id,
                        "result",
                        Map.of(
                                "isError",
                                true,
                                "content",
                                List.of(Map.of("type", "text", "text", message)))));
    }

    private boolean acceptsBoth(String header) {
        if (header == null) return false;
        var types = MediaType.parseMediaTypes(header);
        return types.stream()
                        .anyMatch(
                                t ->
                                        t.getQualityValue() > 0
                                                && t.isCompatibleWith(MediaType.APPLICATION_JSON))
                && types.stream()
                        .anyMatch(
                                t ->
                                        t.getQualityValue() > 0
                                                && t.isCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    private boolean validOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (Collections.list(request.getHeaders("Origin")).size() > 1) return false;
        if (origin == null) return true;
        try {
            URI uri = URI.create(origin);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())) return false;
            int port = request.getLocalPort();
            return allowedOrigins.contains(origin)
                    || origin.equals("http://127.0.0.1:" + port)
                    || origin.equals("http://localhost:" + port);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * @param status HTTP 状态
     * @param id 请求编号
     * @param code 协议错误码
     * @param message 脱敏提示
     * @return 错误响应
     */
    public static ResponseEntity<?> error(int status, Object id, int code, String message) {
        return ResponseEntity.status(status).body(errorBody(id, code, message));
    }

    private static Map<String, Object> errorBody(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        if (id != null) response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }
}
