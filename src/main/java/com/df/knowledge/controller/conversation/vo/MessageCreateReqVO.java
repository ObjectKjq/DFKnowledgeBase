package com.df.knowledge.controller.conversation.vo;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 会话消息创建 Request VO
 */
@Data
@Schema(description = "会话消息创建 Request VO")
public class MessageCreateReqVO {

    @NotNull(message = "会话编号不能为空")
    @Positive(message = "会话编号必须为正数")
    @Schema(description = "会话编号", example = "1")
    private Long conversationId;

    @NotBlank(message = "消息类型不能为空")
    @Size(max = 32, message = "消息类型长度不能超过 32")
    @Pattern(regexp = "user|results|answer|guide|empty|note", message = "消息类型不正确")
    @Schema(description = "消息类型", allowableValues = {"user", "results", "answer", "guide", "empty", "note"})
    private String type;

    @NotNull(message = "消息内容不能为空")
    @Schema(description = "结构化消息内容，可保存文本、结果卡片与引用")
    private JsonNode content;
}
