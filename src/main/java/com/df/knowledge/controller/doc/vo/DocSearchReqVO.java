package com.df.knowledge.controller.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 混合或模糊检索 Request VO
 */
@Data
@Schema(description = "混合或模糊检索 Request VO")
public class DocSearchReqVO {

    @NotBlank
    @Schema(description = "检索文本", example = "知识检索如何工作")
    private String query;

    @Schema(description = "知识库编号范围，不传或为空表示全部有权知识库")
    private List<Long> knowledgeBaseIds;

    @Pattern(regexp = "HYBRID|FUZZY")
    @Schema(description = "检索模式：HYBRID 混合、FUZZY 模糊")
    private String mode;
    @Positive
    @Schema(description = "会话编号；提供时恢复本会话选择并持久化本轮问题和结果，不传为无会话检索")
    private Long conversationId;
    @Min(1)
    @Max(10000)
    @Schema(description = "页码，从 1 开始")
    private Integer pageNo = 1;
    @Min(1)
    @Max(100)
    @Schema(description = "每页数量，默认 20")
    private Integer pageSize = 20;
    @Schema(description = "文档格式编码过滤，空列表表示不限")
    private List<Integer> formats;
    @Schema(description = "上传时间起点，包含")
    private LocalDateTime uploadedFrom;
    @Schema(description = "上传时间终点，包含")
    private LocalDateTime uploadedTo;
}
