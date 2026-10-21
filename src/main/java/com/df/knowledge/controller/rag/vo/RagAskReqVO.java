package com.df.knowledge.controller.rag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * RAG 检索 Request VO
 */
@Data
@Schema(description = "RAG 检索 Request VO")
public class RagAskReqVO {

    @NotNull(message = "会话编号不能为空")
    @Positive(message = "会话编号必须为正数")
    @Schema(description = "会话编号", example = "1")
    private Long conversationId;

    @NotBlank(message = "问题不能为空")
    @Size(max = 2000, message = "问题长度不能超过 2000")
    @Schema(description = "问题", example = "如何让知识检索更可信？")
    private String question;
}
