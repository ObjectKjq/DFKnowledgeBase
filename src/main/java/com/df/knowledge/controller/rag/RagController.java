package com.df.knowledge.controller.rag;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.ErrorCode;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.rag.vo.RagAnswerRespVO;
import com.df.knowledge.controller.rag.vo.RagAskReqVO;
import com.df.knowledge.controller.rag.vo.RagCitationRespVO;
import com.df.knowledge.service.rag.RagAnswerResult;
import com.df.knowledge.service.rag.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.df.knowledge.enums.ErrorCodeConstants.RAG_GENERATION_TIMEOUT;
import static com.df.knowledge.enums.ErrorCodeConstants.RAG_MODEL_UNAVAILABLE;

import static com.df.knowledge.common.CommonResult.success;

/**
 * RAG 检索 Controller
 */
@Tag(name = "ragController")
@RestController
@RequestMapping("/knowledge/rag")
@Validated
@Slf4j
public class RagController {

    @Resource
    private RagService ragService;

    @Resource
    private ExecutorService ragStreamExecutor;

    @Resource
    private ScheduledExecutorService ragStreamTimeoutExecutor;

    @Value("${knowledge.rag.stream-timeout-ms:65000}")
    private long streamTimeoutMs;

    @PostMapping("/ask")
    @Operation(summary = "RAG 检索", description = "在当前用户会话中同步执行权限过滤、混合检索和带引用答案生成")
    public CommonResult<RagAnswerRespVO> ask(@Valid @RequestBody RagAskReqVO reqVO) {
        RagAnswerResult result = ragService.ask(StpUtil.getLoginIdAsLong(), reqVO);
        RagAnswerRespVO response = BeanUtil.copyProperties(result, RagAnswerRespVO.class);
        response.setCitations(BeanUtil.copyToList(result.getCitations(), RagCitationRespVO.class));
        return success(response);
    }

    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式 RAG 检索", description = "SSE 事件的 data 均为 CommonResult。"
            + "delta 的 data 是暂定答案文本增量；citations 的 data 是当前引用集合（整组替换）；"
            + "done 的 data 是 RagAnswerRespVO，客户端须以该结果替换暂定文本（可能拒答）。"
            + "error 使用公共错误码，随后关闭流；不会再发送 done。可用原会话和问题重新提交。")
    public SseEmitter stream(@Valid @RequestBody RagAskReqVO reqVO, HttpServletResponse response) {
        // 登录身份在请求线程读取，异步任务只使用明确传递的用户编号
        Long userId = StpUtil.getLoginIdAsLong();
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        // 超时由主动定时任务控制，Servlet 超时后已无法可靠写入错误事件
        StreamResponse stream = new StreamResponse(new SseEmitter(0L));
        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                RagAnswerResult result = ragService.stream(userId, reqVO,
                        delta -> stream.send("delta", success(delta)),
                        citations -> stream.send("citations", success(BeanUtil.copyToList(citations, RagCitationRespVO.class))));
                RagAnswerRespVO answer = BeanUtil.copyProperties(result, RagAnswerRespVO.class);
                answer.setCitations(BeanUtil.copyToList(result.getCitations(), RagCitationRespVO.class));
                stream.finish(answer);
            } catch (ServiceException ex) {
                stream.fail(ex.getErrorCode());
            } catch (RuntimeException ex) {
                log.error("[stream][问答流执行失败，会话编号({})]", reqVO.getConversationId(), ex);
                stream.fail(RAG_MODEL_UNAVAILABLE);
            }
        }, null);
        var deadline = ragStreamTimeoutExecutor.schedule(() -> {
            // 调度线程不写网络，避免某个慢客户端拖延其他问答的超时处理
            task.cancel(true);
            ragStreamExecutor.execute(() -> stream.fail(RAG_GENERATION_TIMEOUT));
        }, streamTimeoutMs, TimeUnit.MILLISECONDS);
        stream.emitter.onError(error -> {
            stream.close();
            deadline.cancel(false);
            task.cancel(true);
        });
        stream.emitter.onCompletion(() -> {
            stream.close();
            deadline.cancel(false);
            task.cancel(true);
        });
        ragStreamExecutor.execute(task);
        return stream.emitter;
    }

    /**
     * 串行化增量与终止事件，保证流至多结束一次
     */
    private static final class StreamResponse {

        private final SseEmitter emitter;
        private boolean closed;

        private StreamResponse(SseEmitter emitter) {
            this.emitter = emitter;
        }

        private synchronized void send(String event, CommonResult<?> data) {
            if (closed) {
                throw new ServiceException(RAG_GENERATION_TIMEOUT);
            }
            try {
                emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ex) {
                closed = true;
                emitter.completeWithError(ex);
                throw new IllegalStateException("问答流已断开", ex);
            }
        }

        private synchronized void finish(RagAnswerRespVO answer) {
            if (!closed) {
                send("citations", success(answer.getCitations()));
                send("done", success(answer));
                close();
                emitter.complete();
            }
        }

        private synchronized void fail(ErrorCode errorCode) {
            if (!closed) {
                try {
                    send("error", CommonResult.error(errorCode));
                } finally {
                    close();
                    emitter.complete();
                }
            }
        }

        private synchronized void close() {
            closed = true;
        }
    }
}
