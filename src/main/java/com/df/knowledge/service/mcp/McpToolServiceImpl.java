package com.df.knowledge.service.mcp;

import com.df.knowledge.controller.doc.vo.DocSearchReqVO;
import com.df.knowledge.controller.knowledge.vo.knowledgebase.KnowledgeBasePageReqVO;
import com.df.knowledge.controller.rag.vo.RagAskReqVO;
import com.df.knowledge.dal.dataobject.mcp.McpCredentialDO;
import com.df.knowledge.service.doc.DocSearchService;
import com.df.knowledge.service.knowledge.KnowledgeBaseService;
import com.df.knowledge.service.rag.RagService;
import com.fasterxml.jackson.databind.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/** 三个只读工具复用网页检索、内容权限与最终引用校验。 */
@Service
@RequiredArgsConstructor
public class McpToolServiceImpl implements McpToolService {
    private final KnowledgeBaseService knowledgeBases;
    private final DocSearchService search;
    private final RagService rag;
    private final ObjectMapper json;

    @Override
    public List<Map<String, Object>> definitions(McpCredentialDO credential) {
        Set<String> allowed = Set.copyOf(Arrays.asList(credential.getTools().split(",")));
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : List.of("ask", "list_knowledge_bases", "search")) {
            if (!allowed.contains(name)) continue;
            Map<String, Object> properties = new LinkedHashMap<>();
            if (!"list_knowledge_bases".equals(name)) {
                properties.put("knowledgeBaseIds", identifiers());
                properties.put(
                        "formats",
                        Map.of(
                                "type",
                                "array",
                                "maxItems",
                                6,
                                "items",
                                Map.of("type", "integer", "minimum", 1, "maximum", 6)));
                properties.put(
                        "uploadedFrom",
                        Map.of(
                                "type",
                                "string",
                                "description",
                                "上传起点，ISO 本地时间，例如 2026-10-24T00:00:00"));
                properties.put(
                        "uploadedTo", Map.of("type", "string", "description", "上传终点，ISO 本地时间"));
            }
            List<String> required = List.of();
            String description;
            if ("ask".equals(name)) {
                properties.put(
                        "question", Map.of("type", "string", "minLength", 1, "maxLength", 2000));
                properties.put("modelId", Map.of("type", "integer", "minimum", 1));
                properties.put("selectedChunkIds", identifiers());
                required = List.of("question");
                description = "按当前权限进行 RAG 检索并返回最终引用；选择分块后只使用所选证据，不创建或修改聊天会话";
            } else {
                properties.put(
                        "pageNo",
                        Map.of("type", "integer", "minimum", 1, "maximum", 500, "default", 1));
                properties.put(
                        "pageSize",
                        Map.of("type", "integer", "minimum", 1, "maximum", 100, "default", 20));
                if ("search".equals(name)) {
                    properties.put(
                            "query", Map.of("type", "string", "minLength", 1, "maxLength", 2000));
                    properties.put(
                            "mode",
                            Map.of(
                                    "type",
                                    "string",
                                    "enum",
                                    List.of("HYBRID", "FUZZY"),
                                    "default",
                                    "HYBRID"));
                    required = List.of("query");
                    description = "混合或模糊检索可用文档，支持范围、格式、上传时间及分页，返回定位、高亮与相关性";
                } else description = "分页列出当前用户有权访问的知识库，不包含已删除知识库";
            }
            result.add(
                    Map.of(
                            "name",
                            name,
                            "description",
                            description,
                            "inputSchema",
                            Map.of(
                                    "type",
                                    "object",
                                    "properties",
                                    properties,
                                    "required",
                                    required,
                                    "additionalProperties",
                                    false),
                            "annotations",
                            Map.of("readOnlyHint", true, "openWorldHint", "ask".equals(name))));
        }
        return result;
    }

    @Override
    public ToolOutcome call(Long userId, String name, JsonNode arguments) {
        JsonNode args = arguments.isMissingNode() ? json.createObjectNode() : arguments;
        validate(name, args);
        try {
            if ("search".equals(name)) {
                DocSearchReqVO request = json.treeToValue(args, DocSearchReqVO.class);
                var page = search.search(userId, request);
                return new ToolOutcome(
                        page,
                        page.getList().size(),
                        page.getList().stream()
                                .map(chunk -> chunk.getKnowledgeBaseId())
                                .distinct()
                                .toList());
            }
            if ("ask".equals(name)) {
                RagAskReqVO request = json.treeToValue(args, RagAskReqVO.class);
                var answer = rag.askReadOnly(userId, request);
                return new ToolOutcome(
                        answer,
                        answer.getCitations().size(),
                        answer.getCitations().stream()
                                .map(citation -> citation.getKnowledgeBaseId())
                                .distinct()
                                .toList());
            }
            var request = new KnowledgeBasePageReqVO();
            request.setPageNum(integer(args, "pageNo", 1, 500));
            request.setPageSize(integer(args, "pageSize", 20, 100));
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
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalArgumentException("工具参数格式无效");
        }
    }

    private void validate(String name, JsonNode args) {
        if (!args.isObject()) throw new IllegalArgumentException("工具参数必须是对象");
        Set<String> allowed = new HashSet<>();
        if ("list_knowledge_bases".equals(name)) allowed.addAll(Set.of("pageNo", "pageSize"));
        else {
            allowed.addAll(Set.of("knowledgeBaseIds", "formats", "uploadedFrom", "uploadedTo"));
            if ("search".equals(name))
                allowed.addAll(Set.of("query", "mode", "pageNo", "pageSize"));
            else if ("ask".equals(name))
                allowed.addAll(Set.of("question", "modelId", "selectedChunkIds"));
            else throw new IllegalArgumentException("工具不存在");
        }
        args.fieldNames()
                .forEachRemaining(
                        key -> {
                            if (!allowed.contains(key))
                                throw new IllegalArgumentException("存在未支持字段");
                        });
        if (!"ask".equals(name)) {
            integer(args, "pageNo", 1, 500);
            integer(args, "pageSize", 20, 100);
        }
        if (!"list_knowledge_bases".equals(name)) {
            String field = "search".equals(name) ? "query" : "question";
            if (!args.path(field).isTextual()
                    || args.path(field).asText().isBlank()
                    || args.path(field).asText().length() > 2000)
                throw new IllegalArgumentException("问题或检索文本无效");
            for (String identifiers : List.of("knowledgeBaseIds", "selectedChunkIds"))
                if (args.has(identifiers)) {
                    JsonNode values = args.get(identifiers);
                    if (!values.isArray() || values.size() > 100)
                        throw new IllegalArgumentException("范围过大");
                    for (JsonNode value : values)
                        if (!value.isIntegralNumber()
                                || !value.canConvertToLong()
                                || value.longValue() < 1)
                            throw new IllegalArgumentException("编号必须为正整数");
                }
            if (args.has("modelId")
                    && (!args.get("modelId").isIntegralNumber()
                            || !args.get("modelId").canConvertToLong()
                            || args.get("modelId").longValue() < 1))
                throw new IllegalArgumentException("模型编号无效");
            if (args.has("formats")) {
                JsonNode formats = args.get("formats");
                if (!formats.isArray() || formats.size() > 6)
                    throw new IllegalArgumentException("格式过滤无效");
                for (JsonNode format : formats)
                    if (!format.isIntegralNumber()
                            || !format.canConvertToInt()
                            || format.intValue() < 1
                            || format.intValue() > 6) throw new IllegalArgumentException("格式过滤无效");
            }
            LocalDateTime from = date(args, "uploadedFrom"), to = date(args, "uploadedTo");
            if (from != null && to != null && from.isAfter(to))
                throw new IllegalArgumentException("上传时间范围无效");
            if (args.has("mode")
                    && (!args.get("mode").isTextual()
                            || !Set.of("HYBRID", "FUZZY").contains(args.get("mode").asText())))
                throw new IllegalArgumentException("检索模式仅支持混合和模糊");
        }
    }

    private LocalDateTime date(JsonNode args, String field) {
        if (!args.has(field)) return null;
        if (!args.get(field).isTextual()) throw new IllegalArgumentException("上传时间格式无效");
        try {
            return LocalDateTime.parse(args.get(field).asText());
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("上传时间格式无效");
        }
    }

    private int integer(JsonNode args, String name, int fallback, int maximum) {
        if (!args.has(name)) return fallback;
        JsonNode value = args.get(name);
        if (!value.isIntegralNumber()
                || !value.canConvertToInt()
                || value.intValue() < 1
                || value.intValue() > maximum) throw new IllegalArgumentException("分页参数超出范围");
        return value.intValue();
    }

    private Map<String, Object> identifiers() {
        return Map.of(
                "type", "array", "maxItems", 100, "items", Map.of("type", "integer", "minimum", 1));
    }
}
