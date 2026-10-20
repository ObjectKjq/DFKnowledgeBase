package com.df.knowledge.controller.knowledge.vo.knowledgebase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库创建 Request VO
 */
@Data
@Schema(description = "知识库创建 Request VO")
public class KnowledgeBaseSaveReqVO {

    @Schema(description = "知识库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "期末复习")
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 50, message = "知识库名称长度不能超过 50")
    private String name;

    @Schema(description = "知识库类型（1 个人 2 共享）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "知识库类型不能为空")
    private Integer type;

    @Schema(description = "知识库说明", example = "高数与线代资料")
    @Size(max = 200, message = "知识库说明长度不能超过 200")
    private String description;
}
