package com.df.knowledge.controller.knowledge.vo.knowledgebase;

import com.df.knowledge.common.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库分页查询 Request VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识库分页查询 Request VO")
public class KnowledgeBasePageReqVO extends PageParam {

    @Schema(description = "知识库名称（模糊匹配）", example = "复习")
    private String name;

    @Schema(description = "知识库类型（1 个人 2 共享）", example = "2")
    private Integer type;
}
