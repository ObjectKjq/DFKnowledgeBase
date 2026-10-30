package com.df.knowledge.service.conversation;

import cn.hutool.core.util.StrUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.conversation.vo.ConversationSendReqVO;
import com.df.knowledge.controller.doc.vo.DocGuideReqVO;
import com.df.knowledge.controller.doc.vo.DocSearchReqVO;
import com.df.knowledge.controller.rag.vo.RagAskReqVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.service.doc.DocGuideService;
import com.df.knowledge.service.doc.DocSearchService;
import com.df.knowledge.service.rag.RagCitation;
import com.df.knowledge.service.rag.RagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.function.Consumer;

import static com.df.knowledge.enums.ErrorCodeConstants.RAG_GENERATION_TIMEOUT;

/** 根据本轮选择分派发送，最终结果从持久化消息中读取。 */
@Service
@Validated
@Slf4j
public class ConversationTurnServiceImpl implements ConversationTurnService {

    @Resource
    private ConversationService conversationService;

    @Resource
    private DocSearchService docSearchService;

    @Resource
    private DocGuideService docGuideService;

    @Resource
    private RagService ragService;

    @Override
    public ConversationTurnResult send(Long userId, ConversationSendReqVO request,
                                       Consumer<ConversationDO> onConversation, Consumer<String> onDelta,
                                       Consumer<List<RagCitation>> onCitations) {
        var state = request.getState();
        var conversation = conversationService.beginTurn(userId, request.getConversationId(),
                StrUtil.subPre(request.getQuestion().trim(), 100), state);
        Long id = conversation.getId();
        onConversation.accept(conversation);
        switch (state.getMode()) {
            case "RAG" -> ragService.stream(userId, new RagAskReqVO().setConversationId(id)
                    .setQuestion(request.getQuestion()).setKnowledgeBaseIds(state.getKnowledgeBaseIds())
                    .setFormats(state.getFormats()).setUploadedFrom(state.getUploadedFrom())
                    .setUploadedTo(state.getUploadedTo()).setSelectedChunkIds(state.getSelectedChunkIds())
                    .setModelId(state.getModelId()), onDelta, onCitations);
            case "GUIDED" -> {
                var guide = new DocGuideReqVO().setConversationId(id)
                        .setAnswer(request.getGuideAnswer()).setSkip(request.isSkipGuide()).setModelId(state.getModelId());
                guide.setQuery(StrUtil.blankToDefault(request.getGuideQuestion(), request.getQuestion()))
                        .setKnowledgeBaseIds(state.getKnowledgeBaseIds()).setFormats(state.getFormats())
                        .setUploadedFrom(state.getUploadedFrom()).setUploadedTo(state.getUploadedTo());
                docGuideService.guide(userId, guide);
            }
            default -> docSearchService.search(userId, new DocSearchReqVO().setConversationId(id)
                    .setQuery(request.getQuestion()).setMode(state.getMode())
                    .setKnowledgeBaseIds(state.getKnowledgeBaseIds()).setFormats(state.getFormats())
                    .setUploadedFrom(state.getUploadedFrom()).setUploadedTo(state.getUploadedTo()));
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new ServiceException(RAG_GENERATION_TIMEOUT);
        }
        return new ConversationTurnResult(conversationService.getConversation(userId, id),
                conversationService.getMessageListAfter(userId, id, request.getAfterMessageId()));
    }
}
