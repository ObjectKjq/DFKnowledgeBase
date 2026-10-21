package com.df.knowledge.dal.dataobject.conversation;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.df.knowledge.dal.dataobject.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会话消息 DO
 */
@TableName(value = "knowledge_message", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageDO extends BaseDO {

    /**
     * 会话编号，关联 {@link ConversationDO#getId()}
     */
    private Long conversationId;

    /**
     * 消息类型（user、results、answer、guide、empty、note）
     */
    private String type;

    /**
     * 消息内容；保留结果卡片、引用等不同消息类型的结构
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode content;
}
