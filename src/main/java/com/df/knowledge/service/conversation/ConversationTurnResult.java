package com.df.knowledge.service.conversation;

import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.dal.dataobject.conversation.MessageDO;

import java.util.List;

/** 一轮发送结束后的会话和新增持久化消息。 */
public record ConversationTurnResult(ConversationDO conversation, List<MessageDO> messages) {
}
