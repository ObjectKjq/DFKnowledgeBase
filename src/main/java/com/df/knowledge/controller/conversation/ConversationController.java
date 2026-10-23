package com.df.knowledge.controller.conversation;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.conversation.vo.ConversationCreateReqVO;
import com.df.knowledge.controller.conversation.vo.ConversationDetailRespVO;
import com.df.knowledge.controller.conversation.vo.ConversationRespVO;
import com.df.knowledge.controller.conversation.vo.MessageCreateReqVO;
import com.df.knowledge.controller.conversation.vo.MessageRespVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.service.conversation.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.df.knowledge.common.CommonResult.success;

/**
 * 会话与消息 Controller
 *
 * 接口仅要求登录，所有数据访问都在服务层按当前用户隔离
 */
@Tag(name = "conversationController")
@RestController
@RequestMapping("/knowledge/conversation")
@Validated
public class ConversationController {

    @Resource
    private ConversationService conversationService;

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

    @org.springframework.web.bind.annotation.PutMapping("/state/update")
    @Operation(summary = "修改会话当前选择", description = "保存范围、过滤、模式、模型和所选上下文；只影响后续消息")
    public CommonResult<Boolean> updateState(@Valid @RequestBody com.df.knowledge.controller.conversation.vo.ConversationStateReqVO request) {
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
