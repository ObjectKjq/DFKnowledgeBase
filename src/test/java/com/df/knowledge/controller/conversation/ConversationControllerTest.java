package com.df.knowledge.controller.conversation;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.ErrorCode;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.conversation.vo.ConversationSendReqVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.dal.dataobject.conversation.MessageDO;
import com.df.knowledge.service.conversation.ConversationTurnResult;
import com.df.knowledge.service.conversation.ConversationTurnService;
import com.df.knowledge.service.conversation.RetrievalState;
import com.df.knowledge.service.rag.RagCitation;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.servlet.AsyncEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.df.knowledge.enums.ErrorCodeConstants.CONVERSATION_NOT_FOUND;
import static com.df.knowledge.enums.ErrorCodeConstants.RAG_GENERATION_TIMEOUT;
import static com.df.knowledge.enums.ErrorCodeConstants.RAG_MODEL_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** 会话 Flux SSE 的协议、总时限和取消订阅测试。 */
class ConversationControllerTest {

    private ConversationController controller;
    private ConversationTurnService service;
    private ExecutorService executor;
    private Scheduler scheduler;
    private MockedStatic<StpUtil> login;

    @BeforeEach
    void setUp() {
        service = mock(ConversationTurnService.class);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        scheduler = Schedulers.fromExecutorService(executor);
        controller = new ConversationController();
        ReflectionTestUtils.setField(controller, "conversationTurnService", service);
        ReflectionTestUtils.setField(controller, "ragStreamScheduler", scheduler);
        ReflectionTestUtils.setField(controller, "streamTimeoutMs", 65000L);
        login = mockStatic(StpUtil.class);
        login.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
    }

    @AfterEach
    void tearDown() {
        scheduler.dispose();
        executor.close();
        login.close();
    }

    @Test
    void testSendStream_httpProtocolAndFinalMessages() throws Exception {
        // 准备参数
        doAnswer(invocation -> {
            Consumer<ConversationDO> conversation = invocation.getArgument(2);
            Consumer<String> delta = invocation.getArgument(3);
            Consumer<List<RagCitation>> citations = invocation.getArgument(4);
            conversation.accept(result().conversation());
            delta.accept("暂定答案");
            citations.accept(List.of(new RagCitation().setNumber(1).setChunkId(101L)));
            return result();
        }).when(service).send(eq(7L), any(), any(), any(), any());
        MockMvc mvc = mockMvc();

        // 调用
        MvcResult pending = open(mvc);
        pending.getAsyncResult(5000);
        var response = mvc.perform(asyncDispatch(pending)).andReturn().getResponse();
        String body = response.getContentAsString(StandardCharsets.UTF_8);

        // 断言
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentType().startsWith("text/event-stream"));
        assertEquals("no-cache", response.getHeader("Cache-Control"));
        assertEquals("no", response.getHeader("X-Accel-Buffering"));
        assertEquals(List.of("conversation", "delta", "citations", "done"), body.lines()
                .filter(line -> line.startsWith("event:")).map(line -> line.substring(6).trim()).toList());
        assertTrue(body.contains("\"data\":\"暂定答案\""));
        assertTrue(body.contains("\"chunkId\":101"));
        assertTrue(body.contains("\"text\":\"最终答案\""));
        assertEquals(4, body.lines().filter(line -> line.startsWith("data:") && line.contains("\"code\":0")).count());
    }

    @Test
    void testSendStream_emitsDeltaBeforeGenerationCompletes() {
        // 准备参数：最终结果必须等客户端接收增量后才能返回。
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            Consumer<String> delta = invocation.getArgument(3);
            delta.accept("暂定答案");
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return result();
        }).when(service).send(eq(7L), any(), any(), any(), any());

        // 调用并断言
        StepVerifier.create(stream())
                .assertNext(event -> {
                    assertEquals("delta", event.event());
                    assertEquals("暂定答案", event.data().getData());
                })
                .then(release::countDown)
                .assertNext(event -> assertEquals("done", event.event()))
                .expectComplete().verify(Duration.ofSeconds(5));
    }

    @Test
    void testSendStream_businessErrorEndsWithoutDone() {
        // 准备参数
        doAnswer(invocation -> {
            Consumer<String> delta = invocation.getArgument(3);
            delta.accept("部分答案");
            throw new ServiceException(CONVERSATION_NOT_FOUND);
        }).when(service).send(eq(7L), any(), any(), any(), any());

        // 调用并断言
        StepVerifier.create(stream())
                .assertNext(event -> assertEquals("delta", event.event()))
                .assertNext(event -> assertError(event, CONVERSATION_NOT_FOUND))
                .expectComplete().verify(Duration.ofSeconds(5));
    }

    @Test
    void testSendStream_runtimeErrorReturnsModelUnavailable() {
        // 准备参数
        when(service.send(eq(7L), any(), any(), any(), any())).thenThrow(new IllegalStateException("模型调用失败"));

        // 调用并断言
        StepVerifier.create(stream())
                .assertNext(event -> assertError(event, RAG_MODEL_UNAVAILABLE))
                .expectComplete().verify(Duration.ofSeconds(5));
    }

    @Test
    void testSendStream_totalTimeoutDespiteDeltasInterruptsGeneration() throws Exception {
        // 准备参数：模型持续等待，并允许在总时限前继续发送增量。
        AtomicReference<Consumer<String>> onDelta = new AtomicReference<>();
        CountDownLatch interrupted = new CountDownLatch(1);
        doAnswer(invocation -> {
            Consumer<String> delta = invocation.getArgument(3);
            onDelta.set(delta);
            delta.accept("开始");
            awaitInterruption(interrupted);
            return result();
        }).when(service).send(eq(7L), any(), any(), any(), any());

        // 调用并断言：第 64 秒仍有增量，第 65 秒必须结束整轮请求。
        StepVerifier.withVirtualTime(this::stream)
                .assertNext(event -> assertEquals("开始", event.data().getData()))
                .thenAwait(Duration.ofSeconds(64))
                .then(() -> onDelta.get().accept("继续"))
                .assertNext(event -> assertEquals("继续", event.data().getData()))
                .thenAwait(Duration.ofSeconds(1))
                .assertNext(event -> assertError(event, RAG_GENERATION_TIMEOUT))
                .expectComplete().verify(Duration.ofSeconds(5));
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
    }

    @Test
    void testSendStream_timeoutInterruptsGenerationWithoutDemand() {
        // 准备参数：客户端暂停消费时，生成任务仍受整轮总时限约束。
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            awaitInterruption(interrupted);
            return result();
        }).when(service).send(eq(7L), any(), any(), any(), any());

        // 调用并断言：恢复消费前，任务已经因超时中断。
        StepVerifier.withVirtualTime(this::stream, 0)
                .then(() -> assertDoesNotThrow(() -> assertTrue(started.await(5, TimeUnit.SECONDS))))
                .thenAwait(Duration.ofSeconds(65))
                .then(() -> assertDoesNotThrow(() -> assertTrue(interrupted.await(5, TimeUnit.SECONDS))))
                .thenRequest(1)
                .assertNext(event -> assertError(event, RAG_GENERATION_TIMEOUT))
                .expectComplete().verify(Duration.ofSeconds(5));
    }

    @Test
    void testSendStream_disconnectionInterruptsGeneration() throws Exception {
        // 准备参数
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            awaitInterruption(interrupted);
            return result();
        }).when(service).send(eq(7L), any(), any(), any(), any());
        MockMvc mvc = mockMvc();

        // 调用：模拟 Servlet 通知客户端断开。
        MvcResult pending = open(mvc);
        assertTrue(started.await(5, TimeUnit.SECONDS));
        MockAsyncContext context = (MockAsyncContext) pending.getRequest().getAsyncContext();
        for (var listener : context.getListeners()) {
            listener.onError(new AsyncEvent(context, new IOException("客户端断开")));
        }

        // 断言
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
    }

    private Flux<ServerSentEvent<CommonResult<?>>> stream() {
        var request = new ConversationSendReqVO().setQuestion("问题").setState(new RetrievalState());
        return controller.sendStream(request, new MockHttpServletRequest()).getBody();
    }

    private MockMvc mockMvc() {
        // 与 application.yaml 中 preferred-json-mapper: jackson2 保持一致。
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter()).build();
    }

    private MvcResult open(MockMvc mvc) throws Exception {
        return mvc.perform(post("/knowledge/conversation/send-stream").contentType("application/json")
                .accept("text/event-stream")
                .content("{\"question\":\"问题\",\"state\":{\"mode\":\"RAG\"},\"afterMessageId\":0}"))
                .andReturn();
    }

    private ConversationTurnResult result() {
        var conversation = new ConversationDO().setTitle("会话");
        conversation.setId(11L);
        var message = new MessageDO().setConversationId(11L).setType("answer")
                .setContent(JsonNodeFactory.instance.objectNode().put("text", "最终答案"));
        message.setId(12L);
        return new ConversationTurnResult(conversation, List.of(message));
    }

    private void awaitInterruption(CountDownLatch interrupted) {
        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException ex) {
            interrupted.countDown();
            Thread.currentThread().interrupt();
            throw new ServiceException(RAG_GENERATION_TIMEOUT);
        }
    }

    private void assertError(ServerSentEvent<CommonResult<?>> event, ErrorCode errorCode) {
        assertEquals("error", event.event());
        assertEquals(errorCode.getCode(), event.data().getCode());
    }
}
