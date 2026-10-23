package com.df.knowledge.controller.mcp;

import com.df.knowledge.service.mcp.McpProtocolService;
import com.df.knowledge.service.mcp.McpProtocolServiceImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** MCP 独立协议入口，响应遵循 JSON-RPC。 */
@RestController
@Tag(name = "mcpProtocolController")
@RequiredArgsConstructor
public class McpProtocolController {
    private final McpProtocolService service;

    /**
     * @param body 原始 JSON
     * @param request 传输请求
     * @return MCP 响应
     */
    @PostMapping("/mcp")
    @Operation(summary = "MCP 2025-11-25 Streamable HTTP JSON-RPC 接入")
    public ResponseEntity<?> post(
            @RequestBody(required = false) String body, HttpServletRequest request) {
        try {
            return service.post(body, request);
        } catch (RuntimeException ex) {
            // 包括审计存储故障，协议入口始终保持 JSON-RPC 且不暴露内部错误。
            return McpProtocolServiceImpl.error(500, null, -32603, "MCP 服务暂时不可用，请重试");
        }
    }

    /**
     * @return 本实现不提供服务器主动事件流或协议会话
     */
    @Operation(summary = "检查来源并拒绝不支持的 MCP HTTP 方法")
    @RequestMapping(
            value = "/mcp",
            method = {
                RequestMethod.GET,
                RequestMethod.DELETE,
                RequestMethod.OPTIONS,
                RequestMethod.HEAD,
                RequestMethod.PUT,
                RequestMethod.PATCH
            })
    public ResponseEntity<?> unsupported(HttpServletRequest request) {
        return service.unsupported(request);
    }
}
