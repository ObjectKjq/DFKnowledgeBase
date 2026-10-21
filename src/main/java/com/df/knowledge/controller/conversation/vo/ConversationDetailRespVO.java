package com.df.knowledge.controller.conversation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 会话恢复 Response VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "会话恢复 Response VO")
public class ConversationDetailRespVO extends ConversationRespVO {

    @Schema(description = "按时间正序排列的消息流")
    private List<MessageRespVO> messages;
}
