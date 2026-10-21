package com.df.knowledge.controller.conversation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 会话创建 Request VO
 */
@Data
@Schema(description = "会话创建 Request VO")
public class ConversationCreateReqVO {

    @NotBlank(message = "会话标题不能为空")
    @Size(max = 100, message = "会话标题长度不能超过 100")
    @Schema(description = "会话标题", example = "如何让知识检索更可信？")
    private String title;

    @Size(max = 100, message = "检索范围最多包含 100 个知识库")
    @Schema(description = "发起会话时的知识库编号范围，不传或为空表示全部有权知识库")
    private List<@Positive(message = "知识库编号必须为正数") Long> knowledgeBaseIds;
}
