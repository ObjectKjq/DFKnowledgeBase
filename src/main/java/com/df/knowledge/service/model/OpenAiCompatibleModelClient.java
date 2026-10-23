package com.df.knowledge.service.model;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.model.ModelConfigurationDO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.df.knowledge.enums.ErrorCodeConstants.*;

/** OpenAI 兼容聊天与常见 /rerank 协议边界；不返回提供方正文错误。 */
@Component
@RequiredArgsConstructor
public class OpenAiCompatibleModelClient {

    private final ObjectMapper objectMapper;

    /**
     * 调用非流式聊天端点。
     * @param model 接入配置
     * @param prompt 提示词
     * @return 生成文本
     */
    public String chat(ModelConfigurationDO model, String prompt) {
        JsonNode root = request(model, "/chat/completions", Map.of("model", model.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt)), "stream", false,
                "max_tokens", 512));
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new ServiceException(MODEL_RESPONSE_INVALID);
        }
        return content.asText();
    }

    /**
     * 调用重排端点并校验完整排序结果。
     * @param model 接入配置
     * @param query 问题
     * @param documents 待排序文本
     * @return 按相关性排序的原始索引
     */
    public List<Integer> rerank(ModelConfigurationDO model, String query, List<String> documents) {
        JsonNode results = request(model, "/rerank", Map.of("model", model.getModel(), "query", query,
                "documents", documents, "top_n", documents.size(), "return_documents", false)).path("results");
        if (!results.isArray() || results.size() != documents.size()) {
            throw new ServiceException(MODEL_RESPONSE_INVALID);
        }
        Map<Integer, Double> scores = new HashMap<>();
        for (JsonNode item : results) {
            int index = item.path("index").asInt(-1);
            double score = item.path("relevance_score").asDouble(Double.NaN);
            if (index < 0 || index >= documents.size() || !Double.isFinite(score) || scores.put(index, score) != null) {
                throw new ServiceException(MODEL_RESPONSE_INVALID);
            }
        }
        return scores.keySet().stream().sorted(Comparator.<Integer>comparingDouble(scores::get).reversed()
                .thenComparingInt(Integer::intValue)).toList();
    }

    private JsonNode request(ModelConfigurationDO model, String suffix, Object body) {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(model.getTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER).build()) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(model.getBaseUrl().replaceAll("/+$", "") + suffix))
                    .timeout(Duration.ofSeconds(model.getTimeoutSeconds())).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            if (model.getApiKey() != null && !model.getApiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + model.getApiKey());
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new ServiceException(MODEL_AUTH_FAILED);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ServiceException(MODEL_CONNECTION_FAILED);
            }
            return objectMapper.readTree(response.body());
        } catch (ServiceException ex) {
            throw ex;
        } catch (HttpTimeoutException ex) {
            throw new ServiceException(MODEL_TIMEOUT);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(MODEL_TIMEOUT);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(MODEL_RESPONSE_INVALID);
        } catch (Exception ex) {
            // 错误消息可能包含提供方正文、提示词或密钥，仅返回平台安全错误
            throw new ServiceException(MODEL_CONNECTION_FAILED);
        }
    }
}
