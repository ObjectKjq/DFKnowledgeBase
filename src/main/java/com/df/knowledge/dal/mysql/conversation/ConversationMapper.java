package com.df.knowledge.dal.mysql.conversation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * 会话 Mapper
 */
@Mapper
public interface ConversationMapper extends BaseMapper<ConversationDO> {

    /**
     * 查询用户自己的会话列表
     *
     * @param userId 用户编号
     * @return 会话列表
     */
    default List<ConversationDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getUserId, userId)
                .orderByDesc(ConversationDO::getUpdateTime)
                .orderByDesc(ConversationDO::getId));
    }

    /**
     * 按编号与所属用户查询会话
     *
     * @param id 会话编号
     * @param userId 所属用户编号
     * @return 会话
     */
    default ConversationDO selectByIdAndUserId(Long id, Long userId) {
        return selectOne(new LambdaQueryWrapper<ConversationDO>()
                .eq(ConversationDO::getId, id)
                .eq(ConversationDO::getUserId, userId)
                .last(TransactionSynchronizationManager.isActualTransactionActive() ? "FOR UPDATE" : ""));
    }
}
