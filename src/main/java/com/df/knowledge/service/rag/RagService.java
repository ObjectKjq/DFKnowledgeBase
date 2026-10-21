package com.df.knowledge.service.rag;

import com.df.knowledge.controller.rag.vo.RagAskReqVO;

import java.util.List;
import java.util.function.Consumer;

/**
 * RAG 检索 Service 接口
 */
public interface RagService {

    /**
     * 在当前用户会话中同步执行检索与带引用生成
     *
     * @param userId 当前用户编号
     * @param reqVO 问答请求
     * @return 答案或拒答结果
     */
    RagAnswerResult ask(Long userId, RagAskReqVO reqVO);

    /**
     * 流式执行问答；增量为暂定内容，以最终返回结果为准
     *
     * @param userId 当前用户编号
     * @param reqVO 问答请求
     * @param onDelta 答案文本增量接收者
     * @param onCitations 当前已识别引用集合的接收者
     * @return 校验后的完整答案或拒答结果
     */
    RagAnswerResult stream(Long userId, RagAskReqVO reqVO, Consumer<String> onDelta,
                           Consumer<List<RagCitation>> onCitations);
}
