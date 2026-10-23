package com.df.knowledge.controller.rag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

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

    @Schema(description = "本次提问的知识库范围；不传沿用会话初始范围，空列表表示全部有权知识库")
    private List<@NotNull @Positive Long> knowledgeBaseIds;

    @Schema(description = "所选分块：不传沿用会话，空数组清空；选择后严格只用这些分块")
    @Size(max = 100)
    private List<@NotNull @Positive Long> selectedChunkIds;
    @Schema(description = "所选命名模型；不传沿用会话模型")
    @Positive
    private Long modelId;
    @Schema(description = "格式过滤，不传沿用会话，空列表清空")
    private List<Integer> formats;
    @Schema(description = "上传起点，不传沿用会话")
    private java.time.LocalDateTime uploadedFrom;
    @Schema(description = "上传终点，不传沿用会话")
    private java.time.LocalDateTime uploadedTo;
}
