package com.df.knowledge.service.mcp;

import com.df.knowledge.dal.dataobject.mcp.McpCredentialDO;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

/** MCP 的只读工具描述与业务分发。 */
public interface McpToolService {
    /** 工具名固定白名单，不提供任何写工具。 */
    Set<String> TOOL_NAMES = Set.of("search", "ask", "list_knowledge_bases");

    /**
     * @param credential 当前凭证
     * @return 当前允许发现的工具
     */
    List<Map<String, Object>> definitions(McpCredentialDO credential);

    /**
     * @param userId 当前用户
     * @param name 工具名
     * @param arguments 参数
     * @return 最终业务结果
     */
    ToolOutcome call(Long userId, String name, JsonNode arguments);

    /** 结果值、条数及已经通过授权的知识库编号。 */
    record ToolOutcome(Object value, int count, List<Long> knowledgeBaseIds) {}
}
