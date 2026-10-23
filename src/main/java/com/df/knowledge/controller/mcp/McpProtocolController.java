package com.df.knowledge.controller.mcp;

import com.df.knowledge.service.mcp.McpProtocolService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** MCP 独立协议入口，响应遵循 JSON-RPC。 */
@RestController
@RequiredArgsConstructor
public class McpProtocolController {
    private final McpProtocolService service;

    /**
     * @param body 原始 JSON @param request 传输请求 @return MCP 响应
     */
    @PostMapping("/mcp")
    public ResponseEntity<?> post(
            @RequestBody(required = false) String body, HttpServletRequest request) {
        try {
            return service.post(body, request);
        } catch (RuntimeException ex) {
            // 包括审计存储故障，协议入口始终保持 JSON-RPC 且不暴露内部错误。
            return com.df.knowledge.service.mcp.McpProtocolServiceImpl.error(
                    500, null, -32603, "MCP 服务暂时不可用，请重试");
        }
    }

    /**
     * @return 本实现不提供服务器主动事件流或协议会话
     */
    @RequestMapping(
            value = "/mcp",
            method = {RequestMethod.GET, RequestMethod.DELETE})
    public ResponseEntity<?> unsupported() {
        return ResponseEntity.status(405).header("Allow", "POST").build();
    }
}
