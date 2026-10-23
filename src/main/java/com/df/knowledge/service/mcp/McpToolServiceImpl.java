package com.df.knowledge.service.mcp;

import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBasePageReqVO;
import com.df.knowledge.dal.dataobject.mcp.McpCredentialDO;
import com.df.knowledge.service.doc.DocSearchService;
import com.df.knowledge.service.knowledge.KnowledgeBaseService;
import com.df.knowledge.service.rag.RagService;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

/** 只读工具复用网页业务服务，禁止扩大用户内容权限。 */
@Service
@RequiredArgsConstructor
public class McpToolServiceImpl {
    public static final Set<String> TOOL_NAMES = Set.of("search", "ask", "list_knowledge_bases");
    private final KnowledgeBaseService knowledgeBases;
    private final DocSearchService search;
    private final RagService rag;

    /**
     * @param credential 当前凭证 @return 当前开放工具的协议描述
     */
    public List<Map<String, Object>> definitions(McpCredentialDO credential) {
        if (!Arrays.asList(credential.getTools().split(",")).contains("list_knowledge_bases"))
            return List.of();
        return List.of(
                Map.of(
                        "name",
                        "list_knowledge_bases",
                        "description",
                        "分页列出当前用户有权访问的知识库",
                        "inputSchema",
                        Map.of(
                                "type",
                                "object",
                                "properties",
                                Map.of(
                                        "pageNum",
                                        Map.of("type", "integer", "minimum", 1),
                                        "pageSize",
                                        Map.of("type", "integer", "minimum", 1, "maximum", 100)),
                                "additionalProperties",
                                false),
                        "annotations",
                        Map.of("readOnlyHint", true, "openWorldHint", false)));
    }

    /**
     * @param userId 当前用户 @param name 白名单工具 @param arguments 输入参数 @return 只读工具结果
     */
    public ToolOutcome call(Long userId, String name, JsonNode arguments) {
        if (!"list_knowledge_bases".equals(name)) throw new IllegalArgumentException("工具参数无效");
        var request = new KnowledgeBasePageReqVO();
        request.setPageNum(positiveInt(arguments, "pageNum", 1, 1000000));
        request.setPageSize(positiveInt(arguments, "pageSize", 20, 100));
        if (arguments.isObject())
            arguments
                    .fieldNames()
                    .forEachRemaining(
                            key -> {
                                if (!Set.of("pageNum", "pageSize").contains(key))
                                    throw new IllegalArgumentException("工具参数无效");
                            });
        var page = knowledgeBases.getKnowledgeBasePage(userId, request);
        List<Map<String, Object>> list =
                page.getList().stream()
                        .map(
                                kb -> {
                                    Map<String, Object> value = new LinkedHashMap<>();
                                    value.put("id", kb.getId());
                                    value.put("name", kb.getName());
                                    value.put("type", kb.getType());
                                    return value;
                                })
                        .toList();
        return new ToolOutcome(
                Map.of("list", list, "total", page.getTotal()),
                list.size(),
                page.getList().stream().map(kb -> kb.getId()).toList());
    }

    private int positiveInt(JsonNode args, String name, int fallback, int maximum) {
        if (!args.has(name)) return fallback;
        JsonNode value = args.get(name);
        if (!value.isIntegralNumber()
                || !value.canConvertToInt()
                || value.intValue() < 1
                || value.intValue() > maximum) throw new IllegalArgumentException("参数超出范围");
        return value.intValue();
    }

    /** 结果值、条数及已经通过授权的知识库编号。 */
    public record ToolOutcome(Object value, int count, List<Long> knowledgeBaseIds) {}
}
