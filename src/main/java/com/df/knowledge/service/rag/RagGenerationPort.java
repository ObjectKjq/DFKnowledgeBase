package com.df.knowledge.service.rag;

import java.util.List;

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
