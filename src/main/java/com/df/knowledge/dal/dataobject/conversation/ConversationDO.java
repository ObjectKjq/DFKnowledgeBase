package com.df.knowledge.dal.dataobject.conversation;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.df.knowledge.dal.dataobject.BaseDO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 会话 DO
 */
@TableName(value = "knowledge_conversation", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationDO extends BaseDO {

    /**
     * 所属用户编号，关联 {@link UserDO#getId()}
     */
    private Long userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 发起会话时的知识库编号范围，关联 {@link KnowledgeBaseDO#getId()}；空列表表示全部有权知识库
     */
    @TableField(value = "retrieval_scope", typeHandler = JacksonTypeHandler.class)
    private List<Long> knowledgeBaseIds;

    /** 当前会话检索状态，存量会话由 SQL015 补混合模式 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private com.df.knowledge.service.conversation.RetrievalState state;
}
