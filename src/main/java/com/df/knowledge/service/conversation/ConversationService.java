package com.df.knowledge.service.conversation;

import com.df.knowledge.controller.conversation.vo.ConversationCreateReqVO;
import com.df.knowledge.controller.conversation.vo.MessageCreateReqVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.dal.dataobject.conversation.MessageDO;

import java.util.List;

/**
 * 会话与消息 Service 接口
 */
public interface ConversationService {

    /**
     * 准备一轮发送，首次输入创建会话，并保存本轮完整选择。
     * @param userId 当前用户
     * @param id 会话编号，可空
     * @param title 首次输入生成的标题
     * @param state 完整选择
     * @return 会话
     */
    ConversationDO beginTurn(Long userId, Long id, String title, RetrievalState state);

    /**
     * 读取客户端尚未展示的消息。
     * @param userId 当前用户
     * @param conversationId 会话编号
     * @param afterId 已展示的最大消息编号
     * @return 新增消息
     */
    List<MessageDO> getMessageListAfter(Long userId, Long conversationId, Long afterId);

    /**
     * 保存当前会话的完整检索选择。
     * @param userId 用户编号
     * @param id 会话编号
     * @param state 当前选择
     */
    void updateState(Long userId, Long id, RetrievalState state);

    /**
     * 创建当前用户的会话
     *
     * @param userId 当前用户编号
     * @param reqVO 创建信息
     * @return 会话编号
     */
    Long createConversation(Long userId, ConversationCreateReqVO reqVO);

    /**
     * 查询当前用户的会话列表
     *
     * @param userId 当前用户编号
     * @return 会话列表
     */
    List<ConversationDO> getConversationList(Long userId);

    /**
     * 获取当前用户的会话
     *
     * @param userId 当前用户编号
     * @param id 会话编号
     * @return 会话
     */
    ConversationDO getConversation(Long userId, Long id);

    /**
     * 删除当前用户的会话及其消息
     *
     * @param userId 当前用户编号
     * @param id 会话编号
     */
    void deleteConversation(Long userId, Long id);

    /**
     * 向当前用户的会话写入消息
     *
     * @param userId 当前用户编号
     * @param reqVO 消息信息
     * @return 消息编号
     */
    Long createMessage(Long userId, MessageCreateReqVO reqVO);

    /**
     * 按时间正序读取当前用户会话的消息
     *
     * @param userId 当前用户编号
     * @param conversationId 会话编号
     * @return 消息列表
     */
    List<MessageDO> getMessageList(Long userId, Long conversationId);
}
