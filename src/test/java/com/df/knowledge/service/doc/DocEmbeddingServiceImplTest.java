package com.df.knowledge.service.doc;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.enums.ErrorCodeConstants;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * {@link DocEmbeddingServiceImpl} 的服务层测试，仅使用 ONNX 模型端口桩
 */
@ExtendWith(MockitoExtension.class)
class DocEmbeddingServiceImplTest {

    @Mock
    private EmbeddingModel docEmbeddingModel;
    @InjectMocks
    private DocEmbeddingServiceImpl docEmbeddingService;

    @Test
    void testEmbedAll_preservesBatchOrder() {
        // 准备参数：模型返回两个不同的向量，不执行真实推理
        List<String> texts = List.of("第一块", "第二块");
        float[] first = new float[512];
        float[] second = new float[512];
        first[0] = 1;
        second[1] = 1;
        when(docEmbeddingModel.embedAll(List.of(TextSegment.from("第一块"), TextSegment.from("第二块"))))
                .thenReturn(Response.from(List.of(Embedding.from(first), Embedding.from(second))));

        // 调用
        List<float[]> vectors = docEmbeddingService.embedAll(texts);

        // 断言：输入文本和输出顺序均保留，没有对文档添加检索专用的查询前缀
        assertEquals(2, vectors.size());
        assertArrayEquals(first, vectors.get(0));
        assertArrayEquals(second, vectors.get(1));
    }

    @Test
    void testEmbedAll_nativeLoadingFailureIsReadableBusinessError() {
        // 准备参数：本机原生库无法加载，不能让任务无声停留在索引中
        when(docEmbeddingModel.embedAll(List.of(TextSegment.from("知识检索"))))
                .thenThrow(new UnsatisfiedLinkError("native library unavailable"));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> docEmbeddingService.embedAll(List.of("知识检索")));
        assertEquals(ErrorCodeConstants.DOC_EMBEDDING_FAILED.getCode(), exception.getErrorCode().getCode());
        assertTrue(exception.getMessage().startsWith("文档向量化失败："));
    }
}
