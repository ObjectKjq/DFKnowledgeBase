package com.df.knowledge.service.conversation;

import com.df.knowledge.controller.conversation.vo.ConversationSendReqVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.service.rag.RagCitation;

import java.util.List;
import java.util.function.Consumer;

/** 会话发送编排，复用现有检索、引导和 RAG 服务。 */
public interface ConversationTurnService {

    /**
     * 执行一轮发送，首次输入创建会话，并返回尚未展示的消息。
     * @param userId 当前用户编号
     * @param request 输入和完整选择
     * @param onConversation 会话准备完成的接收者
     * @param onDelta 暂定答案增量接收者
     * @param onCitations 暂定引用集合接收者
     * @return 会话和新增消息
     */
    ConversationTurnResult send(Long userId, ConversationSendReqVO request,
                                Consumer<ConversationDO> onConversation, Consumer<String> onDelta,
                                Consumer<List<RagCitation>> onCitations);
}
