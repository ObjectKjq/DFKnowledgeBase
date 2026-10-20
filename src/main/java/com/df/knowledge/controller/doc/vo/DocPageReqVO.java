package com.df.knowledge.controller.doc.vo;

import com.df.knowledge.common.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档分页查询 Request VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文档分页查询 Request VO")
public class DocPageReqVO extends PageParam {

    @Schema(description = "知识库编号（不传查全部有权知识库）", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "文档标题（模糊匹配）", example = "复习")
    private String title;

    @Schema(description = "文档状态（1 排队 2 解析中 3 索引中 4 可用 5 失败 6 已删除）", example = "4")
    private Integer status;
}
