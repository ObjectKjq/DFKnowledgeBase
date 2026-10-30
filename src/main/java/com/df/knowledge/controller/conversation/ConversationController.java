package com.df.knowledge.controller.conversation;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.conversation.vo.ConversationCreateReqVO;
import com.df.knowledge.controller.conversation.vo.ConversationDetailRespVO;
import com.df.knowledge.controller.conversation.vo.ConversationRespVO;
import com.df.knowledge.controller.conversation.vo.ConversationStateReqVO;
import com.df.knowledge.controller.conversation.vo.ConversationSendReqVO;
import com.df.knowledge.controller.conversation.vo.ConversationTurnRespVO;
import com.df.knowledge.controller.conversation.vo.MessageCreateReqVO;
import com.df.knowledge.controller.conversation.vo.MessageRespVO;
import com.df.knowledge.controller.rag.vo.RagCitationRespVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.service.conversation.ConversationService;
import com.df.knowledge.service.conversation.ConversationTurnResult;
import com.df.knowledge.service.conversation.ConversationTurnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.reactivestreams.Subscription;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.List;

import static com.df.knowledge.common.CommonResult.success;
import static com.df.knowledge.enums.ErrorCodeConstants.RAG_GENERATION_TIMEOUT;
import static com.df.knowledge.enums.ErrorCodeConstants.RAG_MODEL_UNAVAILABLE;

/**
 * 会话与消息 Controller
 *
 * 接口仅要求登录，所有数据访问都在服务层按当前用户隔离
 */
@Tag(name = "conversationController")
@RestController
@RequestMapping("/knowledge/conversation")
@Validated
@Slf4j
public class ConversationController {

    @Resource
    private ConversationService conversationService;

    @Resource
    private ConversationTurnService conversationTurnService;

    @Resource
    private Scheduler ragStreamScheduler;

    @Value("${knowledge.rag.stream-timeout-ms:65000}")
    private long streamTimeoutMs;

    @PostMapping("/send")
    @Operation(summary = "发送一轮会话", description = "首次发送创建会话，按完整选择执行检索、引导或 RAG，返回新增持久化消息")
    public CommonResult<ConversationTurnRespVO> send(@Valid @RequestBody ConversationSendReqVO request) {
        return success(convertTurn(conversationTurnService.send(StpUtil.getLoginIdAsLong(), request,
                conversation -> { }, delta -> { }, citations -> { })));
    }

    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式发送一轮会话", description = "所有 SSE 事件均为 CommonResult：conversation 返回会话摘要，"
            + "delta 返回暂定答案增量，citations 返回暂定引用集合，done 返回 ConversationTurnRespVO 并替换暂定内容；"
            + "error 返回公共错误码并关闭流。")
    public ResponseEntity<Flux<ServerSentEvent<CommonResult<?>>>> sendStream(
            @Valid @RequestBody ConversationSendReqVO request, HttpServletRequest servletRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        var events = Flux.<ServerSentEvent<CommonResult<?>>>create(sink -> {
            var result = conversationTurnService.send(userId, request,
                    conversation -> sink.next(streamEvent("conversation",
                            success(BeanUtil.copyProperties(conversation, ConversationRespVO.class)))),
                    delta -> sink.next(streamEvent("delta", success(delta))),
                    citations -> sink.next(streamEvent("citations",
                            success(BeanUtil.copyToList(citations, RagCitationRespVO.class)))));
            sink.next(streamEvent("done", success(convertTurn(result))));
            sink.complete();
        }).subscribeOn(ragStreamScheduler, false);
        // 固定整轮总时限，即使客户端暂停消费，也要及时中断生成任务。
        Mono<ServerSentEvent<CommonResult<?>>> deadline = Mono.delay(Duration.ofMillis(streamTimeoutMs))
                .then(Mono.error(new ServiceException(RAG_GENERATION_TIMEOUT)));
        var body = events.mergeWith(deadline)
                .onErrorResume(ServiceException.class,
                        ex -> Flux.just(streamEvent("error", CommonResult.error(ex.getErrorCode()))))
                .onErrorResume(RuntimeException.class, ex -> {
                    log.error("[sendStream][会话({})发送失败]", request.getConversationId(), ex);
                    return Flux.just(streamEvent("error", CommonResult.error(RAG_MODEL_UNAVAILABLE)));
                })
                .takeUntil(event -> "done".equals(event.event()) || "error".equals(event.event()))
                .doOnSubscribe(subscription -> registerStreamCancellation(servletRequest, subscription));
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).header("X-Accel-Buffering", "no")
                .body(body);
    }

    /** 监听 Servlet 异步结束，确保模型等待期间的断开也能取消订阅。 */
    private void registerStreamCancellation(HttpServletRequest request, Subscription subscription) {
        WebAsyncUtils.getAsyncManager(request).registerDeferredResultInterceptor(subscription,
                new DeferredResultProcessingInterceptor() {
                    @Override
                    public <T> void postProcess(NativeWebRequest webRequest, DeferredResult<T> result, Object value) {
                        subscription.cancel();
                    }

                    @Override
                    public <T> void afterCompletion(NativeWebRequest webRequest, DeferredResult<T> result) {
                        subscription.cancel();
                    }
                });
    }

    private ServerSentEvent<CommonResult<?>> streamEvent(String event, CommonResult<?> data) {
        return ServerSentEvent.<CommonResult<?>>builder(data).event(event).build();
    }

    private ConversationTurnRespVO convertTurn(ConversationTurnResult result) {
        return new ConversationTurnRespVO()
                .setConversation(BeanUtil.copyProperties(result.conversation(), ConversationRespVO.class))
                .setMessages(BeanUtil.copyToList(result.messages(), MessageRespVO.class));
    }

    @PostMapping("/create")
    @Operation(summary = "创建会话", description = "记录当前用户和发起时的知识库检索范围")
    public CommonResult<Long> createConversation(@Valid @RequestBody ConversationCreateReqVO reqVO) {
        return success(conversationService.createConversation(StpUtil.getLoginIdAsLong(), reqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "查询会话列表", description = "仅返回当前用户自己的会话")
    public CommonResult<List<ConversationRespVO>> getConversationList() {
        return success(BeanUtil.copyToList(conversationService.getConversationList(StpUtil.getLoginIdAsLong()),
                ConversationRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "恢复会话", description = "返回会话及按时间正序排列的完整消息流")
    @Parameter(name = "id", description = "会话编号", required = true, example = "1")
    public CommonResult<ConversationDetailRespVO> getConversation(@RequestParam("id") Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        ConversationDO conversation = conversationService.getConversation(userId, id);
        ConversationDetailRespVO response = BeanUtil.copyProperties(conversation, ConversationDetailRespVO.class);
        response.setMessages(BeanUtil.copyToList(conversationService.getMessageList(userId, id), MessageRespVO.class));
        return success(response);
    }

    @PutMapping("/state/update")
    @Operation(summary = "修改会话当前选择", description = "保存范围、过滤、模式、模型和所选上下文；只影响后续消息")
    public CommonResult<Boolean> updateState(@Valid @RequestBody ConversationStateReqVO request) {
        conversationService.updateState(StpUtil.getLoginIdAsLong(), request.getId(), request.getState());
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会话", description = "删除当前用户的会话及其全部消息")
    @Parameter(name = "id", description = "会话编号", required = true, example = "1")
    public CommonResult<Boolean> deleteConversation(@RequestParam("id") Long id) {
        conversationService.deleteConversation(StpUtil.getLoginIdAsLong(), id);
        return success(true);
    }

    @PostMapping("/message/create")
    @Operation(summary = "写入消息", description = "向当前用户自己的会话写入结构化消息")
    public CommonResult<Long> createMessage(@Valid @RequestBody MessageCreateReqVO reqVO) {
        return success(conversationService.createMessage(StpUtil.getLoginIdAsLong(), reqVO));
    }

    @GetMapping("/message/list")
    @Operation(summary = "读取消息", description = "按创建时间和消息编号正序返回当前用户会话的消息流")
    @Parameter(name = "conversationId", description = "会话编号", required = true, example = "1")
    public CommonResult<List<MessageRespVO>> getMessageList(@RequestParam("conversationId") Long conversationId) {
        return success(BeanUtil.copyToList(conversationService.getMessageList(StpUtil.getLoginIdAsLong(), conversationId),
                MessageRespVO.class));
    }
}
