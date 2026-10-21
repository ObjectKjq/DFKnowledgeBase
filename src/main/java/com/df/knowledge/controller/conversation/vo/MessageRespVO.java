package com.df.knowledge.controller.conversation.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息 Response VO
 */
@Data
@Schema(description = "会话消息 Response VO")
public class MessageRespVO {

    @Schema(description = "消息编号", example = "1")
    private Long id;

    @Schema(description = "会话编号", example = "1")
    private Long conversationId;

    @Schema(description = "消息类型")
    private String type;

    @Schema(description = "结构化消息内容")
    private JsonNode content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
