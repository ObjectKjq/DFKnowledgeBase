package com.df.knowledge.controller.knowledge.vo.knowledgebase;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库 Response VO
 */
@Data
@Schema(description = "知识库 Response VO")
public class KnowledgeBaseRespVO {

    @Schema(description = "知识库编号", example = "1")
    private Long id;

    @Schema(description = "知识库名称", example = "期末复习")
    private String name;

    @Schema(description = "知识库类型（1 个人 2 共享）", example = "1")
    private Integer type;

    @Schema(description = "所有者用户编号", example = "1")
    private Long ownerId;

    @Schema(description = "是否默认知识库（不可删除）", example = "false")
    private Boolean defaultFlag;

    @Schema(description = "知识库说明", example = "高数与线代资料")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
