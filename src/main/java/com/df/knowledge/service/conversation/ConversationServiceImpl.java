package com.df.knowledge.service.conversation;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.conversation.vo.ConversationCreateReqVO;
import com.df.knowledge.controller.conversation.vo.MessageCreateReqVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.dal.dataobject.conversation.MessageDO;
import com.df.knowledge.dal.mysql.conversation.ConversationMapper;
import com.df.knowledge.dal.mysql.conversation.MessageMapper;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import static com.df.knowledge.enums.ErrorCodeConstants.CONVERSATION_NOT_FOUND;

/**
 * 会话与消息 Service 实现类
 */
@Service
@Validated
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    @Resource
    private ConversationMapper conversationMapper;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConversation(Long userId, ConversationCreateReqVO reqVO) {
        // 1. 固化创建时的检索范围，并拒绝无权知识库
        List<Long> knowledgeBaseIds = reqVO.getKnowledgeBaseIds() == null
                ? List.of() : List.copyOf(new LinkedHashSet<>(reqVO.getKnowledgeBaseIds()));
        knowledgeBaseIds.forEach(id -> knowledgeBasePermissionService.checkViewPermission(userId, id));
        // 2. 会话所属用户只取当前登录身份，不接受客户端传入
        ConversationDO conversation = new ConversationDO();
        conversation.setUserId(userId);
        conversation.setTitle(reqVO.getTitle());
        conversation.setKnowledgeBaseIds(knowledgeBaseIds);
        conversation.setState(new RetrievalState().setKnowledgeBaseIds(knowledgeBaseIds));
        conversationMapper.insert(conversation);
        return conversation.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateState(Long userId, Long id, RetrievalState state) {
        var conversation = validateConversationOwner(userId, id);
        // 状态记录用户选择，允许保留已失权的历史编号；真正读取内容时逐次求当前权限交集。
        // 将失权选择改成空数组会把“无可见范围”错误扩大为“全部有权知识库”。
        conversation.setState(state);
        conversationMapper.updateById(conversation);
    }

    @Override
    public List<ConversationDO> getConversationList(Long userId) {
        return conversationMapper.selectListByUserId(userId);
    }

    @Override
    public ConversationDO getConversation(Long userId, Long id) {
        return validateConversationOwner(userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(Long userId, Long id) {
        validateConversationOwner(userId, id);
        messageMapper.deleteByConversationId(id);
        conversationMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMessage(Long userId, MessageCreateReqVO reqVO) {
        validateConversationOwner(userId, reqVO.getConversationId());
        MessageDO message = new MessageDO();
        message.setConversationId(reqVO.getConversationId());
        message.setType(reqVO.getType());
        message.setContent(reqVO.getContent());
        messageMapper.insert(message);
        return message.getId();
    }

    @Override
    public List<MessageDO> getMessageList(Long userId, Long conversationId) {
        validateConversationOwner(userId, conversationId);
        return messageMapper.selectListByConversationId(conversationId).stream()
                .sorted(Comparator.comparing(MessageDO::getCreateTime).thenComparing(MessageDO::getId))
                .toList();
    }

    /**
     * 校验会话属于当前用户；不存在与他人会话统一按不存在处理
     */
    private ConversationDO validateConversationOwner(Long userId, Long id) {
        ConversationDO conversation = conversationMapper.selectByIdAndUserId(id, userId);
        if (conversation == null) {
            throw new ServiceException(CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }
}
