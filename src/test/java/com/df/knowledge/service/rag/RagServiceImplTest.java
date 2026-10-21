package com.df.knowledge.service.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.common.ErrorCode;
import com.df.knowledge.controller.rag.vo.RagAskReqVO;
import com.df.knowledge.controller.conversation.vo.MessageCreateReqVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.dal.dataobject.conversation.MessageDO;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.service.conversation.ConversationService;
import com.df.knowledge.service.doc.DocChunkResult;
import com.df.knowledge.service.doc.DocSearchService;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link RagServiceImpl} 的单元测试，答案生成端口使用桩
 */
@ExtendWith(MockitoExtension.class)
class RagServiceImplTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private KnowledgeBasePermissionService permissionService;

    @Mock
    private DocSearchService docSearchService;

    @Mock
    private RagGenerationPort generationPort;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RagServiceImpl ragService;

    @Test
    void testStream_deliversDeltasBeforeReturningCompleteCitations() {
        // 准备参数
        mockConversation(11L, List.of(2L));
        when(docSearchService.search(7L, "问题", List.of(2L)))
                .thenReturn(List.of(chunk(101L, 5L, "证据", 1, 0)));
        List<String> deltas = new ArrayList<>();
        List<List<RagCitation>> citationEvents = new ArrayList<>();
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);
            consumer.accept("可信答案。[");
            assertTrue(citationEvents.isEmpty());
            consumer.accept("1]");
            assertEquals(101L, citationEvents.getFirst().getFirst().getChunkId());
            consumer.accept("[1]");
            return new RagGenerationPort.GenerationResult(true, "可信答案。[1][1]", List.of(1, 1));
        }).when(generationPort).stream(any(), any());

        // 调用
        RagAnswerResult result = ragService.stream(7L, request(11L, "问题"), deltas::add, citationEvents::add);

        // 断言
        assertEquals("可信答案。[1][1]", result.getAnswer());
        assertEquals(101L, result.getCitations().getFirst().getChunkId());
        assertEquals(List.of("可信答案。[", "1]", "[1]"), deltas);
        assertEquals(1, citationEvents.size());
    }

    @Test
    void testStream_interruptedGenerationDoesNotPersistLateAnswer() {
        // 准备参数：模拟生成端口在取消后仍返回完整结果
        mockConversation(11L, List.of(2L));
        when(docSearchService.search(7L, "问题", List.of(2L)))
                .thenReturn(List.of(chunk(101L, 5L, "证据", 1, 0)));
        doAnswer(invocation -> {
            Thread.currentThread().interrupt();
            return new RagGenerationPort.GenerationResult(true, "迟到的答案。[1]", List.of(1));
        }).when(generationPort).stream(any(), any());

        // 调用并断言
        try {
            ServiceException error = assertThrows(ServiceException.class,
                    () -> ragService.stream(7L, request(11L, "问题"), delta -> { }, citations -> { }));
            assertEquals(ErrorCodeConstants.RAG_GENERATION_TIMEOUT.getCode(), error.getErrorCode().getCode());
            ArgumentCaptor<MessageCreateReqVO> message = ArgumentCaptor.forClass(MessageCreateReqVO.class);
            verify(conversationService).createMessage(eq(7L), message.capture());
            assertEquals("user", message.getValue().getType());
        } finally {
            Thread.interrupted();
        }
    }

    @ParameterizedTest
    @MethodSource("generationErrors")
    void testStream_failureKeepsQuestionAndDoesNotSavePartialAnswer(ErrorCode errorCode) {
        // 准备参数
        mockConversation(11L, List.of(2L));
        when(docSearchService.search(7L, "问题", List.of(2L)))
                .thenReturn(List.of(chunk(101L, 5L, "证据", 1, 0)));
        doAnswer(invocation -> {
            Consumer<String> delta = invocation.getArgument(1);
            delta.accept("部分答案");
            throw new ServiceException(errorCode);
        }).when(generationPort).stream(any(), any());
        List<String> deltas = new ArrayList<>();

        // 调用
        ServiceException error = assertThrows(ServiceException.class,
                () -> ragService.stream(7L, request(11L, "问题"), deltas::add, citations -> { }));

        // 断言：只持久化问题，重试不会继承未完成的答案
        assertEquals(errorCode.getCode(), error.getErrorCode().getCode());
        assertEquals(List.of("部分答案"), deltas);
        ArgumentCaptor<MessageCreateReqVO> message = ArgumentCaptor.forClass(MessageCreateReqVO.class);
        verify(conversationService).createMessage(eq(7L), message.capture());
        assertEquals("user", message.getValue().getType());
        assertEquals("问题", message.getValue().getContent().path("text").asText());
    }

    private static List<ErrorCode> generationErrors() {
        return List.of(ErrorCodeConstants.RAG_MODEL_UNAVAILABLE, ErrorCodeConstants.RAG_GENERATION_TIMEOUT);
    }

    @Test
    void testStream_invalidCitationsReturnRefusalAsFinalResult() {
        // 准备参数
        mockConversation(11L, List.of(2L));
        when(docSearchService.search(7L, "问题", List.of(2L)))
                .thenReturn(List.of(chunk(101L, 5L, "证据", 1, 0)));
        doAnswer(invocation -> {
            Consumer<String> delta = invocation.getArgument(1);
            delta.accept("无法验证。[9]");
            return new RagGenerationPort.GenerationResult(true, "无法验证。[9]", List.of(9));
        }).when(generationPort).stream(any(), any());

        // 调用
        RagAnswerResult result = ragService.stream(7L, request(11L, "问题"), delta -> { }, citations -> { });

        // 断言
        assertFalse(result.getAnswered());
        assertTrue(result.getCitations().isEmpty());
        assertTrue(result.getAnswer().contains("未找到足够依据"));
    }

    @Test
    void testAsk_mapsCitationsAndPersistsMessageFlow() {
        // 准备参数：模型引用编号按本次证据顺序映射，重复编号只保留一次
        mockConversation(11L, List.of(2L));
        DocChunkResult first = chunk(101L, 5L, "可信答案", 3, 1);
        DocChunkResult second = chunk(102L, 6L, "引用规则", null, 0);
        when(docSearchService.search(7L, "如何保证可信？", List.of(2L))).thenReturn(List.of(first, second));
        when(generationPort.generate(any())).thenReturn(
                new RagGenerationPort.GenerationResult(true, "答案应逐条附带引用。[2][1]", List.of(2, 1, 2)));

        // 调用
        RagAnswerResult result = ragService.ask(7L, request(11L, "如何保证可信？"));

        // 断言：引用可追溯到分块、文档与页，且问答完整写入消息流
        assertTrue(result.getAnswered());
        assertEquals("答案应逐条附带引用。[2][1]", result.getAnswer());
        assertEquals(List.of(102L, 101L), result.getCitations().stream().map(RagCitation::getChunkId).toList());
        assertEquals(List.of(2, 1), result.getCitations().stream().map(RagCitation::getNumber).toList());
        assertEquals(6L, result.getCitations().getFirst().getDocId());
        assertEquals(3, result.getCitations().get(1).getPageNumber());
        ArgumentCaptor<MessageCreateReqVO> messages = ArgumentCaptor.forClass(MessageCreateReqVO.class);
        verify(conversationService, org.mockito.Mockito.times(2)).createMessage(eq(7L), messages.capture());
        assertEquals(List.of("user", "answer"), messages.getAllValues().stream().map(MessageCreateReqVO::getType).toList());
        assertEquals(101L, messages.getAllValues().get(1).getContent().path("citations").get(1).path("chunkId").asLong());
    }

    @Test
    void testAsk_noEvidenceRefusesWithoutCallingModel() {
        // 准备参数：当前范围有权限，但没有检索到支撑分块
        mockConversation(11L, List.of(2L));
        when(docSearchService.search(7L, "不存在的内容", List.of(2L))).thenReturn(List.of());
        when(docSearchService.hasAvailableDocuments(7L, List.of(2L))).thenReturn(true);

        // 调用
        RagAnswerResult result = ragService.ask(7L, request(11L, "不存在的内容"));

        // 断言：拒答文案明确，包含扩大范围和换问法建议，不调用生成模型
        assertFalse(result.getAnswered());
        assertTrue(result.getAnswer().contains("未找到足够依据"));
        assertEquals(List.of("扩大检索范围", "换一种问法", "上传相关文档并等待索引完成"), result.getSuggestions());
        assertTrue(result.getCitations().isEmpty());
        verifyNoInteractions(generationPort);
    }

    @Test
    void testAsk_invalidModelCitationRefuses() {
        // 准备参数：模型声称有依据，但返回的引用编号不属于本次证据
        mockConversation(11L, List.of(2L));
        when(docSearchService.search(7L, "问题", List.of(2L)))
                .thenReturn(List.of(chunk(101L, 5L, "证据", 1, 0)));
        when(generationPort.generate(any())).thenReturn(
                new RagGenerationPort.GenerationResult(true, "没有可验证引用的答案", List.of(9)));

        // 调用
        RagAnswerResult result = ragService.ask(7L, request(11L, "问题"));

        // 断言：不把无法映射的引用或答案返回给用户
        assertFalse(result.getAnswered());
        assertTrue(result.getAnswer().contains("未找到足够依据"));
        assertTrue(result.getCitations().isEmpty());
    }

    @Test
    void testAsk_refusesAnswerWhenInlineCitationIsMissingOrUnmapped() {
        // 准备参数：答案里的引用标记必须和可映射的引用编号一致
        mockConversation(11L, List.of(2L));
        when(docSearchService.search(7L, "问题", List.of(2L)))
                .thenReturn(List.of(chunk(101L, 5L, "证据", 1, 0)));
        when(generationPort.generate(any())).thenReturn(
                new RagGenerationPort.GenerationResult(true, "无引用的断言", List.of(1)));

        // 调用
        RagAnswerResult result = ragService.ask(7L, request(11L, "问题"));

        // 断言
        assertFalse(result.getAnswered());
        assertTrue(result.getCitations().isEmpty());
    }

    @Test
    void testAsk_emptyKnowledgeBaseReturnsUploadGuidance() {
        // 准备参数：知识库可访问，但没有已完成索引的文档
        mockConversation(11L, List.of(2L));
        when(docSearchService.search(7L, "问题", List.of(2L))).thenReturn(List.of());
        when(docSearchService.hasAvailableDocuments(7L, List.of(2L))).thenReturn(false);

        // 调用
        RagAnswerResult result = ragService.ask(7L, request(11L, "问题"));

        // 断言
        assertFalse(result.getAnswered());
        assertTrue(result.getAnswer().contains("还没有可检索的文档"));
        assertTrue(result.getSuggestions().contains("上传文档并等待索引完成"));
    }

    @Test
    void testAsk_sameConversationUsesWindowAndNewConversationDoesNotInherit() {
        // 准备参数：会话 11 有历史，会话 22 无历史
        ConversationDO firstConversation = conversation(11L, List.of(2L));
        ConversationDO newConversation = conversation(22L, List.of(2L));
        MessageDO previousQuestion = message(1L, "user", "上一轮问题");
        MessageDO previousAnswer = answerMessage(2L, "上一轮答案", 2L);
        when(conversationService.getConversation(7L, 11L)).thenReturn(firstConversation);
        when(conversationService.getConversation(7L, 22L)).thenReturn(newConversation);
        when(conversationService.getMessageList(7L, 11L)).thenReturn(List.of(previousQuestion, previousAnswer));
        when(conversationService.getMessageList(7L, 22L)).thenReturn(List.of());
        when(permissionService.getAccessibleKnowledgeBaseIds(7L)).thenReturn(List.of(2L));
        when(docSearchService.search(eq(7L), any(), eq(List.of(2L))))
                .thenReturn(List.of(chunk(101L, 5L, "证据", 1, 0)));
        when(docSearchService.getPreview(7L, 101L)).thenReturn(chunk(101L, 5L, "当前文档原文", 1, 0));
        when(generationPort.generate(any())).thenReturn(
                new RagGenerationPort.GenerationResult(true, "回答[1]", List.of(1)));

        // 调用
        ragService.ask(7L, request(11L, "追问"));
        ragService.ask(7L, request(22L, "新问题"));

        // 断言：窗口只来自当前会话，新会话为空
        ArgumentCaptor<RagGenerationPort.GenerationRequest> requests =
                ArgumentCaptor.forClass(RagGenerationPort.GenerationRequest.class);
        verify(generationPort, org.mockito.Mockito.times(2)).generate(requests.capture());
        assertEquals(List.of("上一轮问题", "上一轮答案"), requests.getAllValues().getFirst().history().stream()
                .map(RagGenerationPort.ChatMessage::content).toList());
        assertTrue(requests.getAllValues().get(1).history().isEmpty());
    }

    @Test
    void testAsk_filtersConversationScopeByCurrentPermission() {
        // 准备参数：会话创建后，用户已失去知识库 99 的权限
        mockConversation(11L, List.of(2L, 99L));
        MessageDO previousQuestion = message(1L, "user", "仍可保留的上一轮问题");
        MessageDO revokedAnswer = answerMessage(2L, "来自已撤权知识库的答案", 99L);
        when(conversationService.getMessageList(7L, 11L)).thenReturn(List.of(
                previousQuestion, revokedAnswer));
        when(docSearchService.search(7L, "问题", List.of(2L)))
                .thenReturn(List.of(chunk(101L, 5L, "有权证据", 1, 0)));
        when(generationPort.generate(any())).thenReturn(
                new RagGenerationPort.GenerationResult(true, "回答[1]", List.of(1)));

        // 调用
        ragService.ask(7L, request(11L, "问题"));

        // 断言：无权知识库不进入检索或生成上下文
        verify(docSearchService).search(7L, "问题", List.of(2L));
        ArgumentCaptor<RagGenerationPort.GenerationRequest> request =
                ArgumentCaptor.forClass(RagGenerationPort.GenerationRequest.class);
        verify(generationPort).generate(request.capture());
        assertEquals(List.of(101L), request.getValue().evidence().stream()
                .map(RagGenerationPort.Evidence::chunkId).toList());
        assertEquals(List.of("仍可保留的上一轮问题"), request.getValue().history().stream()
                .map(RagGenerationPort.ChatMessage::content).toList());
    }

    @Test
    void testAsk_filtersHistoryAnswerWhenCitedDocumentIsUnavailable() {
        // 准备参数：上一轮答案引用的文档已删除或不可用
        mockConversation(11L, List.of(2L));
        MessageDO previousQuestion = message(1L, "user", "仍可保留的问题");
        MessageDO previousAnswer = answerMessage(2L, "不可重放的答案", 2L);
        when(conversationService.getMessageList(7L, 11L)).thenReturn(List.of(previousQuestion, previousAnswer));
        when(docSearchService.getPreview(7L, 101L)).thenThrow(new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND));
        when(docSearchService.search(7L, "问题", List.of(2L)))
                .thenReturn(List.of(chunk(102L, 6L, "当前证据", 1, 0)));
        when(generationPort.generate(any())).thenReturn(
                new RagGenerationPort.GenerationResult(true, "当前回答[1]", List.of(1)));

        // 调用
        ragService.ask(7L, request(11L, "问题"));

        // 断言：当前可见的用户提问保留，已不可用文档支撑的答案不进入上下文
        ArgumentCaptor<RagGenerationPort.GenerationRequest> generationRequest =
                ArgumentCaptor.forClass(RagGenerationPort.GenerationRequest.class);
        verify(generationPort).generate(generationRequest.capture());
        assertEquals(List.of("仍可保留的问题"), generationRequest.getValue().history().stream()
                .map(RagGenerationPort.ChatMessage::content).toList());
    }

    @Test
    void testAsk_emptyScopeReturnsClearGuidance() {
        // 准备参数：当前用户没有任何可访问知识库
        when(conversationService.getConversation(7L, 11L)).thenReturn(conversation(11L, List.of()));
        when(conversationService.getMessageList(7L, 11L)).thenReturn(List.of());
        when(permissionService.getAccessibleKnowledgeBaseIds(7L)).thenReturn(List.of());

        // 调用
        RagAnswerResult result = ragService.ask(7L, request(11L, "问题"));

        // 断言
        assertFalse(result.getAnswered());
        assertTrue(result.getAnswer().contains("没有可检索的知识库范围"));
        assertEquals(List.of("选择有权访问的知识库", "上传文档并等待索引完成"), result.getSuggestions());
        verifyNoInteractions(docSearchService, generationPort);
    }

    @Test
    void testAsk_modelUnavailableKeepsUserQuestionAndReturnsExplicitError() {
        // 准备参数
        mockConversation(11L, List.of(2L));
        when(docSearchService.search(7L, "问题", List.of(2L)))
                .thenReturn(List.of(chunk(101L, 5L, "证据", 1, 0)));
        doThrow(new ServiceException(ErrorCodeConstants.RAG_MODEL_UNAVAILABLE)).when(generationPort).generate(any());

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> ragService.ask(7L, request(11L, "问题")));
        assertEquals(ErrorCodeConstants.RAG_MODEL_UNAVAILABLE.getCode(), exception.getErrorCode().getCode());
        ArgumentCaptor<MessageCreateReqVO> message = ArgumentCaptor.forClass(MessageCreateReqVO.class);
        verify(conversationService).createMessage(eq(7L), message.capture());
        assertEquals("user", message.getValue().getType());
        assertEquals("问题", message.getValue().getContent().path("text").asText());
    }

    private void mockConversation(Long id, List<Long> scope) {
        when(conversationService.getConversation(7L, id)).thenReturn(conversation(id, scope));
        when(conversationService.getMessageList(7L, id)).thenReturn(List.of());
        when(permissionService.getAccessibleKnowledgeBaseIds(7L)).thenReturn(List.of(2L));
    }

    private static RagAskReqVO request(Long conversationId, String question) {
        RagAskReqVO reqVO = new RagAskReqVO();
        reqVO.setConversationId(conversationId);
        reqVO.setQuestion(question);
        return reqVO;
    }

    private static ConversationDO conversation(Long id, List<Long> scope) {
        ConversationDO conversation = new ConversationDO();
        conversation.setId(id);
        conversation.setUserId(7L);
        conversation.setKnowledgeBaseIds(scope);
        return conversation;
    }

    private MessageDO message(Long id, String type, String text) {
        MessageDO message = new MessageDO();
        message.setId(id);
        message.setType(type);
        message.setContent(objectMapper.createObjectNode().put(type.equals("user") ? "text" : "answer", text));
        message.setCreateTime(LocalDateTime.of(2026, 10, 21, 10, 0).plusSeconds(id));
        return message;
    }

    private MessageDO answerMessage(Long id, String text, Long knowledgeBaseId) {
        MessageDO message = message(id, "answer", text);
        ((ObjectNode) message.getContent()).set("citations", objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode().put("knowledgeBaseId", knowledgeBaseId)
                        .put("docId", 5L).put("chunkId", 101L)));
        return message;
    }

    private static DocChunkResult chunk(Long chunkId, Long docId, String text, Integer page, Integer index) {
        return new DocChunkResult().setKnowledgeBaseId(2L).setKnowledgeBaseName("我的知识库")
                .setDocId(docId).setDocTitle("文档 " + docId).setChunkId(chunkId)
                .setChunkIndex(index).setPageNumber(page).setStartOffset(10).setEndOffset(20).setText(text);
    }
}
