package com.df.knowledge.controller.doc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 混合检索 Request VO
 */
@Data
@Schema(description = "混合检索 Request VO")
public class DocSearchReqVO {

    @NotBlank
    @Schema(description = "检索文本", example = "知识检索如何工作")
    private String query;

    @Schema(description = "知识库编号范围，不传或为空表示全部有权知识库")
    private List<Long> knowledgeBaseIds;

    @jakarta.validation.constraints.Pattern(regexp = "HYBRID|FUZZY")
    @Schema(description = "检索模式：HYBRID 混合、FUZZY 模糊")
    private String mode = "HYBRID";
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(500)
    @Schema(description = "页码，从 1 开始")
    private Integer pageNo = 1;
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(100)
    @Schema(description = "每页数量，默认 20")
    private Integer pageSize = 20;
    @Schema(description = "文档格式编码过滤，空列表表示不限")
    private List<Integer> formats;
    @Schema(description = "上传时间起点，包含")
    private java.time.LocalDateTime uploadedFrom;
    @Schema(description = "上传时间终点，包含")
    private java.time.LocalDateTime uploadedTo;
}
