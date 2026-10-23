package com.df.knowledge.service.rag;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.conversation.vo.MessageCreateReqVO;
import com.df.knowledge.controller.rag.vo.RagAskReqVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.dal.dataobject.conversation.MessageDO;
import com.df.knowledge.service.conversation.ConversationService;
import com.df.knowledge.service.doc.DocChunkResult;
import com.df.knowledge.service.doc.DocSearchService;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

import static com.df.knowledge.enums.ErrorCodeConstants.DOC_SEARCH_QUERY_EMPTY;
import static com.df.knowledge.enums.ErrorCodeConstants.DOC_NOT_FOUND;
import static com.df.knowledge.enums.ErrorCodeConstants.RAG_MODEL_UNAVAILABLE;
import static com.df.knowledge.enums.ErrorCodeConstants.RAG_GENERATION_TIMEOUT;

/**
 * RAG 检索 Service 实现类
 */
@Service
@Validated
@Slf4j
public class RagServiceImpl implements RagService {

    private static final int HISTORY_WINDOW_SIZE = 10;
    private static final Pattern INLINE_CITATION_PATTERN = Pattern.compile("\\[(\\d+)]");
    private static final String INSUFFICIENT_EVIDENCE =
            "当前范围内未找到足够依据，因此无法可靠回答。请扩大检索范围、换一种问法，或上传相关文档并等待索引完成。";
    private static final List<String> INSUFFICIENT_SUGGESTIONS =
            List.of("扩大检索范围", "换一种问法", "上传相关文档并等待索引完成");
    private static final String EMPTY_SCOPE =
            "当前没有可检索的知识库范围。请选择有权访问的知识库，或先上传文档并等待索引完成。";
    private static final List<String> EMPTY_SCOPE_SUGGESTIONS =
            List.of("选择有权访问的知识库", "上传文档并等待索引完成");
    private static final String EMPTY_KNOWLEDGE_BASE =
            "当前范围内还没有可检索的文档。请上传文档并等待索引完成后再提问。";
    private static final List<String> EMPTY_KNOWLEDGE_BASE_SUGGESTIONS = List.of("上传文档并等待索引完成");

    @Resource
    private ConversationService conversationService;

    @Resource
    private KnowledgeBasePermissionService permissionService;

    @Resource
    private DocSearchService docSearchService;

    @Resource
    private RagGenerationPort generationPort;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public RagAnswerResult ask(Long userId, RagAskReqVO reqVO) {
        return answer(userId, reqVO, null, null, false);
    }

    @Override
    public RagAnswerResult stream(Long userId, RagAskReqVO reqVO, Consumer<String> onDelta,
                                  Consumer<List<RagCitation>> onCitations) {
        return answer(userId, reqVO, onDelta, onCitations, false);
    }

    @Override
    public RagAnswerResult askReadOnly(Long userId, RagAskReqVO reqVO) {
        return answer(userId, reqVO, null, null, true);
    }

    private RagAnswerResult answer(Long userId, RagAskReqVO reqVO, Consumer<String> onDelta,
                                   Consumer<List<RagCitation>> onCitations, boolean readOnly) {
        if (StrUtil.isBlank(reqVO.getQuestion())) {
            throw new ServiceException(DOC_SEARCH_QUERY_EMPTY);
        }
        String question = reqVO.getQuestion().trim();
        ConversationDO conversation = readOnly ? new ConversationDO() : conversationService.getConversation(userId, reqVO.getConversationId());
        var state = conversation.getState() == null ? new com.df.knowledge.service.conversation.RetrievalState()
                .setKnowledgeBaseIds(conversation.getKnowledgeBaseIds() == null ? List.of() : conversation.getKnowledgeBaseIds())
                : conversation.getState();
        if (reqVO.getKnowledgeBaseIds() != null) state.setKnowledgeBaseIds(reqVO.getKnowledgeBaseIds());
        if (reqVO.getSelectedChunkIds() != null) state.setSelectedChunkIds(reqVO.getSelectedChunkIds());
        if (reqVO.getModelId() != null) state.setModelId(reqVO.getModelId());
        if (reqVO.getFormats() != null) state.setFormats(reqVO.getFormats());
        if (reqVO.getUploadedFrom() != null) state.setUploadedFrom(reqVO.getUploadedFrom());
        if (reqVO.getUploadedTo() != null) state.setUploadedTo(reqVO.getUploadedTo());
        state.setMode("RAG");
        List<Long> scope = resolveCurrentScope(userId, state.getKnowledgeBaseIds());
        // 保留用户选择的范围；每轮重新授权，避免失权后空集合被解释为全部知识库。
        if (!readOnly) conversationService.updateState(userId, conversation.getId(), state);
        List<Long> selected = state.getSelectedChunkIds() == null ? List.of() : state.getSelectedChunkIds();
        List<RagGenerationPort.ChatMessage> history = readOnly || !selected.isEmpty() ? List.of()
                : loadHistory(userId, conversation.getId(), Set.copyOf(scope));
        // 问题先持久化：模型失败时仍可重试，状态快照不随后续选择改变。
        if (!readOnly) saveUserMessage(userId, conversation.getId(), question, scope, state);
        if (scope.isEmpty()) {
            return refuse(userId, conversation.getId(), question, EMPTY_SCOPE, EMPTY_SCOPE_SUGGESTIONS);
        }

        List<DocChunkResult> chunks;
        if (!selected.isEmpty()) {
            chunks = new ArrayList<>();
            for (Long id : new LinkedHashSet<>(selected)) {
                try {
                    var chunk = docSearchService.getPreview(userId, id);
                    if (!scope.contains(chunk.getKnowledgeBaseId()) || !matchesSelection(chunk, state))
                        return refuse(userId, conversation.getId(), question, "所选上下文当前不可用，请调整或清空后重试。", List.of("调整所选上下文", "清空所选上下文"));
                    chunks.add(chunk);
                } catch (ServiceException ex) {
                    if (ex.getErrorCode().getCode() != DOC_NOT_FOUND.getCode()) throw ex;
                    return refuse(userId, conversation.getId(), question, "所选上下文当前不可用，请调整或清空后重试。", List.of("调整所选上下文", "清空所选上下文"));
                }
            }
        } else {
            var search = new com.df.knowledge.controller.doc.vo.DocSearchReqVO().setQuery(question)
                    .setKnowledgeBaseIds(scope).setFormats(state.getFormats())
                    .setUploadedFrom(state.getUploadedFrom()).setUploadedTo(state.getUploadedTo());
            chunks = docSearchService.search(userId, search).getList();
        }
        if (chunks.isEmpty()) {
            if (!docSearchService.hasAvailableDocuments(userId, scope)) {
                return refuse(userId, conversation.getId(), question,
                        EMPTY_KNOWLEDGE_BASE, EMPTY_KNOWLEDGE_BASE_SUGGESTIONS);
            }
            return refuse(userId, conversation.getId(), question,
                    INSUFFICIENT_EVIDENCE, INSUFFICIENT_SUGGESTIONS);
        }

        final List<DocChunkResult> evidenceChunks = List.copyOf(chunks);
        RagGenerationPort.GenerationResult generated;
        try {
            RagGenerationPort.GenerationRequest request = new RagGenerationPort.GenerationRequest(
                    question, history, toEvidence(evidenceChunks), state.getModelId());
            if (onDelta == null) {
                generated = generationPort.generate(request);
            } else {
                StringBuilder partial = new StringBuilder();
                Set<Integer> sentNumbers = new LinkedHashSet<>();
                generated = generationPort.stream(request, delta -> {
                    onDelta.accept(delta);
                    partial.append(delta);
                    Matcher matcher = INLINE_CITATION_PATTERN.matcher(partial);
                    boolean changed = false;
                    while (matcher.find()) {
                        // 超长或越界编号不作为有效引用发送，由最终校验决定是否拒答
                        try {
                            int number = Integer.parseInt(matcher.group(1));
                            if (number >= 1 && number <= evidenceChunks.size()) {
                                changed |= sentNumbers.add(number);
                            }
                        } catch (NumberFormatException ignored) {
                            // 最终引用校验会拒绝超长编号
                        }
                    }
                    if (changed) {
                        onCitations.accept(mapCitations(evidenceChunks, List.copyOf(sentNumbers)));
                    }
                });
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("[ask][对话模型调用失败，会话编号({})]", conversation.getId(), ex);
            throw new ServiceException(RAG_MODEL_UNAVAILABLE);
        }
        if (generated == null || !generated.sufficientEvidence() || StrUtil.isBlank(generated.answer())) {
            return refuse(userId, conversation.getId(), question,
                    INSUFFICIENT_EVIDENCE, INSUFFICIENT_SUGGESTIONS);
        }

        if (generated.citationNumbers() == null || generated.citationNumbers().stream()
                .anyMatch(n -> n == null || n < 1 || n > evidenceChunks.size()) || !stillAuthorized(userId, evidenceChunks, scope)) {
            return refuse(userId, conversation.getId(), question, INSUFFICIENT_EVIDENCE, INSUFFICIENT_SUGGESTIONS);
        }
        List<RagCitation> citations = mapCitations(evidenceChunks, generated.citationNumbers());
        if (!hasValidInlineCitations(generated.answer(), citations)) {
            return refuse(userId, conversation.getId(), question,
                    INSUFFICIENT_EVIDENCE, INSUFFICIENT_SUGGESTIONS);
        }
        RagAnswerResult result = new RagAnswerResult().setAnswered(true).setAnswer(generated.answer().trim())
                .setSuggestions(List.of()).setCitations(citations);
        saveResultMessage(userId, conversation.getId(), "answer", question, result);
        return result;
    }

    private boolean matchesSelection(DocChunkResult chunk, com.df.knowledge.service.conversation.RetrievalState state) {
        return (state.getFormats() == null || state.getFormats().isEmpty() || state.getFormats().contains(chunk.getFormat()))
                && (state.getUploadedFrom() == null || chunk.getUploadTime() != null && !chunk.getUploadTime().isBefore(state.getUploadedFrom()))
                && (state.getUploadedTo() == null || chunk.getUploadTime() != null && !chunk.getUploadTime().isAfter(state.getUploadedTo()));
    }

    private boolean stillAuthorized(Long userId, List<DocChunkResult> chunks, List<Long> scope) {
        Set<Long> current = Set.copyOf(resolveCurrentScope(userId, scope));
        for (DocChunkResult chunk : chunks) {
            if (!current.contains(chunk.getKnowledgeBaseId())) return false;
            try {
                var preview = docSearchService.getPreview(userId, chunk.getChunkId());
                if (preview == null || !java.util.Objects.equals(preview.getDocId(), chunk.getDocId())
                        || !java.util.Objects.equals(preview.getText(), chunk.getText())) return false;
            } catch (ServiceException ex) {
                if (ex.getErrorCode().getCode() == DOC_NOT_FOUND.getCode()) return false;
                throw ex;
            }
        }
        return true;
    }

    private List<Long> resolveCurrentScope(Long userId, List<Long> recordedScope) {
        List<Long> accessible = permissionService.getAccessibleKnowledgeBaseIds(userId);
        if (recordedScope == null || recordedScope.isEmpty()) {
            return List.copyOf(new LinkedHashSet<>(accessible));
        }
        Set<Long> accessibleSet = Set.copyOf(accessible);
        return recordedScope.stream().filter(accessibleSet::contains).distinct().toList();
    }

    private List<RagGenerationPort.ChatMessage> loadHistory(Long userId, Long conversationId, Set<Long> currentScope) {
        List<MessageDO> messages = conversationService.getMessageList(userId, conversationId);
        int fromIndex = Math.max(0, messages.size() - HISTORY_WINDOW_SIZE);
        List<RagGenerationPort.ChatMessage> history = messages.subList(fromIndex, messages.size()).stream()
                .map(message -> toHistoryMessage(userId, message, currentScope))
                .filter(message -> message != null).toList();
        return List.copyOf(history);
    }

    private RagGenerationPort.ChatMessage toHistoryMessage(Long userId, MessageDO message, Set<Long> currentScope) {
        JsonNode content = message.getContent();
        if (content == null) {
            return null;
        }
        String value;
        String role;
        if ("user".equals(message.getType())) {
            role = "user";
            value = content.path("text").asText();
        } else if ("answer".equals(message.getType())) {
            if (!hasOnlyCurrentCitations(userId, content, currentScope)) {
                return null;
            }
            role = "assistant";
            value = content.path("answer").asText();
        } else {
            return null;
        }
        return StrUtil.isBlank(value) ? null : new RagGenerationPort.ChatMessage(role, value);
    }

    private boolean hasOnlyCurrentCitations(Long userId, JsonNode content, Set<Long> currentScope) {
        JsonNode citations = content.path("citations");
        if (!citations.isArray() || citations.isEmpty()) {
            return false;
        }
        for (JsonNode citation : citations) {
            if (!citation.hasNonNull("knowledgeBaseId") || !citation.hasNonNull("docId")
                    || !citation.hasNonNull("chunkId")) {
                return false;
            }
            Long knowledgeBaseId = citation.path("knowledgeBaseId").asLong();
            Long docId = citation.path("docId").asLong();
            if (!currentScope.contains(knowledgeBaseId)) {
                return false;
            }
            try {
                DocChunkResult currentChunk = docSearchService.getPreview(userId, citation.path("chunkId").asLong());
                if (currentChunk == null || !knowledgeBaseId.equals(currentChunk.getKnowledgeBaseId())
                        || !docId.equals(currentChunk.getDocId())) {
                    return false;
                }
            } catch (ServiceException ex) {
                if (DOC_NOT_FOUND.getCode() == ex.getErrorCode().getCode()) {
                    return false;
                }
                throw ex;
            }
        }
        return true;
    }

    private static List<RagGenerationPort.Evidence> toEvidence(List<DocChunkResult> chunks) {
        List<RagGenerationPort.Evidence> evidence = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            DocChunkResult chunk = chunks.get(i);
            evidence.add(new RagGenerationPort.Evidence(i + 1, chunk.getChunkId(), chunk.getDocTitle(),
                    chunk.getPageNumber(), chunk.getChunkIndex(), chunk.getText()));
        }
        return evidence;
    }

    private static List<RagCitation> mapCitations(List<DocChunkResult> chunks, List<Integer> numbers) {
        if (numbers == null) {
            return List.of();
        }
        List<RagCitation> citations = new ArrayList<>();
        for (Integer number : new LinkedHashSet<>(numbers)) {
            if (number == null || number < 1 || number > chunks.size()) {
                continue;
            }
            DocChunkResult chunk = chunks.get(number - 1);
            citations.add(new RagCitation().setNumber(number).setKnowledgeBaseId(chunk.getKnowledgeBaseId())
                    .setKnowledgeBaseName(chunk.getKnowledgeBaseName()).setDocId(chunk.getDocId())
                    .setDocTitle(chunk.getDocTitle()).setChunkId(chunk.getChunkId())
                    .setChunkIndex(chunk.getChunkIndex()).setPageNumber(chunk.getPageNumber())
                    .setStartOffset(chunk.getStartOffset()).setEndOffset(chunk.getEndOffset()));
        }
        return citations;
    }

    private static boolean hasValidInlineCitations(String answer, List<RagCitation> citations) {
        if (citations.isEmpty()) {
            return false;
        }
        Set<Integer> mappedNumbers = citations.stream().map(RagCitation::getNumber)
                .collect(java.util.stream.Collectors.toSet());
        Set<Integer> inlineNumbers = new LinkedHashSet<>();
        Matcher matcher = INLINE_CITATION_PATTERN.matcher(answer);
        while (matcher.find()) {
            try {
                inlineNumbers.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return !inlineNumbers.isEmpty() && inlineNumbers.equals(mappedNumbers);
    }

    private RagAnswerResult refuse(Long userId, Long conversationId, String question,
                                   String message, List<String> suggestions) {
        RagAnswerResult result = new RagAnswerResult().setAnswered(false).setAnswer(message)
                .setSuggestions(suggestions).setCitations(List.of());
        saveResultMessage(userId, conversationId, "empty", question, result);
        return result;
    }

    private void saveUserMessage(Long userId, Long conversationId, String question, List<Long> scope,
                                 com.df.knowledge.service.conversation.RetrievalState state) {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("text", question);
        content.put("mode", "RAG");
        content.set("state", objectMapper.valueToTree(state));
        content.set("knowledgeBaseIds", objectMapper.valueToTree(scope));
        saveMessage(userId, conversationId, "user", content);
    }

    private void saveResultMessage(Long userId, Long conversationId, String type,
                                   String question, RagAnswerResult result) {
        // 流已超时或客户端断开时，不将迟到的生成结果写入会话
        if (Thread.currentThread().isInterrupted()) {
            throw new ServiceException(RAG_GENERATION_TIMEOUT);
        }
        ObjectNode content = objectMapper.valueToTree(result);
        content.put("question", question);
        saveMessage(userId, conversationId, type, content);
    }

    private void saveMessage(Long userId, Long conversationId, String type, JsonNode content) {
        if (conversationId == null) return;
        MessageCreateReqVO message = new MessageCreateReqVO();
        message.setConversationId(conversationId);
        message.setType(type);
        message.setContent(content);
        conversationService.createMessage(userId, message);
    }
}
