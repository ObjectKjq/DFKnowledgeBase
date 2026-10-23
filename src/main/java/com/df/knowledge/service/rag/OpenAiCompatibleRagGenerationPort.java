package com.df.knowledge.service.rag;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.df.knowledge.common.exception.ServiceException;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.model.chat.response.StreamingHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.df.knowledge.service.model.ModelConfigurationService;
import com.df.knowledge.service.model.ModelConfigurationServiceImpl;
import com.df.knowledge.dal.dataobject.model.ModelConfigurationDO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.df.knowledge.enums.ErrorCodeConstants.RAG_MODEL_UNAVAILABLE;
import static com.df.knowledge.enums.ErrorCodeConstants.RAG_GENERATION_TIMEOUT;

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

    private ModelConfigurationService configurationService;

    @Autowired
    public OpenAiCompatibleRagGenerationPort(ObjectMapper objectMapper,
            ModelConfigurationService configurationService) {
        this(objectMapper, "", "", "");
        this.configurationService = configurationService;
    }

    private final RagAssistant assistant;
    private final StreamingRagAssistant streamingAssistant;
    private final ObjectMapper objectMapper;
    private final int timeoutSeconds;

    public OpenAiCompatibleRagGenerationPort(
            ObjectMapper objectMapper,
            String baseUrl,
            String apiKey,
            String model) {
        this(objectMapper, baseUrl, apiKey, model, 60);
    }

    private OpenAiCompatibleRagGenerationPort(ObjectMapper objectMapper, String baseUrl, String apiKey, String model,
            int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.timeoutSeconds = timeoutSeconds;
        if (StrUtil.isBlank(baseUrl) || StrUtil.isBlank(model)) {
            this.assistant = null;
            this.streamingAssistant = null;
            return;
        }
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(StrUtil.removeSuffix(baseUrl.trim(), "/"))
                .apiKey(StrUtil.blankToDefault(apiKey, "not-needed"))
                .modelName(model)
                .temperature(0.0)
                .responseFormat("json_object")
                .maxRetries(0)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.assistant = AiServices.create(RagAssistant.class, chatModel);
        this.streamingAssistant = AiServices.create(StreamingRagAssistant.class,
                OpenAiStreamingChatModel.builder()
                        .baseUrl(StrUtil.removeSuffix(baseUrl.trim(), "/"))
                        .apiKey(StrUtil.blankToDefault(apiKey, "not-needed"))
                        .modelName(model).temperature(0.0).timeout(Duration.ofSeconds(timeoutSeconds)).build());
    }

    @Override
    public GenerationResult stream(GenerationRequest request, Consumer<String> onDelta) {
        if (configurationService != null) {
            var model = configurationService.resolveChatModel(request.modelId());
            if (ModelConfigurationServiceImpl.isSimulated(model)) {
                GenerationResult result = simulate(request);
                onDelta.accept(result.answer());
                return result;
            }
            return configuredPort(model).stream(request, onDelta);
        }
        if (streamingAssistant == null) {
            throw new ServiceException(RAG_MODEL_UNAVAILABLE);
        }
        CompletableFuture<String> completed = new CompletableFuture<>();
        AtomicReference<StreamingHandle> handle = new AtomicReference<>();
        try {
            streamingAssistant.generate(buildPrompt(request))
                    .onPartialResponseWithContext((partial, context) -> {
                        handle.set(context.streamingHandle());
                        if (completed.isDone()) {
                            context.streamingHandle().cancel();
                        } else {
                            try {
                                onDelta.accept(partial.text());
                            } catch (RuntimeException ex) {
                                completed.completeExceptionally(ex);
                            }
                        }
                    })
                    .onCompleteResponse(response -> completed.complete(response.aiMessage().text()))
                    .onError(completed::completeExceptionally).start();
            String answer = completed.get(timeoutSeconds, TimeUnit.SECONDS);
            List<Integer> citations = new ArrayList<>();
            Matcher matcher = Pattern.compile("\\[(\\d+)]").matcher(answer);
            while (matcher.find()) {
                try {
                    citations.add(Integer.parseInt(matcher.group(1)));
                } catch (NumberFormatException ex) {
                    // 无法映射的编号应走依据不足拒答，不误报模型调用失败
                    return new GenerationResult(false, answer, List.of());
                }
            }
            return new GenerationResult(!citations.isEmpty(), answer, citations);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(RAG_GENERATION_TIMEOUT);
        } catch (TimeoutException ex) {
            throw new ServiceException(RAG_GENERATION_TIMEOUT);
        } catch (ExecutionException | RuntimeException ex) {
            log.warn("[stream][对话模型流式调用失败]");
            throw new ServiceException(RAG_MODEL_UNAVAILABLE);
        } finally {
            completed.cancel(true);
            if (handle.get() != null) {
                handle.get().cancel();
            }
        }
    }

    @Override
    public GenerationResult generate(GenerationRequest request) {
        if (configurationService != null) {
            var model = configurationService.resolveChatModel(request.modelId());
            if (ModelConfigurationServiceImpl.isSimulated(model)) {
                return simulate(request);
            }
            return configuredPort(model).generate(request);
        }
        if (assistant == null) {
            throw new ServiceException(RAG_MODEL_UNAVAILABLE);
        }
        try {
            return parseContent(assistant.generate(buildPrompt(request)));
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("[generate][对话模型调用或响应解析失败]");
            throw new ServiceException(RAG_MODEL_UNAVAILABLE);
        }
    }

    private GenerationResult simulate(GenerationRequest request) {
        if (request.evidence().isEmpty()) {
            return new GenerationResult(false, "【本地模拟，非真实模型生成】未找到足够依据", List.of());
        }
        Evidence evidence = request.evidence().getFirst();
        return new GenerationResult(true, "【本地模拟，非真实模型生成】依据所选证据：" + evidence.text()
                + "[" + evidence.number() + "]", List.of(evidence.number()));
    }

    private OpenAiCompatibleRagGenerationPort configuredPort(ModelConfigurationDO model) {
        return new OpenAiCompatibleRagGenerationPort(objectMapper, model.getBaseUrl(), model.getApiKey(), model.getModel(),
                model.getTimeoutSeconds());
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

    private interface StreamingRagAssistant {

        @SystemMessage("""
                你是本地知识库检索助手。只能依据本次提供的证据回答，不得使用外部知识或猜测。
                仅输出答案文本，每个结论后使用 [证据编号] 标注依据。
                依据不足时仅输出「未找到足够依据」，不要附加引用或编造答案。
                """)
        TokenStream generate(@UserMessage String prompt);
    }
}
