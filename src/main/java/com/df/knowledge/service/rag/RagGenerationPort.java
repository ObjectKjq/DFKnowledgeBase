package com.df.knowledge.service.rag;

import java.util.List;
import java.util.function.Consumer;

/**
 * 基于证据生成答案的端口
 */
public interface RagGenerationPort {

    /**
     * 生成答案与本次证据的引用编号
     *
     * @param request 生成请求
     * @return 生成结果
     */
    GenerationResult generate(GenerationRequest request);

    /**
     * 增量生成答案，完成后返回用于校验的完整结果
     *
     * @param request 生成请求
     * @param onDelta 答案文本增量接收者
     * @return 完整生成结果
     */
    GenerationResult stream(GenerationRequest request, Consumer<String> onDelta);

    /**
     * 生成请求
     */
    record GenerationRequest(String question, List<ChatMessage> history, List<Evidence> evidence) {
    }

    /**
     * 窗口内的历史消息
     */
    record ChatMessage(String role, String content) {
    }

    /**
     * 编号从 1 开始的本次检索证据
     */
    record Evidence(int number, Long chunkId, String docTitle, Integer pageNumber, Integer chunkIndex, String text) {
    }

    /**
     * 生成结果；citationNumbers 引用 {@link Evidence#number()}
     */
    record GenerationResult(boolean sufficientEvidence, String answer, List<Integer> citationNumbers) {
    }
}
