package com.df.knowledge.service.rag;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.df.knowledge.common.exception.ServiceException;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.df.knowledge.enums.ErrorCodeConstants.RAG_MODEL_UNAVAILABLE;

/**
 * 通过 langchain4j AiServices 调用 OpenAI 兼容模型
 */
@Service
@Slf4j
public class OpenAiCompatibleRagGenerationPort implements RagGenerationPort {

    private static final String SYSTEM_PROMPT = """
            你是本地知识库检索助手。只能依据本次提供的证据回答，不得使用外部知识或猜测。
            每个结论后使用 [证据编号] 标注依据。依据不足时不要作答。
            仅输出 JSON：{"sufficient":true|false,"answer":"答案文本","citations":[1,2]}。
            sufficient=false 时 answer 置为空字符串、citations 置为空数组。
            """;

    private final RagAssistant assistant;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleRagGenerationPort(
            ObjectMapper objectMapper,
            @Value("${knowledge.rag.base-url:}") String baseUrl,
            @Value("${knowledge.rag.api-key:}") String apiKey,
            @Value("${knowledge.rag.model:}") String model) {
        this.objectMapper = objectMapper;
        if (StrUtil.isBlank(baseUrl) || StrUtil.isBlank(model)) {
            this.assistant = null;
            return;
        }
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(StrUtil.removeSuffix(baseUrl.trim(), "/"))
                .apiKey(StrUtil.blankToDefault(apiKey, "not-needed"))
                .modelName(model)
                .temperature(0.0)
                .responseFormat("json_object")
                .timeout(Duration.ofSeconds(60))
                .build();
        this.assistant = AiServices.create(RagAssistant.class, chatModel);
    }

    @Override
    public GenerationResult generate(GenerationRequest request) {
        if (assistant == null) {
            throw new ServiceException(RAG_MODEL_UNAVAILABLE);
        }
        try {
            return parseContent(assistant.generate(buildPrompt(request)));
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("[generate][对话模型调用或响应解析失败]", ex);
            throw new ServiceException(RAG_MODEL_UNAVAILABLE);
        }
    }

    private static String buildPrompt(GenerationRequest request) {
        StringBuilder prompt = new StringBuilder();
        if (!request.history().isEmpty()) {
            prompt.append("会话上下文：\n");
            request.history().forEach(message -> prompt.append(message.role()).append(": ")
                    .append(message.content()).append('\n'));
            prompt.append('\n');
        }
        prompt.append("本轮问题：").append(request.question()).append("\n\n本轮证据：\n");
        for (Evidence evidence : request.evidence()) {
            prompt.append('[').append(evidence.number()).append("] ").append(evidence.docTitle());
            if (evidence.pageNumber() != null) {
                prompt.append(" 第 ").append(evidence.pageNumber()).append(" 页");
            } else if (evidence.chunkIndex() != null) {
                prompt.append(" 第 ").append(evidence.chunkIndex() + 1).append(" 块");
            }
            prompt.append('\n').append(evidence.text()).append("\n\n");
        }
        return prompt.toString();
    }

    private GenerationResult parseContent(String content) throws JsonProcessingException {
        if (StrUtil.isBlank(content)) {
            throw new ServiceException(RAG_MODEL_UNAVAILABLE);
        }
        JsonNode root = objectMapper.readTree(content.trim());
        if (root == null || !root.isObject()) {
            throw new ServiceException(RAG_MODEL_UNAVAILABLE);
        }
        List<Integer> citations = new ArrayList<>();
        root.path("citations").forEach(node -> {
            if (node.canConvertToInt()) {
                citations.add(node.asInt());
            }
        });
        return new GenerationResult(root.path("sufficient").asBoolean(false),
                root.path("answer").asText(""), citations);
    }

    private interface RagAssistant {

        @SystemMessage(SYSTEM_PROMPT)
        String generate(@UserMessage String prompt);
    }
}
