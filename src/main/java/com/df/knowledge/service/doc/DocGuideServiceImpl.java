package com.df.knowledge.service.doc;

import cn.hutool.core.util.StrUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.conversation.vo.MessageCreateReqVO;
import com.df.knowledge.controller.doc.vo.DocGuideReqVO;
import com.df.knowledge.controller.doc.vo.DocSearchReqVO;
import com.df.knowledge.service.conversation.ConversationService;
import com.df.knowledge.service.conversation.RetrievalState;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import com.df.knowledge.service.model.ModelConfigurationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/** 引导最多两轮，模型不可用时说明原因并返回真实混合检索结果。 */
@Service
@Validated
@Slf4j
public class DocGuideServiceImpl implements DocGuideService {
    @Resource
    private ConversationService conversationService;

    @Resource
    private KnowledgeBasePermissionService permissionService;

    @Resource
    private DocSearchService searchService;

    @Resource
    private ModelConfigurationService modelConfigurationService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public DocGuideResult guide(Long userId, DocGuideReqVO request) {
        // 同一会话串行更新轮数；当前读隔离让外部调用后的授权复查观察已提交撤权。
        var conversation = conversationService.getConversation(userId, request.getConversationId());
        RetrievalState state = conversation.getState() == null ? new RetrievalState()
                .setKnowledgeBaseIds(conversation.getKnowledgeBaseIds()) : conversation.getState();
        if (!request.getQuery().trim().equals(state.getGuideQuestion())) {
            state.setGuideQuestion(request.getQuery().trim()).setGuideRound(0).setGuideAnswers(List.of());
        }
        if (request.getKnowledgeBaseIds() != null) {
            state.setKnowledgeBaseIds(request.getKnowledgeBaseIds());
        }
        var accessible = permissionService.getAccessibleKnowledgeBaseIds(userId);
        var chosen = state.getKnowledgeBaseIds();
        var effectiveScope = chosen == null || chosen.isEmpty() ? accessible : accessible.stream().filter(chosen::contains).toList();
        if (request.getFormats() != null) {
            state.setFormats(request.getFormats());
        }
        if (request.getUploadedFrom() != null) {
            state.setUploadedFrom(request.getUploadedFrom());
        }
        if (request.getUploadedTo() != null) {
            state.setUploadedTo(request.getUploadedTo());
        }
        if (request.getModelId() != null) {
            state.setModelId(request.getModelId());
        }
        if (StrUtil.isNotBlank(request.getAnswer()) && state.getGuideRound() > 0 && state.getGuideRound() <= 2) {
            var answers = new ArrayList<>(state.getGuideAnswers());
            answers.add(request.getAnswer().trim());
            state.setGuideAnswers(answers.stream().limit(2).toList());
        }
        state.setMode("GUIDED");
        save(userId, request.getConversationId(), "user", objectMapper.createObjectNode()
                .put("text", StrUtil.isBlank(request.getAnswer()) ? request.getQuery() : request.getAnswer())
                .set("state", objectMapper.valueToTree(state)));
        if (effectiveScope.isEmpty()) {
            return finish(userId, request, state, "当前选择范围内没有有权内容，已直接执行混合检索。");
        }
        if (request.isSkip() || state.getGuideRound() >= 2) {
            return finish(userId, request, state, null);
        }
        String question;
        List<String> options = new ArrayList<>();
        try {
            String prompt = "你是知识库检索澄清助手。最多澄清两轮，只输出 JSON，包含 question 中文问题及 options 字符串数组（最多6项）。"
                    + "用户可自由回答或跳过。原始问题：" + state.getGuideQuestion() + "；已回答：" + objectMapper.writeValueAsString(state.getGuideAnswers());
            String raw = modelConfigurationService.chat(prompt, state.getModelId());
            var json = objectMapper.readTree(raw);
            question = json.path("question").asText();
            if (question.isBlank() || question.length() > 2000 || !json.path("options").isArray()) {
                throw new IllegalArgumentException("无效澄清内容");
            }
            for (var option : json.path("options")) {
                if (option.isTextual() && !option.asText().isBlank()) {
                    options.add(StrUtil.subPre(option.asText(), 200));
                }
                if (options.size() == 6) {
                    break;
                }
            }
        } catch (ServiceException ex) {
            // 仅记录公共错误码，不能把提供方响应或密钥带入会话。
            log.warn("[guide][引导模型不可用，错误码({})]", ex.getErrorCode().getCode());
            return finish(userId, request, state, "引导模型不可用：" + ex.getErrorCode().getMsg() + "，已直接执行混合检索。");
        } catch (Exception ex) {
            log.warn("[guide][引导模型响应无法解析]");
            return finish(userId, request, state, "引导模型响应无效，已直接执行混合检索。");
        }
        state.setGuideRound(state.getGuideRound() + 1);
        conversationService.updateState(userId, request.getConversationId(), state);
        var result = new DocGuideResult().setStage("CLARIFY").setRound(state.getGuideRound())
                .setQuestion(question).setOptions(options).setOriginalQuestion(state.getGuideQuestion()).setState(state);
        var content = objectMapper.valueToTree(result);
        save(userId, request.getConversationId(), "guide", content);
        return result;
    }

    private DocGuideResult finish(Long userId, DocGuideReqVO request, RetrievalState state, String reason) {
        state.setMode("HYBRID");
        conversationService.updateState(userId, request.getConversationId(), state);
        String query = state.getGuideQuestion() + (state.getGuideAnswers().isEmpty() ? "" : " " + String.join(" ", state.getGuideAnswers()));
        var search = new DocSearchReqVO().setQuery(query).setKnowledgeBaseIds(state.getKnowledgeBaseIds())
                .setFormats(state.getFormats()).setUploadedFrom(state.getUploadedFrom()).setUploadedTo(state.getUploadedTo())
                .setPageNo(request.getPageNo()).setPageSize(request.getPageSize());
        var result = new DocGuideResult().setStage(reason == null ? "RESULTS" : "FALLBACK")
                .setReason(reason).setRound(state.getGuideRound()).setSearch(searchService.search(userId, search))
                .setQuery(query).setOriginalQuestion(state.getGuideQuestion()).setState(state);
        var content = objectMapper.valueToTree(result);
        save(userId, request.getConversationId(), "results", content);
        return result;
    }

    private void save(Long userId, Long id, String type, JsonNode content) {
        conversationService.createMessage(userId, new MessageCreateReqVO().setConversationId(id).setType(type).setContent(content));
    }
}
