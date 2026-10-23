package com.df.knowledge.service.mcp;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;

/** MCP Streamable HTTP 协议边界。 */
public interface McpProtocolService {
    /**
     * @param body 原始 JSON
     * @param request HTTP 请求
     * @return JSON-RPC 或空通知响应
     */
    ResponseEntity<?> post(String body, HttpServletRequest request);

    /**
     * @param request 不支持方法的 HTTP 请求
     * @return 来源无效时 403，否则为 405
     */
    ResponseEntity<?> unsupported(HttpServletRequest request);
}
