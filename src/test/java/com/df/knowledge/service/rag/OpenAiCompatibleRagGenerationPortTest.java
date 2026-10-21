package com.df.knowledge.service.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * langchain4j 模型适配器的单元测试
 */
class OpenAiCompatibleRagGenerationPortTest {

    @Test
    void testCreate_configuredModelBuildsAiService() {
        // 准备参数
        ObjectMapper objectMapper = new ObjectMapper();

        // 调用并断言：配置完整时可创建 langchain4j AiServices
        assertDoesNotThrow(() -> new OpenAiCompatibleRagGenerationPort(
                objectMapper, "http://localhost:1234/v1", "test-key", "test-model"));
    }

    @Test
    void testGenerate_modelNotConfiguredReturnsExplicitError() {
        // 准备参数：未配置模型地址和模型名
        OpenAiCompatibleRagGenerationPort port = new OpenAiCompatibleRagGenerationPort(
                new ObjectMapper(), "", "", "");

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class, () -> port.generate(
                new RagGenerationPort.GenerationRequest("问题", java.util.List.of(), java.util.List.of())));
        assertEquals(ErrorCodeConstants.RAG_MODEL_UNAVAILABLE.getCode(), exception.getErrorCode().getCode());
    }
}
