package com.df.knowledge.dal.mysql.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.conversation.MessageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 会话消息 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<MessageDO> {

    /**
     * 查询会话消息
     *
     * @param conversationId 会话编号
     * @return 消息列表
     */
    default List<MessageDO> selectListByConversationId(Long conversationId) {
        return selectList(new LambdaQueryWrapper<MessageDO>().eq(MessageDO::getConversationId, conversationId));
    }

    /**
     * 删除会话下的全部消息
     *
     * @param conversationId 会话编号
     */
    default void deleteByConversationId(Long conversationId) {
        delete(new LambdaQueryWrapper<MessageDO>().eq(MessageDO::getConversationId, conversationId));
    }
}
