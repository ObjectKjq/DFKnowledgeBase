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
}
