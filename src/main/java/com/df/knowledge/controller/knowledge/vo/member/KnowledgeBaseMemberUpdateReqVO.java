package com.df.knowledge.controller.knowledge.vo.member;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库成员角色修改 Request VO
 */
@Data
@Schema(description = "知识库成员角色修改 Request VO")
public class KnowledgeBaseMemberUpdateReqVO {

    @Schema(description = "知识库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "知识库编号不能为空")
    private Long knowledgeBaseId;

    @Schema(description = "成员编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "成员编号不能为空")
    private Long id;

    @Schema(description = "成员角色（1 编辑者 2 查看者）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "成员角色不能为空")
    private Integer memberRole;
}
