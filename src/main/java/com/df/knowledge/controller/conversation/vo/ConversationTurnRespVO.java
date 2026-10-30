package com.df.knowledge.controller.conversation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 会话本轮执行结果，复用持久化消息作为最终展示内容。 */
@Data
@Schema(description = "会话本轮执行结果")
public class ConversationTurnRespVO {

    @Schema(description = "本轮执行后的会话摘要和当前选择")
    private ConversationRespVO conversation;

    @Schema(description = "已展示编号之后的持久化消息，按时间正序排列")
    private List<MessageRespVO> messages;
}
