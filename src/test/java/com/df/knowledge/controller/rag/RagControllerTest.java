package com.df.knowledge.controller.rag;

import cn.dev33.satoken.stp.StpUtil;
import com.df.knowledge.service.rag.RagAnswerResult;
import com.df.knowledge.service.rag.RagCitation;
import com.df.knowledge.service.rag.RagService;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockAsyncContext;
import jakarta.servlet.AsyncEvent;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * SSE 接口协议测试
 */
class RagControllerTest {

    @Test
    void testStream_disconnectionInterruptsGeneration() throws Exception {
        // 准备参数
        RagService service = mock(RagService.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException ex) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
                throw new ServiceException(ErrorCodeConstants.RAG_GENERATION_TIMEOUT);
            }
            return null;
        }).when(service).stream(eq(7L), any(), any(), any());

        // 调用
        try (var client = new StreamClient(service)) {
            MvcResult pending = client.open();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            MockAsyncContext context = (MockAsyncContext) pending.getRequest().getAsyncContext();
            for (var listener : context.getListeners()) {
                listener.onError(new AsyncEvent(context, new java.io.IOException("客户端断开")));
            }

            // 断言
            assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void testStream_refusalReplacesProvisionalAnswerInDone() throws Exception {
        // 准备参数
        RagService service = mock(RagService.class);
        doAnswer(invocation -> {
            Consumer<String> delta = invocation.getArgument(2);
            delta.accept("暂定答案。[9]");
            return new RagAnswerResult().setAnswered(false).setAnswer("未找到足够依据")
                    .setSuggestions(List.of("扩大检索范围")).setCitations(List.of());
        }).when(service).stream(eq(7L), any(), any(), any());

        // 调用并断言
        try (var client = new StreamClient(service)) {
            MvcResult pending = client.open();
            pending.getAsyncResult(5000);
            String body = client.finish(pending);
            assertEquals(List.of("delta", "citations", "done"), events(body));
            assertTrue(body.contains("\"answered\":false"));
            assertTrue(body.contains("\"citations\":[]"));
            assertTrue(body.contains("\"answer\":\"未找到足够依据\""));
        }
    }

    @Test
    void testStream_generationErrorEndsWithErrorInsteadOfDone() throws Exception {
        // 准备参数：先生成部分文本，再报告模型错误
        RagService service = mock(RagService.class);
        doAnswer(invocation -> {
            Consumer<String> delta = invocation.getArgument(2);
            delta.accept("部分答案");
            throw new ServiceException(ErrorCodeConstants.RAG_MODEL_UNAVAILABLE);
        }).when(service).stream(eq(7L), any(), any(), any());

        // 调用并断言
        try (var client = new StreamClient(service)) {
            MvcResult pending = client.open();
            pending.getAsyncResult(5000);
            String body = client.finish(pending);
            assertEquals(List.of("delta", "error"), events(body));
            assertTrue(body.contains("\"code\":1001004002"));
        }
    }

    @Test
    void testStream_timeoutEndsWithErrorAndInterruptsGeneration() throws Exception {
        // 准备参数：模型保持等待，通过时间端口触发超时
        RagService service = mock(RagService.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException ex) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
                throw new ServiceException(ErrorCodeConstants.RAG_GENERATION_TIMEOUT);
            }
            return null;
        }).when(service).stream(eq(7L), any(), any(), any());

        // 调用
        try (var client = new StreamClient(service)) {
            MvcResult pending = client.open();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            client.timeout.get().run();
            pending.getAsyncResult(5000);

            // 断言
            assertTrue(interrupted.await(5, TimeUnit.SECONDS));
            String body = client.finish(pending);
            assertEquals(List.of("error"), events(body));
            assertTrue(body.contains("\"code\":1001004003"));
        }
    }

    @Test
    void testStream_deltaCitationsAndDoneArriveInOrder() throws Exception {
        // 准备参数：生成服务在完成前推送两段文本
        RagService service = mock(RagService.class);
        doAnswer(invocation -> {
            Consumer<String> delta = invocation.getArgument(2);
            delta.accept("可信");
            delta.accept("答案。[1]");
            Consumer<List<RagCitation>> citations = invocation.getArgument(3);
            citations.accept(List.of(new RagCitation().setNumber(1).setChunkId(101L)));
            return new RagAnswerResult().setAnswered(true).setAnswer("可信答案。[1]")
                    .setSuggestions(List.of()).setCitations(List.of(new RagCitation().setNumber(1).setChunkId(101L)));
        }).when(service).stream(eq(7L), any(), any(), any());
        try (var client = new StreamClient(service)) {
            // 调用
            MvcResult pending = client.open();
            pending.getAsyncResult(5000);
            var response = client.mvc.perform(asyncDispatch(pending)).andReturn().getResponse();

            // 断言
            String body = response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(200, response.getStatus());
            assertTrue(response.getContentType().startsWith("text/event-stream"));
            assertEquals(List.of("delta", "delta", "citations", "citations", "done"), body.lines()
                    .filter(line -> line.startsWith("event:")).map(line -> line.substring(6)).toList());
            assertTrue(body.contains("\"data\":\"可信\""));
            assertTrue(body.contains("\"chunkId\":101"));
            assertTrue(body.contains("\"answer\":\"可信答案。[1]\""));
        }
    }

    private static List<String> events(String body) {
        return body.lines().filter(line -> line.startsWith("event:"))
                .map(line -> line.substring(6)).toList();
    }

    /**
     * 每次测试独立管理登录上下文与异步执行器
     */
    private static final class StreamClient implements AutoCloseable {

        private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        private final MockedStatic<StpUtil> login = mockStatic(StpUtil.class);
        private final MockMvc mvc;
        private final AtomicReference<Runnable> timeout = new AtomicReference<>();

        private StreamClient(RagService service) {
            RagController controller = new RagController();
            ReflectionTestUtils.setField(controller, "ragService", service);
            ReflectionTestUtils.setField(controller, "ragStreamExecutor", executor);
            ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
            when(scheduler.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.MILLISECONDS)))
                    .thenAnswer(invocation -> {
                        timeout.set(invocation.getArgument(0));
                        return mock(ScheduledFuture.class);
                    });
            ReflectionTestUtils.setField(controller, "ragStreamTimeoutExecutor", scheduler);
            ReflectionTestUtils.setField(controller, "streamTimeoutMs", 65000L);
            login.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
            mvc = MockMvcBuilders.standaloneSetup(controller).build();
        }

        private MvcResult open() throws Exception {
            return mvc.perform(post("/knowledge/rag/ask-stream").contentType("application/json")
                    .content("{\"conversationId\":11,\"question\":\"问题\"}")).andReturn();
        }

        private String finish(MvcResult pending) throws Exception {
            return mvc.perform(asyncDispatch(pending)).andReturn().getResponse()
                    .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override
        public void close() {
            executor.shutdownNow();
            executor.close();
            login.close();
        }
    }
}
