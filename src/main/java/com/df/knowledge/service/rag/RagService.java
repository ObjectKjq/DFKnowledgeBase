package com.df.knowledge.service.rag;

import com.df.knowledge.controller.rag.vo.RagAskReqVO;

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
}
